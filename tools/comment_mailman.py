#!/usr/bin/env python3
"""Deliver *links* to addressed GitHub comments, not copies of messages.

Only ordinary Issue / PR conversation comments are scanned (not inline code
reviews or review submissions). Windows + Firefox, one window, roles 00..06.
No ChatGPT API, HTML access, automatic paste, Enter, or GitHub writes.
Python 3.10+, stdlib only.
"""
from __future__ import annotations

import argparse
import ctypes
import json
import os
from pathlib import Path
import re
import sqlite3
import sys
import time
from urllib.parse import urlsplit
from urllib.request import Request, urlopen

ROLES = tuple(f"{i:02d}" for i in range(7))
REPOSITORY = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
# One explicit recipient, near the top. A separate comment for each role.
ADDRESS = re.compile(r"^\s*(?:#{1,6}\s*)?(?:\*\*)?Кому\s*:\s*(0[0-6])\s*(?:\*\*)?\s*$",
                     re.IGNORECASE)


class DeliveryError(RuntimeError):
    """A failed delivery is NOT recorded as processed."""


class WindowFocusDeferred(DeliveryError):
    """Windows blocked focus activation; retry at the next poll."""


def recipient(body: str) -> str | None:
    """Find one standalone 'Кому: 02' header among the first twelve lines."""
    if not isinstance(body, str):
        return None
    found = []
    for line in body.splitlines()[:12]:
        if line.lstrip().startswith(("```", "~~~")):
            break  # no addresses inside code fences
        if (match := ADDRESS.fullmatch(line)):
            found.append(match.group(1))
    return found[0] if len(found) == 1 else None


def github_comment_url(repo: str, item: dict) -> str | None:
    """Accept only a native URL for the exact GitHub comment ID."""
    number, url = item.get("id"), item.get("html_url")
    if (type(number) is not int or number <= 0 or not isinstance(url, str)
            or any(char in url for char in "\r\n\t")):
        return None
    parts = urlsplit(url)
    prefix = f"/{repo}/"
    if (parts.scheme != "https" or parts.netloc != "github.com"
            or parts.username or parts.password or parts.query
            or parts.fragment != f"issuecomment-{number}"
            or not parts.path.startswith(prefix)):
        return None
    remainder = parts.path[len(prefix):]
    if not re.fullmatch(r"(?:issues|pull)/[1-9][0-9]*", remainder):
        return None
    return url


def fetch_comments(repo: str, token: str, pages: int = 3) -> list[dict]:
    """Most recent N*100 Issue/PR conversation comments in this repository."""
    if not REPOSITORY.fullmatch(repo) or not 1 <= pages <= 30:
        raise ValueError("invalid repository or pages")
    result = []
    for page in range(1, pages + 1):
        url = (f"https://api.github.com/repos/{repo}/issues/comments"
               f"?sort=created&direction=desc&per_page=100&page={page}")
        headers = {"Accept": "application/vnd.github+json",
                   "User-Agent": "backend-labs-comment-mailman",
                   "X-GitHub-Api-Version": "2022-11-28"}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        with urlopen(Request(url, headers=headers), timeout=25) as response:
            batch = json.load(response)
        if not isinstance(batch, list):
            raise DeliveryError("GitHub did not return a comments list")
        result.extend(batch)
        if len(batch) < 100:
            break
    return result


def connect_db(path: Path) -> sqlite3.Connection:
    path.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(path)
    conn.execute("CREATE TABLE IF NOT EXISTS seen (comment_id INTEGER PRIMARY KEY)")
    conn.commit()
    return conn


def seen(conn: sqlite3.Connection, comment_id: int) -> bool:
    return conn.execute("SELECT 1 FROM seen WHERE comment_id = ?", (comment_id,)).fetchone() is not None


def remember(conn: sqlite3.Connection, comment_id: int) -> None:
    conn.execute("INSERT OR IGNORE INTO seen(comment_id) VALUES (?)", (comment_id,))
    conn.commit()


def route(conn: sqlite3.Connection, comments: list[dict], repo: str,
          actors: set[str], switch, clipboard, *, bootstrap: bool = False,
          dry_run: bool = False) -> int:
    """Prepare at most one URL per pass. A bootstrap records all existing IDs."""
    eligible = []
    for item in comments:
        cid = item.get("id")
        if type(cid) is not int or seen(conn, cid):
            continue
        if (item.get("user") or {}).get("login", "").casefold() not in actors:
            continue
        role = recipient(item.get("body", ""))
        url = github_comment_url(repo, item)
        if role is not None and url is not None:
            eligible.append((cid, role, url))
    eligible.sort(key=lambda x: x[0])  # oldest pending first
    if bootstrap:
        for cid, _, _ in eligible:
            remember(conn, cid)
        print(f"BOOTSTRAP: {len(eligible)} existing addressed comments marked as seen")
        return len(eligible)
    for cid, role, url in eligible[:1]:
        if dry_run:
            print(f"DRY RUN: role {role}, comment {cid}: {url}")
            return 1
        switch(int(role) + 1)  # 00 -> Firefox tab 1, ... 06 -> tab 7
        clipboard(url)
        remember(conn, cid)  # ONLY after window switch and clipboard success
        print(f"PREPARED role {role}, comment {cid}: {url}")
        print("Вставьте ссылку в чат вручную (Ctrl+V / Shift+Insert, Enter).")
        return 1
    return 0


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


def ensure_firefox_foreground(user, hwnd, *, sleeper=time.sleep) -> None:
    """Respect Windows foreground restrictions; never type into another app.

    SetForegroundWindow may return FALSE even if activation completes later.
    Check actual foreground state, including the already-active case, instead
    of treating the BOOL as a reliable final focus state.
    """
    if user.GetForegroundWindow() == hwnd:
        return
    user.SetForegroundWindow(hwnd)
    for _ in range(3):
        sleeper(0.15)
        if user.GetForegroundWindow() == hwnd:
            return
    raise WindowFocusDeferred(
        "Windows не разрешила активировать Firefox. Поручение НЕ потеряно: "
        "оставьте Firefox активным — цикл повторит попытку на следующем опросе")


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
    ensure_firefox_foreground(user, hwnd)

    INPUT, INPUTUNION, KEYBDINPUT = windows_input_layout()
    expected_size = 40 if ctypes.sizeof(ctypes.c_void_p) == 8 else 28
    if ctypes.sizeof(INPUT) != expected_size:
        raise DeliveryError(f"incorrect Windows INPUT layout: {ctypes.sizeof(INPUT)} bytes")
    user.SendInput.argtypes = (ctypes.c_uint32, ctypes.POINTER(INPUT), ctypes.c_int)
    user.SendInput.restype = ctypes.c_uint32

    def key_event(key, up=False):
        return INPUT(1, INPUTUNION(ki=KEYBDINPUT(key, 0, 0x0002 if up else 0, 0, 0)))

    for control, key in steps:
        if user.GetForegroundWindow() != hwnd:
            raise WindowFocusDeferred(
                "Firefox потерял фокус до нажатия клавиш. "
                "Поручение осталось в очереди; повторим после возврата в Firefox")
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



def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default="Melchiorius/backend-interview-labs")
    parser.add_argument("--db", type=Path,
                        default=Path(".comment-mailman/seen.sqlite3"))
    parser.add_argument("--pages", type=int, default=3,
                        help="number of newest 100-comment pages to scan (1..30)")
    parser.add_argument("--interval", type=int, default=0,
                        help="poll every N seconds; omitted = one pass")
    parser.add_argument("--actor", action="append", default=[],
                        help="additional trusted GitHub login; repo owner is always allowed")
    parser.add_argument("--bootstrap", action="store_true",
                        help="mark existing addressed comments seen, without notifying")
    parser.add_argument("--dry-run", action="store_true",
                        help="print next notification; do not focus/copy/record")
    args = parser.parse_args(argv)
    if not REPOSITORY.fullmatch(args.repo):
        parser.error("expected --repo OWNER/REPO")
    if args.interval < 0 or args.pages not in range(1, 31):
        parser.error("--interval must be >=0; --pages must be 1..30")
    token = os.environ.get("GITHUB_TOKEN", "")
    if not token:
        parser.error("set GITHUB_TOKEN to access the private GitHub repository")
    actors = {args.repo.split("/")[0].casefold(),
              *(a.casefold() for a in args.actor)}
    conn = connect_db(args.db)
    try:
        while True:
            try:
                comments = fetch_comments(args.repo, token, args.pages)
                route(conn, comments, args.repo, actors,
                      switch_firefox_tab, copy_windows_clipboard,
                      bootstrap=args.bootstrap, dry_run=args.dry_run)
            except (OSError, ValueError, DeliveryError) as exc:
                print(f"RETRY: {exc}", file=sys.stderr)
                if not args.interval:
                    return 1
            if args.bootstrap or not args.interval:
                return 0
            time.sleep(args.interval)
    except KeyboardInterrupt:
        return 0
    finally:
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
