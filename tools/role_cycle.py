#!/usr/bin/env python3
"""Continuous GitHub -> existing Firefox tab -> human-confirmed role handoff.

No Selenium, browser DOM access, OpenAI API, or automated message submission.
The loop continues running locally while this process is open. Each delivery
waits for the user to paste, send, and explicitly acknowledge the result.
"""
from __future__ import annotations

import argparse
import os
from pathlib import Path
import sqlite3
import sys
import time
from typing import Callable

import manual_chat_delivery as manual
import role_dispatcher as routing

# The user's verified left-to-right Firefox tab layout. Explicit init required.
DEFAULT_TABS = {f"{index:02d}": index + 1 for index in range(7)}
CONFIRM = {"y", "yes", "да", "д"}
SKIP = {"s", "skip", "пропустить", "п"}
QUIT = {"q", "quit", "выход", "в"}


def initialize_tabs(folder: Path, mapping: dict[str, int] | None = None) -> dict[str, int]:
    """Save a seven-role map; never silently reset a user's existing settings."""
    mapping = dict(DEFAULT_TABS if mapping is None else mapping)
    if set(mapping) != set(manual.ROLES):
        raise manual.DeliveryError("all roles 00..06 must have tab indexes")
    for number in mapping.values():
        manual.validate_tab_index(number)
    if len(set(mapping.values())) != len(mapping):
        raise manual.DeliveryError("two roles cannot use the same tab number")
    folder.mkdir(parents=True, exist_ok=True)
    target = folder / "chat_tabs.json"
    temporary = folder / "chat_tabs.json.tmp"
    import json
    temporary.write_text(json.dumps(mapping, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temporary.replace(target)
    return mapping


def validate_tabs(folder: Path) -> dict[str, int]:
    mapping = manual.read_tab_mapping(folder)
    if set(mapping) != set(manual.ROLES):
        raise manual.DeliveryError(
            "missing tab bindings: first run 'py tools/role_cycle.py init-tabs'")
    if len(set(mapping.values())) != len(mapping):
        raise manual.DeliveryError("duplicate tab numbers in configured role mapping")
    return mapping


class RoleCycle:
    """One polling process; no background threads or automatic browser submissions."""

    def __init__(self, router: routing.Dispatcher, folder: Path,
                 *, ask: Callable[[str], str] = input,
                 tell: Callable[[str], None] = print,
                 tab_switcher: Callable[[int], None] = manual.switch_firefox_tab,
                 clipboard: Callable[[str], None] = manual.copy_windows_clipboard):
        self.router = router
        self.folder = folder
        self.state = manual.DeliveryState(folder)
        self.ask = ask
        self.tell = tell
        self.tab_switcher = tab_switcher
        self.clipboard = clipboard
        self.skipped: set[tuple[str, str]] = set()
        self.stop_requested = False

    def close(self) -> None:
        self.state.close()

    def scan(self, issue: int | None, *, dry_run: bool = False) -> int:
        """Read GitHub, render inbox, then handle a bounded set of pending deliveries.

        Returns number of user-confirmed messages, not number of tabs switched.
        """
        stats = self.router.scan(issue)
        self.tell("GitHub: " + ", ".join(f"{k}={v}" for k, v in stats.items()))
        items = manual.pending(self.folder, self.state)
        if not items:
            self.tell("Новых поручений для отправки нет.")
            return 0
        confirmed = 0
        handled_roles: set[str] = set()
        for role, event, path, message, digest, previous in items:
            if (role, event) in self.skipped or role in handled_roles:
                continue
            handled_roles.add(role)  # One task per role per scan iteration.
            tab = manual.read_tab_mapping(self.folder).get(role)
            if tab is None:
                raise manual.DeliveryError(f"no tab configured for role {role}")
            self.tell(f"\nПоручение {event} -> роль {role}, вкладка #{tab} "
                      f"| {previous} | {len(message)} символов")
            self.tell(f"Источник: {path}")
            if dry_run:
                self.tell("PREVIEW: браузер, буфер обмена и статус доставки не изменены.")
                continue

            # Failure in tab selection or clipboard leaves this message pending.
            prepared = manual.prepare(
                self.folder, self.state, role, event,
                use_tab=True, tab_switcher=self.tab_switcher, clipboard=self.clipboard)
            if prepared != (role, event):
                raise manual.DeliveryError(f"unexpected prepared event: {prepared!r}")
            self.tell("В ЭТОМ РЕЖИМЕ подтверждайте через y ниже (отдельную команду confirm не вводите).")
            self.tell("Проверьте, что это нужный чат. Вставьте Ctrl+V и отправьте Enter.")
            while True:
                answer = self.ask(
                    "ПОСЛЕ отправки в ChatGPT: [y] подтверждаю / [s] позже / [q] выход: ").strip().lower()
                if answer in CONFIRM:
                    # No acknowledgement if file was changed between prepare & confirmation.
                    current_message, current_digest = manual.read_message(path)
                    if current_digest != digest or current_message != message:
                        raise manual.DeliveryError(
                            "message changed while waiting for confirmation; NOT confirmed")
                    self.state.confirm(role, event, digest)
                    confirmed += 1
                    self.tell(f"Подтверждено вручную: {role}/{event}")
                    break
                if answer in SKIP:
                    # The task stays pending for next process launch; skip in this run.
                    self.skipped.add((role, event))
                    self.tell("Отложено, осталось в очереди (без подтверждения).")
                    break
                if answer in QUIT:
                    self.stop_requested = True
                    self.tell("Цикл остановлен. Неподтверждённое поручение осталось в очереди.")
                    return confirmed
                self.tell("Ответ не распознан. Нажмите y, s или q; Enter сам по себе ничего не подтверждает.")
        return confirmed


def make_router(repo: str, folder: Path, allowed_actors: str) -> routing.Dispatcher:
    token = os.getenv("GITHUB_TOKEN") or os.getenv("GH_TOKEN") or ""
    if not token:
        raise manual.DeliveryError("GITHUB_TOKEN (or GH_TOKEN) is required for a private repository")
    actors = {x.strip().lower() for x in allowed_actors.split(",") if x.strip()}
    if not actors:
        actors = {repo.split("/")[0].lower()}
    return routing.Dispatcher(routing.GitHub(repo, token), folder, actors, publish=False)


def run_loop(cycle: RoleCycle, *, issue: int | None, interval: int,
             once: bool = False, dry_run: bool = False,
             sleeper: Callable[[float], None] = time.sleep,
             max_rounds: int | None = None) -> int:
    if interval < 10:
        raise manual.DeliveryError("interval must be >= 10 seconds to avoid excessive GitHub polling")
    rounds = 0
    failures = 0
    while True:
        try:
            cycle.scan(issue, dry_run=dry_run)
            failures = 0
        except RuntimeError as exc:
            # GitHub client wraps HTTP/network failures as RuntimeError.
            failures += 1
            cycle.tell(f"GitHub/OS временно недоступен: {exc}")
            cycle.tell("Повторим запрос, не подтверждая никакие поручения.")
        except (manual.DeliveryError, routing.HandoffError, sqlite3.Error, OSError) as exc:
            cycle.tell(f"Ошибка очереди: {exc}. Цикл остановлен; исправьте причину.")
            return 1
        except (KeyboardInterrupt, EOFError):
            cycle.tell("Остановлено пользователем; неподтверждённые задачи сохранены.")
            return 0
        rounds += 1
        if once or cycle.stop_requested or (max_rounds is not None and rounds >= max_rounds):
            return 0
        delay = min(interval * (2 ** min(failures, 3)), 900)
        cycle.tell(f"Ожидание {delay} сек. Ctrl+C для выхода.")
        try:
            sleeper(delay)
        except KeyboardInterrupt:
            cycle.tell("Остановлено пользователем; очередь сохранена.")
            return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--folder", type=Path, default=Path(".dispatcher"))
    parser.add_argument("--repo", default="Melchiorius/backend-interview-labs")
    parser.add_argument("--allowed-actors", default="", help="GitHub logins; default: repository owner")
    sub = parser.add_subparsers(dest="action", required=True)
    sub.add_parser("init-tabs", help="explicitly save role 00..06 to Firefox tabs 1..7")
    sub.add_parser("status", help="show configured tabs and pending messages; no GitHub API")
    run = sub.add_parser("run", help="poll GitHub and manually deliver each ready handoff")
    run.add_argument("--issue", type=int, help="limit scan to a single Issue (pilot)")
    run.add_argument("--interval", type=int, default=120, help="seconds between polls (>=10)")
    run.add_argument("--once", action="store_true", help="one scan only, without waiting")
    run.add_argument("--dry-run", action="store_true", help="scan GitHub; no browser/clipboard/delivery state")
    args = parser.parse_args(argv)
    if args.action == "init-tabs":
        mapping = initialize_tabs(args.folder)
        for role, tab in sorted(mapping.items()):
            print(f"{role} -> вкладка #{tab}")
        print("Сохранено. Перед отправкой визуально проверьте роль открытого чата.")
        return 0
    try:
        mapping = validate_tabs(args.folder)
        if args.action == "status":
            state = manual.DeliveryState(args.folder)
            try:
                for role, tab in sorted(mapping.items()):
                    print(f"{role} -> вкладка #{tab}")
                for role, event, _, _, _, status in manual.pending(args.folder, state):
                    print(f"Ожидает: {role}/{event} [{status}]")
            finally:
                state.close()
            return 0
        if args.issue is not None and args.issue < 1:
            parser.error("--issue must be a positive Issue number")
        if args.interval < 10:
            parser.error("--interval must be >= 10")
        router = make_router(args.repo, args.folder, args.allowed_actors)
        cycle = RoleCycle(router, args.folder)
        try:
            return run_loop(cycle, issue=args.issue, interval=args.interval,
                            once=args.once, dry_run=args.dry_run)
        finally:
            cycle.close()
            router.close()
    except (manual.DeliveryError, sqlite3.Error, OSError, ValueError) as exc:
        print(f"Ошибка: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())