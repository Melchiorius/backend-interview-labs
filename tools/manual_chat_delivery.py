#!/usr/bin/env python3
"""Human-confirmed ChatGPT handoff delivery; no browser instrumentation/API.

Consumes local inbox created by role_dispatcher.py. Opens only a configured chat
URL in the default browser and copies text to Windows clipboard. The user must
paste, send, and explicitly acknowledge delivery. Python 3.10+, stdlib only.
"""
from __future__ import annotations

import argparse
import ctypes
import hashlib
import json
import os
from pathlib import Path
import re
import sqlite3
import sys
import time
from urllib.parse import urlparse
import webbrowser

ROLES = tuple(f"{i:02d}" for i in range(7))
EVENT_ID = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{7,95}\Z")
CHAT_ID = re.compile(r"(?:/g/[a-zA-Z0-9_-]+)?/c/[a-zA-Z0-9-]+/?\Z")
MAX_MESSAGE = 30_000


class DeliveryError(ValueError):
    """An invalid or ambiguous delivery must not be silently acknowledged."""


def validate_chat_url(url: str) -> str:
    if not isinstance(url, str):
        raise DeliveryError("chat URL must be text")
    parsed = urlparse(url.strip())
    if (parsed.scheme != "https" or parsed.netloc.lower() not in
            {"chatgpt.com", "chat.openai.com"} or parsed.username or parsed.password
            or parsed.query or parsed.fragment or not CHAT_ID.fullmatch(parsed.path)):
        raise DeliveryError("expected an HTTPS URL to an individual ChatGPT chat (/c/...) ")
    return parsed.geturl()


def read_mapping(folder: Path) -> dict[str, str]:
    path = folder / "chat_urls.json"
    if not path.exists():
        return {}
    try:
        mapping = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise DeliveryError(f"invalid mapping {path}: {exc}") from exc
    if not isinstance(mapping, dict) or any(k not in ROLES for k in mapping):
        raise DeliveryError("mapping must be a role -> URL JSON object")
    return {key: validate_chat_url(value) for key, value in mapping.items()}


def write_mapping(folder: Path, mapping: dict[str, str]) -> None:
    folder.mkdir(parents=True, exist_ok=True)
    target = folder / "chat_urls.json"
    temporary = folder / "chat_urls.json.tmp"
    temporary.write_text(json.dumps(mapping, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temporary.replace(target)


def validate_tab_index(index: int) -> int:
    if type(index) is not int or not 1 <= index <= 64:
        raise DeliveryError("tab number must be in 1..64")
    return index


def tab_steps(index: int) -> list[tuple[int, int]]:
    """Firefox physical tab order: Ctrl+1..8, otherwise Ctrl+1 then Ctrl+PgDn."""
    index = validate_tab_index(index)
    if index <= 8:
        return [(0x11, ord(str(index)))]
    return [(0x11, ord("1"))] + [(0x11, 0x22)] * (index - 1)


def read_tab_mapping(folder: Path) -> dict[str, int]:
    path = folder / "chat_tabs.json"
    if not path.exists():
        return {}
    try:
        mapping = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise DeliveryError(f"invalid tab mapping {path}: {exc}") from exc
    if not isinstance(mapping, dict) or any(k not in ROLES for k in mapping):
        raise DeliveryError("tab mapping must be a role -> number JSON object")
    return {role: validate_tab_index(index) for role, index in mapping.items()}


def configure_tab(folder: Path, role: str, index: int) -> None:
    if role not in ROLES:
        raise DeliveryError("unknown role")
    mapping = read_tab_mapping(folder)
    mapping[role] = validate_tab_index(index)
    folder.mkdir(parents=True, exist_ok=True)
    path = folder / "chat_tabs.json"
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_text(json.dumps(mapping, indent=2) + "\n", encoding="utf-8")
    temporary.replace(path)
    print(f"Роль {role} -> вкладка #{index} (слева направо в одном окне Firefox)")


def windows_input_layout(pointer_type=ctypes.c_size_t):
    """Mirror Win32 INPUT union, including its largest (mouse) member.

    Fixed-width Win32 integers make this layout testable even on non-Windows
    hosts. A 64-bit INPUT is 40 bytes, a 32-bit INPUT is 28 bytes.
    """
    DWORD = ctypes.c_uint32
    LONG = ctypes.c_int32
    WORD = ctypes.c_uint16

    class MOUSEINPUT(ctypes.Structure):
        _fields_ = [("dx", LONG), ("dy", LONG), ("mouseData", DWORD),
                    ("dwFlags", DWORD), ("time", DWORD),
                    ("dwExtraInfo", pointer_type)]

    class KEYBDINPUT(ctypes.Structure):
        _fields_ = [("wVk", WORD), ("wScan", WORD),
                    ("dwFlags", DWORD), ("time", DWORD),
                    ("dwExtraInfo", pointer_type)]

    class HARDWAREINPUT(ctypes.Structure):
        _fields_ = [("uMsg", DWORD), ("wParamL", WORD), ("wParamH", WORD)]

    class INPUTUNION(ctypes.Union):
        _fields_ = [("mi", MOUSEINPUT), ("ki", KEYBDINPUT), ("hi", HARDWAREINPUT)]

    class INPUT(ctypes.Structure):
        _fields_ = [("type", DWORD), ("u", INPUTUNION)]

    return INPUT, INPUTUNION, KEYBDINPUT


def switch_firefox_tab(index: int) -> None:
    """Use Windows keystrokes, not Firefox DOM or Marionette; reject multiple windows."""
    steps = tab_steps(index)
    if os.name != "nt":
        raise DeliveryError("automatic tab switching is Windows-only")
    from ctypes import wintypes
    user = ctypes.WinDLL("user32", use_last_error=True)
    proc = ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM)
    user.EnumWindows.argtypes = (proc, wintypes.LPARAM)
    user.GetClassNameW.argtypes = (wintypes.HWND, wintypes.LPWSTR, ctypes.c_int)
    user.GetClassNameW.restype = ctypes.c_int
    user.IsWindowVisible.argtypes = (wintypes.HWND,)
    user.IsWindowVisible.restype = wintypes.BOOL
    user.IsIconic.argtypes = (wintypes.HWND,)
    user.IsIconic.restype = wintypes.BOOL
    user.ShowWindow.argtypes = (wintypes.HWND, ctypes.c_int)
    user.SetForegroundWindow.argtypes = (wintypes.HWND,)
    user.SetForegroundWindow.restype = wintypes.BOOL
    user.GetForegroundWindow.restype = wintypes.HWND
    windows = []

    @proc
    def visit(hwnd, unused):
        buf = ctypes.create_unicode_buffer(128)
        user.GetClassNameW(hwnd, buf, len(buf))
        if buf.value == "MozillaWindowClass" and user.IsWindowVisible(hwnd):
            windows.append(hwnd)
        return True

    if not user.EnumWindows(visit, 0):
        raise DeliveryError("could not enumerate Firefox windows")
    if len(windows) != 1:
        raise DeliveryError(f"expected exactly one visible Firefox window; found {len(windows)}")
    hwnd = windows[0]
    if user.IsIconic(hwnd):
        user.ShowWindow(hwnd, 9)  # SW_RESTORE
    if not user.SetForegroundWindow(hwnd):
        raise DeliveryError("Windows refused to activate Firefox; select it manually")
    time.sleep(0.2)
    if user.GetForegroundWindow() != hwnd:
        raise DeliveryError("Firefox is not foreground; no keys sent")

    INPUT, INPUTUNION, KEYBDINPUT = windows_input_layout()
    expected_size = 40 if ctypes.sizeof(ctypes.c_void_p) == 8 else 28
    if ctypes.sizeof(INPUT) != expected_size:
        raise DeliveryError(f"incorrect Windows INPUT layout: {ctypes.sizeof(INPUT)} bytes")
    user.SendInput.argtypes = (ctypes.c_uint32, ctypes.POINTER(INPUT), ctypes.c_int)
    user.SendInput.restype = ctypes.c_uint32

    def key_event(key, up=False):
        return INPUT(1, INPUTUNION(ki=KEYBDINPUT(key, 0, 0x0002 if up else 0, 0, 0)))

    for control, key in steps:
        sequence = (INPUT * 4)(key_event(control), key_event(key),
                               key_event(key, True), key_event(control, True))
        ctypes.set_last_error(0)
        inserted = user.SendInput(4, sequence, ctypes.sizeof(INPUT))
        if inserted != 4:
            code = ctypes.get_last_error()
            explanation = ctypes.FormatError(code).strip() if code else (
                "no Win32 error code; possibly blocked by Windows input permissions")
            raise DeliveryError(
                f"SendInput inserted {inserted}/4 keys; WinError {code}: {explanation}; "
                f"sizeof(INPUT)={ctypes.sizeof(INPUT)}")
        time.sleep(0.09)


def list_inbox(folder: Path) -> list[tuple[str, str, Path]]:
    result = []
    inbox = folder / "inbox"
    if not inbox.exists():
        return result
    for role in ROLES:
        role_dir = inbox / role
        if not role_dir.is_dir():
            continue
        for entry in role_dir.glob("*.md"):
            if not entry.is_file() or not EVENT_ID.fullmatch(entry.stem):
                continue
            if entry.stat().st_size > MAX_MESSAGE * 4:
                raise DeliveryError(f"oversized inbox file: {entry}")
            result.append((role, entry.stem, entry))
    return sorted(result, key=lambda x: (x[2].stat().st_mtime, x[0], x[1]))


def read_message(path: Path) -> tuple[str, str]:
    message = path.read_text(encoding="utf-8-sig")
    if not message.strip() or len(message) > MAX_MESSAGE:
        raise DeliveryError(f"empty or oversized message: {path}")
    digest = hashlib.sha256(message.encode("utf-8")).hexdigest()
    return message, digest


def copy_windows_clipboard(message: str) -> None:
    """Copy Unicode using Win32 clipboard APIs. No shell and no Selenium."""
    if os.name != "nt":
        raise DeliveryError("clipboard helper supports Windows only")
    from ctypes import wintypes

    kernel = ctypes.WinDLL("kernel32", use_last_error=True)
    user = ctypes.WinDLL("user32", use_last_error=True)
    kernel.GlobalAlloc.argtypes = (wintypes.UINT, ctypes.c_size_t)
    kernel.GlobalAlloc.restype = ctypes.c_void_p
    kernel.GlobalLock.argtypes = (ctypes.c_void_p,)
    kernel.GlobalLock.restype = ctypes.c_void_p
    kernel.GlobalUnlock.argtypes = (ctypes.c_void_p,)
    kernel.GlobalUnlock.restype = wintypes.BOOL
    kernel.GlobalFree.argtypes = (ctypes.c_void_p,)
    kernel.GlobalFree.restype = ctypes.c_void_p
    user.OpenClipboard.argtypes = (wintypes.HWND,)
    user.OpenClipboard.restype = wintypes.BOOL
    user.EmptyClipboard.restype = wintypes.BOOL
    user.SetClipboardData.argtypes = (wintypes.UINT, ctypes.c_void_p)
    user.SetClipboardData.restype = ctypes.c_void_p
    user.CloseClipboard.restype = wintypes.BOOL

    # create_unicode_buffer includes terminal NUL, even for Cyrillic text.
    data = ctypes.create_unicode_buffer(message)
    handle = kernel.GlobalAlloc(0x0002, ctypes.sizeof(data))  # GMEM_MOVEABLE
    if not handle:
        raise OSError("GlobalAlloc failed")
    pointer = kernel.GlobalLock(handle)
    if not pointer:
        kernel.GlobalFree(handle)
        raise OSError("GlobalLock failed")
    ctypes.memmove(pointer, ctypes.addressof(data), ctypes.sizeof(data))
    kernel.GlobalUnlock(handle)
    if not user.OpenClipboard(None):
        kernel.GlobalFree(handle)
        raise OSError("OpenClipboard failed; close clipboard manager and retry")
    transferred = False
    try:
        if not user.EmptyClipboard():
            raise OSError("EmptyClipboard failed")
        if not user.SetClipboardData(13, handle):  # CF_UNICODETEXT
            raise OSError("SetClipboardData failed")
        transferred = True  # clipboard now owns the memory
    finally:
        user.CloseClipboard()
        if not transferred:
            kernel.GlobalFree(handle)


class DeliveryState:
    def __init__(self, folder: Path):
        folder.mkdir(parents=True, exist_ok=True)
        self.conn = sqlite3.connect(folder / "manual_delivery.sqlite3")
        self.conn.execute("CREATE TABLE IF NOT EXISTS delivery ("
                          "role TEXT NOT NULL, event_id TEXT NOT NULL, digest TEXT NOT NULL, "
                          "status TEXT NOT NULL CHECK(status IN ('prepared','confirmed')), "
                          "PRIMARY KEY(role, event_id))")
        self.conn.commit()

    def close(self) -> None:
        self.conn.close()

    def get(self, role: str, event_id: str) -> tuple[str, str] | None:
        return self.conn.execute("SELECT digest,status FROM delivery WHERE role=? AND event_id=?",
                                 (role, event_id)).fetchone()

    def track(self, role: str, event_id: str, digest: str) -> None:
        old = self.get(role, event_id)
        if old and old[0] != digest:
            raise DeliveryError("message changed after preparation; new event_id required")
        self.conn.execute("INSERT OR IGNORE INTO delivery(role,event_id,digest,status) "
                          "VALUES(?,?,?,'prepared')", (role, event_id, digest))
        self.conn.commit()

    def confirm(self, role: str, event_id: str, digest: str) -> None:
        old = self.get(role, event_id)
        if not old:
            raise DeliveryError("not prepared: run prepare before confirm")
        if old[0] != digest:
            raise DeliveryError("message changed after preparation; cannot confirm")
        self.conn.execute("UPDATE delivery SET status='confirmed' WHERE role=? AND event_id=?",
                          (role, event_id))
        self.conn.commit()


def pending(folder: Path, state: DeliveryState, role: str | None = None):
    items = []
    for current_role, event, path in list_inbox(folder):
        if role and current_role != role:
            continue
        msg, digest = read_message(path)
        row = state.get(current_role, event)
        if row and row[0] != digest:
            raise DeliveryError(f"event {event} changed after preparing it")
        if row and row[1] == "confirmed":
            continue
        items.append((current_role, event, path, msg, digest, row[1] if row else "new"))
    return items


def prepare(folder: Path, state: DeliveryState, role: str | None, event: str | None,
            *, dry_run: bool = False, open_chat: bool = True, use_tab: bool = False,
            clipboard=copy_windows_clipboard, opener=webbrowser.open,
            tab_switcher=switch_firefox_tab, show_confirm_hint: bool = True) -> tuple[str, str] | None:
    items = pending(folder, state, role)
    if event:
        items = [x for x in items if x[1] == event]
    if not items:
        print("Нет ожидающих поручений.")
        return None
    if event and not role and len(items) != 1:
        raise DeliveryError("ambiguous event_id: also supply --role")
    current_role, event_id, path, message, digest, status = items[0]
    if use_tab and not open_chat:
        raise DeliveryError("use_tab and no_open cannot be combined")
    if use_tab:
        mapping = read_tab_mapping(folder)
        if current_role not in mapping:
            raise DeliveryError(f"no tab number for role {current_role}; run configure-tab")
        tab = mapping[current_role]
        destination = f"Вкладка #{tab} (одного окна Firefox)"
    else:
        mapping = read_mapping(folder)
        if current_role not in mapping:
            raise DeliveryError(f"no URL configured for role {current_role}; run configure")
        url = mapping[current_role]
        destination = f"Чат: {url}"
    print(f"Роль: {current_role} | Событие: {event_id} | Статус: {status}")
    print(f"{destination}\nФайл: {path}\nДлина: {len(message)} символов")
    if dry_run:
        print("ПРОВЕРКА: браузер и буфер обмена не изменены.")
        return current_role, event_id
    if use_tab:
        tab_switcher(tab)
    clipboard(message)
    if open_chat and not use_tab:
        if not opener(url):
            print("Не удалось подтвердить открытие браузера; текст уже в буфере обмена.",
                  file=sys.stderr)
    state.track(current_role, event_id, digest)
    if use_tab:
        print("Переключение на существующую вкладку выполнено; ПРОВЕРЬТЕ адрес и роль,")
        print("затем вручную нажмите Ctrl+V и Enter. Никакого HTML-анализа или отправки не было.")
    elif open_chat:
        print("Текст скопирован. Проверьте адрес открытого чата, нажмите Ctrl+V и Enter.")
    else:
        print("Текст скопирован. Переключитесь на существующую вкладку нужного чата,")
        print("проверьте её адрес, нажмите Ctrl+V и Enter. Новых вкладок скрипт не создавал.")
    if show_confirm_hint:
        print(f"ПОСЛЕ отправки выполните: py tools/manual_chat_delivery.py confirm --role {current_role} --event {event_id}")
        print("Если отправка не состоялась, НЕ подтверждайте: поручение останется в очереди.")
    else:
        print("Цикл не ждёт подтверждения. Статус prepared означает только подготовку текста.")
        print("Если не успели вставить сообщение, повторите вручную: "
              f"py tools/manual_chat_delivery.py prepare --role {current_role} "
              f"--event {event_id} --use-tab")
    return current_role, event_id


def configure(folder: Path, role: str, url: str) -> None:
    if role not in ROLES:
        raise DeliveryError("unknown role")
    mapping = read_mapping(folder)
    mapping[role] = validate_chat_url(url)
    write_mapping(folder, mapping)
    print(f"Чат роли {role} сохранён в {folder / 'chat_urls.json'}")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--folder", type=Path, default=Path(".dispatcher"))
    commands = parser.add_subparsers(dest="command", required=True)
    c = commands.add_parser("configure", help="save the URL of an existing ChatGPT conversation")
    c.add_argument("--role", required=True, choices=ROLES)
    c.add_argument("--url", required=True)
    ct = commands.add_parser("configure-tab", help="bind role to physical tab number in Firefox")
    ct.add_argument("--role", required=True, choices=ROLES)
    ct.add_argument("--tab", required=True, type=int)
    q = commands.add_parser("status", help="list messages awaiting manual confirmation")
    q.add_argument("--role", choices=ROLES)
    p = commands.add_parser("prepare", help="open chat and copy next message; no sending")
    p.add_argument("--role", choices=ROLES)
    p.add_argument("--event")
    p.add_argument("--dry-run", action="store_true")
    group = p.add_mutually_exclusive_group()
    group.add_argument("--no-open", action="store_true",
                       help="copy only: do not open a URL or create a browser tab")
    group.add_argument("--use-tab", action="store_true",
                       help="select existing Firefox tab by configured physical index")
    a = commands.add_parser("confirm", help="mark a pasted-and-sent message as confirmed")
    a.add_argument("--role", required=True, choices=ROLES)
    a.add_argument("--event", required=True)
    args = parser.parse_args(argv)
    if args.command == "configure":
        try:
            configure(args.folder, args.role, args.url)
            return 0
        except (DeliveryError, OSError) as exc:
            parser.error(str(exc))
    if args.command == "configure-tab":
        try:
            configure_tab(args.folder, args.role, args.tab)
            return 0
        except (DeliveryError, OSError) as exc:
            parser.error(str(exc))
    state = DeliveryState(args.folder)
    try:
        if args.command == "status":
            items = pending(args.folder, state, args.role)
            for role, event, _, _, _, status in items:
                print(f"{role}: {event} [{status}]")
            print(f"Ожидают: {len(items)}")
        elif args.command == "prepare":
            if args.event and not EVENT_ID.fullmatch(args.event):
                raise DeliveryError("invalid event_id")
            prepare(args.folder, state, args.role, args.event, dry_run=args.dry_run,
                    open_chat=not args.no_open, use_tab=args.use_tab)
        elif args.command == "confirm":
            if not EVENT_ID.fullmatch(args.event):
                raise DeliveryError("invalid event_id")
            path = args.folder / "inbox" / args.role / f"{args.event}.md"
            if not path.is_file():
                raise DeliveryError("no inbox message for that role/event")
            _, digest = read_message(path)
            state.confirm(args.role, args.event, digest)
            print(f"Подтверждено вручную: {args.role}/{args.event}")
    except (DeliveryError, OSError, UnicodeError, sqlite3.Error) as exc:
        print(f"Ошибка: {exc}", file=sys.stderr)
        return 1
    finally:
        state.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())