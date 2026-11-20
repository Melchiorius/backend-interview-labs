"""Unit tests for the small GitHub-comment mailman; no Windows or network needed."""
from __future__ import annotations

import ctypes
from pathlib import Path
import tempfile
import unittest

from tools import comment_mailman as m

REPO = "Melchiorius/backend-interview-labs"
ACTORS = {"melchiorius"}


def comment(cid, role="02", *, actor="Melchiorius", body=None, link=None):
    return {"id": cid, "user": {"login": actor},
            "body": body if body is not None else f"Кому: {role}\n\nПолное поручение — здесь.",
            "html_url": link or f"https://github.com/{REPO}/issues/51#issuecomment-{cid}"}


class ParserTests(unittest.TestCase):
    def test_simple_and_markdown_headings(self):
        self.assertEqual(m.recipient("Кому: 02\nРабота"), "02")
        self.assertEqual(m.recipient("**Кому: 06**\nРабота"), "06")
        self.assertEqual(m.recipient("## Кому: 00\nРабота"), "00")
        self.assertIsNone(m.recipient("**Кому: 07**"))
        self.assertIsNone(m.recipient("Кому: 02, 03"))
        self.assertIsNone(m.recipient("Кому: 02\nКому: 03"))
        self.assertIsNone(m.recipient("Текст\n" * 12 + "Кому: 02"))
        self.assertIsNone(m.recipient("Задача\n" + "Кому: 02" * 0))
        self.assertIsNone(m.recipient("~~~markdown\nКому: 02\n~~~"))

    def test_only_trusted_exact_comment_url(self):
        self.assertEqual(m.github_comment_url(REPO, comment(67))[-15:],
                         "issuecomment-67")
        self.assertIsNone(m.github_comment_url(REPO, comment(67, link="https://evil.test/")))
        self.assertIsNone(m.github_comment_url(REPO, comment(67, link=
            f"https://github.com/{REPO}/issues/51#issuecomment-68")))
        self.assertIsNone(m.github_comment_url(REPO, comment(67, link=
            f"https://github.com/other/repo/issues/51#issuecomment-67")))
        self.assertIsNone(m.github_comment_url(REPO, comment(67, link=
            f"https://github.com/{REPO}/issues/51#issuecomment-67\n")))

    def test_win32_layout_remains_valid(self):
        pointer32 = m.windows_input_layout(ctypes.c_uint32)
        pointer64 = m.windows_input_layout(ctypes.c_uint64)
        self.assertEqual(ctypes.sizeof(pointer32[0]), 28)
        self.assertEqual(ctypes.sizeof(pointer64[0]), 40)


class DispatchTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.db = m.connect_db(Path(self.tmp.name) / "seen.sqlite3")
        self.calls = []

    def tearDown(self):
        self.db.close()
        self.tmp.cleanup()

    def run_route(self, comments, *, bootstrap=False, dry_run=False,
                  copy_fails=False, focus_fails=False):
        def switch(tab):
            self.calls.append(("tab", tab))
            if focus_fails:
                raise m.WindowFocusDeferred("focus denied")
        def clipboard(url):
            self.calls.append(("clipboard", url))
            if copy_fails:
                raise OSError("clipboard busy")
        return m.route(self.db, comments, REPO, ACTORS, switch, clipboard,
                       bootstrap=bootstrap, dry_run=dry_run)

    def test_one_comment_per_pass_and_database_contains_only_id(self):
        comments = [comment(72, "03"), comment(71, "02")]
        self.assertEqual(self.run_route(comments), 1)
        self.assertTrue(m.seen(self.db, 71))
        self.assertFalse(m.seen(self.db, 72))
        self.assertEqual(self.calls[0], ("tab", 3))
        self.assertEqual(self.calls[1], ("clipboard", comment(71)["html_url"]))
        self.assertEqual(self.db.execute("PRAGMA table_info(seen)").fetchall()[0][1],
                         "comment_id")
        self.assertEqual(len(self.db.execute("PRAGMA table_info(seen)").fetchall()), 1)
        self.assertEqual(self.run_route(comments), 1)
        self.assertTrue(m.seen(self.db, 72))
        self.assertEqual(self.run_route(comments), 0)

    def test_author_and_header_filter(self):
        self.assertEqual(self.run_route([
            comment(71, actor="unknown"),
            comment(72, body="Роль 02\nВ тексте лишь упоминание адресата"),
            comment(73, "06"),
        ]), 1)
        self.assertEqual(self.calls[0], ("tab", 7))
        self.assertFalse(m.seen(self.db, 71))
        self.assertFalse(m.seen(self.db, 72))

    def test_failure_never_marks_seen_and_can_retry(self):
        item = [comment(88)]
        with self.assertRaises(m.WindowFocusDeferred):
            self.run_route(item, focus_fails=True)
        self.assertFalse(m.seen(self.db, 88))
        with self.assertRaises(OSError):
            self.run_route(item, copy_fails=True)
        self.assertFalse(m.seen(self.db, 88))
        self.assertEqual(self.run_route(item), 1)
        self.assertTrue(m.seen(self.db, 88))

    def test_dry_run_does_not_consume(self):
        self.assertEqual(self.run_route([comment(67)], dry_run=True), 1)
        self.assertEqual(self.calls, [])
        self.assertFalse(m.seen(self.db, 67))

    def test_bootstrap_records_existing_ids_without_actions(self):
        self.assertEqual(self.run_route([comment(68), comment(67)],
                                        bootstrap=True), 2)
        self.assertEqual(self.calls, [])
        self.assertTrue(m.seen(self.db, 67))
        self.assertTrue(m.seen(self.db, 68))


if __name__ == "__main__":
    unittest.main()
