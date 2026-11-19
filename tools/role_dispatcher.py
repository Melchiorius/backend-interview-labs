#!/usr/bin/env python3
"""Route explicit role handoffs from GitHub Issue comments; never drive ChatGPT UI.

Python >=3.10, standard library only. Default is read-only on GitHub and writes
local inbox files. --publish posts marked delivery comments back to the Issue.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import sqlite3
import sys
import time
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, urlopen

ROLES = {f"{n:02d}" for n in range(7)}
GATES = {f"G{n}" for n in range(7)}
ACTIONS = {"READY", "BLOCKED", "QUESTION"}
REQUIRES = {"issue_open", "pr_open", "pr_merged", "head_matches", "ci_success"}
FIELDS = {"protocol", "event_id", "from", "to", "issue", "gate", "action", "message",
          "pr", "head_sha", "requires"}
MANDATORY = {"protocol", "event_id", "from", "to", "issue", "gate", "action", "message"}
FENCE = re.compile(r"(?ms)^```backend-handoff[ \t]*\r?\n(.*?)\r?\n```[ \t]*$")
ID = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{7,95}$")
SHA = re.compile(r"^[a-fA-F0-9]{40}$")
REPO = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")


class HandoffError(ValueError):
    """Malformed or unsafe handoff: do not deliver."""


def parse_handoff(body: str) -> dict[str, Any] | None:
    """Exactly one fenced JSON block per comment; otherwise ignore or reject."""
    blocks = FENCE.findall(body)
    if not blocks:
        return None
    if len(blocks) != 1:
        raise HandoffError("expected exactly one backend-handoff block per comment")
    if len(blocks[0]) > 12000:
        raise HandoffError("handoff exceeds 12 KB")
    try:
        obj = json.loads(blocks[0])
    except json.JSONDecodeError as exc:
        raise HandoffError(f"invalid JSON: {exc}") from exc
    if not isinstance(obj, dict) or not MANDATORY <= obj.keys() or obj.keys() - FIELDS:
        raise HandoffError("missing required or unknown fields")
    if obj["protocol"] != "backend-labs-handoff/v1":
        raise HandoffError("unsupported protocol")
    if not isinstance(obj["event_id"], str) or not ID.fullmatch(obj["event_id"]):
        raise HandoffError("invalid event_id")
    if obj["from"] not in ROLES or obj["gate"] not in GATES or obj["action"] not in ACTIONS:
        raise HandoffError("unknown role, gate or action")
    targets = obj["to"]
    if not isinstance(targets, list) or not targets or len(set(map(str, targets))) != len(targets):
        raise HandoffError("to must be a nonempty list without duplicates")
    if any(not isinstance(t, str) or t not in ROLES or t == obj["from"] for t in targets):
        raise HandoffError("invalid target or self-delivery")
    if type(obj["issue"]) is not int or obj["issue"] < 1:
        raise HandoffError("issue must be a positive integer")
    if not isinstance(obj["message"], str) or not 1 <= len(obj["message"]) <= 6000:
        raise HandoffError("message must be 1..6000 characters")
    if "pr" in obj and (type(obj["pr"]) is not int or obj["pr"] < 1):
        raise HandoffError("pr must be a positive integer")
    if "head_sha" in obj and (not isinstance(obj["head_sha"], str)
                               or not SHA.fullmatch(obj["head_sha"])):
        raise HandoffError("head_sha must contain 40 hexadecimal characters")
    conditions = obj.get("requires", [])
    if (not isinstance(conditions, list) or len(conditions) != len(set(map(str, conditions)))
            or any(not isinstance(c, str) or c not in REQUIRES for c in conditions)):
        raise HandoffError("unsupported requires values")
    if {"pr_open", "pr_merged", "head_matches", "ci_success"} & set(conditions) and "pr" not in obj:
        raise HandoffError("PR-related requires need pr")
    if "head_matches" in conditions and "head_sha" not in obj:
        raise HandoffError("head_matches needs head_sha")
    if "ci_success" in conditions and "head_sha" not in obj:
        raise HandoffError("ci_success needs head_sha")
    if "pr_open" in conditions and "pr_merged" in conditions:
        raise HandoffError("pr_open and pr_merged cannot both be required")
    return obj


class GitHub:
    def __init__(self, repo: str, token: str = ""):
        if not REPO.fullmatch(repo):
            raise ValueError("repository must be owner/name")
        self.repo = repo
        self.token = token
        self.base = f"https://api.github.com/repos/{repo}"

    def request(self, method: str, path: str, payload: dict | None = None) -> Any:
        data = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
        headers = {"Accept": "application/vnd.github+json", "User-Agent": "backend-labs-role-dispatcher",
                   "X-GitHub-Api-Version": "2022-11-28"}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        if data is not None:
            headers["Content-Type"] = "application/json"
        req = Request(self.base + path, data=data, headers=headers, method=method)
        try:
            with urlopen(req, timeout=25) as response:
                return json.load(response)
        except HTTPError as exc:
            details = exc.read(300).decode("utf-8", "replace")
            raise RuntimeError(f"GitHub HTTP {exc.code} on {method} {path}: {details}") from exc
        except URLError as exc:
            raise RuntimeError(f"GitHub unavailable: {exc}") from exc

    def pages(self, path: str):
        page = 1
        while True:
            separator = "&" if "?" in path else "?"
            results = self.request("GET", f"{path}{separator}per_page=100&page={page}")
            if not isinstance(results, list):
                raise RuntimeError("GitHub expected a list")
            yield from results
            if len(results) < 100:
                break
            page += 1

    def issues(self, selected: int | None):
        if selected is not None:
            issue = self.request("GET", f"/issues/{selected}")
            if "pull_request" in issue:
                raise RuntimeError("--issue must point to a GitHub Issue, not a PR")
            return [issue]
        return [x for x in self.pages("/issues?state=open") if "pull_request" not in x]

    def comments(self, issue: int):
        return list(self.pages(f"/issues/{issue}/comments"))

    def pr(self, number: int):
        return self.request("GET", f"/pulls/{number}")

    def check_runs(self, sha: str):
        return self.request("GET", f"/commits/{quote(sha)}/check-runs?per_page=100")

    def publish(self, issue: int, body: str):
        return self.request("POST", f"/issues/{issue}/comments", {"body": body})


def check_conditions(gh: GitHub, obj: dict, issue: dict) -> list[str]:
    """Only verify external machine-checkable facts, NEVER content quality/gates."""
    problems = []
    conditions = obj.get("requires", [])
    if "issue_open" in conditions and issue.get("state") != "open":
        problems.append("issue is not open")
    pr = None
    if "pr" in obj:
        pr = gh.pr(obj["pr"])
        # Always guard specified commit SHA against a changing PR head.
        if "head_sha" in obj and pr.get("head", {}).get("sha", "").lower() != obj["head_sha"].lower():
            problems.append("PR head_sha differs from supplied head_sha")
    if "pr_open" in conditions and (pr is None or pr.get("state") != "open"):
        problems.append("PR is not open")
    if "pr_merged" in conditions and (pr is None or not pr.get("merged_at")):
        problems.append("PR has not been merged")
    if "ci_success" in conditions:
        checks = gh.check_runs(obj["head_sha"])
        runs = checks.get("check_runs", [])
        if not runs or checks.get("total_count", len(runs)) > len(runs):
            problems.append("CI check runs missing or paginated; cannot confirm success")
        elif any(r.get("status") != "completed" or r.get("conclusion") != "success" for r in runs):
            problems.append("not all commit check runs completed successfully")
    return problems


def make_markdown(obj: dict, target: str, issue_url: str, comment_url: str) -> str:
    pr_line = f"PR: #{obj['pr']}\n" if "pr" in obj else ""
    sha_line = f"HEAD: `{obj['head_sha']}`\n" if "head_sha" in obj else ""
    # The message is *data*, never executed by the dispatcher.
    return (f"# Поручение роли {target}\n\n"
            f"Событие: `{obj['event_id']}` · источник: {obj['from']} · "
            f"этап: {obj['gate']} · тип: {obj['action']}\n\n"
            f"Issue: {issue_url}\n{pr_line}{sha_line}"
            f"Источник сообщения: {comment_url}\n\n"
            "Перед началом повторно сверьте фактический GitHub, Issue, PR и SHA. "
            "Это автоматическая доставка, а не доказательство качества или завершения gate.\n\n"
            f"## Задание\n\n{obj['message']}\n")


def atomic_write(path: Path, content: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_name(path.name + ".tmp")
    temp.write_text(content, encoding="utf-8")
    temp.replace(path)


class Dispatcher:
    def __init__(self, gh: GitHub, folder: Path, actors: set[str], publish: bool = False):
        self.gh = gh
        self.folder = folder
        self.actors = actors
        self.publish = publish
        folder.mkdir(parents=True, exist_ok=True)
        self.db = sqlite3.connect(folder / "dispatch.sqlite3")
        self.db.execute("CREATE TABLE IF NOT EXISTS events (event_id TEXT PRIMARY KEY, digest TEXT NOT NULL)")
        self.db.execute("CREATE TABLE IF NOT EXISTS deliveries (event_id TEXT NOT NULL, target TEXT NOT NULL, "
                        "posted INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(event_id,target))")
        self.db.commit()

    def close(self):
        self.db.close()

    def scan(self, selected: int | None = None) -> dict[str, int]:
        totals = {"new": 0, "waiting": 0, "invalid": 0, "published": 0}
        for issue in self.gh.issues(selected):
            n = issue["number"]
            for comment in self.gh.comments(n):
                actor = (comment.get("user") or {}).get("login", "")
                if actor.lower() not in self.actors:
                    continue
                try:
                    obj = parse_handoff(comment.get("body") or "")
                    if obj is None:
                        continue
                    if obj["issue"] != n:
                        raise HandoffError("comment Issue and payload Issue differ")
                    digest = hashlib.sha256(json.dumps(obj, sort_keys=True, ensure_ascii=False).encode()).hexdigest()
                    existing = self.db.execute("SELECT digest FROM events WHERE event_id=?", (obj["event_id"],)).fetchone()
                    if existing and existing[0] != digest:
                        raise HandoffError("event_id reused with different payload")
                    problems = check_conditions(self.gh, obj, issue)
                    if problems:
                        print(f"WAIT {obj['event_id']} (Issue #{n}): {'; '.join(problems)}")
                        totals["waiting"] += 1
                        continue
                    self.db.execute("INSERT OR IGNORE INTO events (event_id,digest) VALUES (?,?)",
                                    (obj["event_id"], digest))
                    for target in obj["to"]:
                        path = self.folder / "inbox" / target / f"{obj['event_id']}.md"
                        markdown = make_markdown(obj, target, issue["html_url"], comment.get("html_url", ""))
                        before = path.exists()
                        atomic_write(path, markdown)
                        self.db.execute("INSERT OR IGNORE INTO deliveries(event_id,target) VALUES (?,?)",
                                        (obj["event_id"], target))
                        if not before:
                            totals["new"] += 1
                            print(f"INBOX {target}: {path}")
                        if self.publish:
                            posted = self.db.execute("SELECT posted FROM deliveries WHERE event_id=? AND target=?",
                                                     (obj["event_id"], target)).fetchone()[0]
                            if not posted:
                                marker = f"<!-- backend-delivery:v1:{obj['event_id']}:{target} -->"
                                # Before retrying, check for a successful previous POST after a process crash.
                                already = any(marker in (c.get("body") or "") for c in self.gh.comments(n))
                                if not already:
                                    self.gh.publish(n, marker + "\n" + markdown)
                                    totals["published"] += 1
                                self.db.execute("UPDATE deliveries SET posted=1 WHERE event_id=? AND target=?",
                                                (obj["event_id"], target))
                    self.db.commit()
                except (HandoffError, KeyError, TypeError, RuntimeError) as exc:
                    totals["invalid"] += 1
                    print(f"REJECT Issue #{n}, comment {comment.get('id')}: {exc}", file=sys.stderr)
        return totals


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default="Melchiorius/backend-interview-labs")
    parser.add_argument("--issue", type=int, help="scan one Issue, including closed ones")
    parser.add_argument("--folder", type=Path, default=Path(".dispatcher"))
    parser.add_argument("--allowed-actors", default="", help="comma-separated GitHub logins; default: repo owner")
    parser.add_argument("--publish", action="store_true", help="opt-in POST delivery comments to GitHub")
    parser.add_argument("--interval", type=int, default=0, help="poll every N seconds, 0 means once")
    args = parser.parse_args(argv)
    if args.interval < 0:
        parser.error("--interval must be >= 0")
    token = os.getenv("GITHUB_TOKEN") or os.getenv("GH_TOKEN") or ""
    if args.publish and not token:
        parser.error("--publish requires GITHUB_TOKEN or GH_TOKEN")
    gh = GitHub(args.repo, token)
    actors = {x.strip().lower() for x in args.allowed_actors.split(",") if x.strip()}
    if not actors:
        actors = {args.repo.split("/")[0].lower()}
    dispatcher = Dispatcher(gh, args.folder, actors, publish=args.publish)
    try:
        while True:
            counts = dispatcher.scan(args.issue)
            print("Scan complete:", ", ".join(f"{k}={v}" for k, v in counts.items()))
            if args.interval == 0:
                break
            time.sleep(args.interval)
    except (KeyboardInterrupt, RuntimeError, OSError) as exc:
        print(f"Stopped: {exc}", file=sys.stderr)
        return 1
    finally:
        dispatcher.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
