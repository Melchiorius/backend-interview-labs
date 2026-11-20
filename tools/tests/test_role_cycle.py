"""Offline continuous-loop integration tests; no GitHub, Windows or browser access."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
from contextlib import redirect_stdout
from io import StringIO

import sys
TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
import role_cycle as cycle_mod
import manual_chat_delivery as manual


class FakeRouter:
    def __init__(self):
        self.scanned = []

    def scan(self, issue=None):
        self.scanned.append(issue)
        return {"new": 0, "waiting": 0, "invalid": 0, "published": 0}


class LoopTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.folder = Path(self.temp.name) / ".dispatcher"
        self.folder.mkdir()
        self.router = FakeRouter()
        self.console = []
        self.selected = []
        self.clipboard = []
        self.answers = iter([])
        self.loop = cycle_mod.RoleCycle(
            self.router, self.folder,
            ask=lambda _: next(self.answers),
            tell=self.console.append,
            tab_switcher=self.selected.append,
            clipboard=self.clipboard.append)

    def tearDown(self):
        self.loop.close()
        self.temp.cleanup()

    def add(self, role="06", event="test-cycle-0001", text="Тестовое поручение"):
        d = self.folder / "inbox" / role
        d.mkdir(parents=True, exist_ok=True)
        file = d / f"{event}.md"
        file.write_text(text, encoding="utf-8")
        return file

    def init(self):
        cycle_mod.initialize_tabs(self.folder)

    def test_exact_seven_tabs_and_reject_ambiguous_mapping(self):
        self.assertEqual(cycle_mod.DEFAULT_TABS, {
            "00": 1, "01": 2, "02": 3, "03": 4, "04": 5, "05": 6, "06": 7})
        self.assertEqual(cycle_mod.initialize_tabs(self.folder), cycle_mod.DEFAULT_TABS)
        self.assertEqual(cycle_mod.validate_tabs(self.folder), cycle_mod.DEFAULT_TABS)
        with self.assertRaises(manual.DeliveryError):
            cycle_mod.initialize_tabs(self.folder, {**cycle_mod.DEFAULT_TABS, "06": 6})
        with self.assertRaises(manual.DeliveryError):
            cycle_mod.initialize_tabs(self.folder, {"06": 7})

    def test_missing_mapping_rejected(self):
        with self.assertRaises(manual.DeliveryError):
            cycle_mod.validate_tabs(self.folder)

    def test_one_end_to_end_confirm_and_no_second_delivery(self):
        self.init()
        self.add("06")
        self.answers = iter(["y"])
        self.assertEqual(self.loop.scan(34), 1)
        self.assertEqual(self.router.scanned, [34])
        self.assertEqual(self.selected, [7])
        self.assertEqual(self.clipboard, ["Тестовое поручение"])
        self.assertEqual(self.loop.state.get("06", "test-cycle-0001")[1], "confirmed")
        self.assertEqual(self.loop.scan(34), 0)
        self.assertEqual(self.selected, [7])

    def test_skip_keeps_pending_and_does_not_retry_same_session(self):
        self.init()
        self.add("00", "test-cycle-0002")
        self.answers = iter(["s"])
        self.assertEqual(self.loop.scan(None), 0)
        self.assertEqual(self.loop.state.get("00", "test-cycle-0002")[1], "prepared")
        self.assertEqual(self.loop.scan(None), 0)
        self.assertEqual(self.selected, [1])

    def test_quit_keeps_pending_and_signals_stop(self):
        self.init()
        self.add("01", "test-cycle-0003")
        self.answers = iter(["q"])
        self.assertEqual(self.loop.scan(None), 0)
        self.assertTrue(self.loop.stop_requested)
        self.assertEqual(self.loop.state.get("01", "test-cycle-0003")[1], "prepared")

    def test_unknown_or_empty_response_is_not_accepted(self):
        self.init()
        self.add("02", "test-cycle-0004")
        self.answers = iter(["", "OK", "y"])
        self.assertEqual(self.loop.scan(None), 1)
        self.assertEqual(self.loop.state.get("02", "test-cycle-0004")[1], "confirmed")
        self.assertTrue(any("не распознан" in line for line in self.console))

    def test_changed_message_during_confirmation_is_not_acknowledged(self):
        self.init()
        path = self.add("03", "test-cycle-0005")

        def mutate(_):
            path.write_text("Изменённое сообщение", encoding="utf-8")
            return "y"

        self.loop.ask = mutate
        with self.assertRaises(manual.DeliveryError):
            self.loop.scan(None)
        self.assertEqual(self.loop.state.get("03", "test-cycle-0005")[1], "prepared")

    def test_switch_failure_does_not_touch_clipboard_or_delivery_state(self):
        self.init()
        self.add("04", "test-cycle-0006")
        self.loop.tab_switcher = lambda _: (_ for _ in ()).throw(
            manual.DeliveryError("WinError 5"))
        with self.assertRaises(manual.DeliveryError):
            self.loop.scan(None)
        self.assertEqual(self.clipboard, [])
        self.assertIsNone(self.loop.state.get("04", "test-cycle-0006"))

    def test_preview_does_not_change_clipboard_browser_or_delivery_state(self):
        self.init()
        self.add("05", "test-cycle-0007")
        self.assertEqual(self.loop.scan(34, dry_run=True), 0)
        self.assertFalse(self.selected)
        self.assertFalse(self.clipboard)
        self.assertIsNone(self.loop.state.get("05", "test-cycle-0007"))
        self.assertTrue(any("PREVIEW" in x for x in self.console))

    def test_only_one_new_message_per_role_in_one_round(self):
        self.init()
        self.add("00", "test-cycle-0008")
        self.add("00", "test-cycle-0009")
        self.answers = iter(["y", "y"])
        self.assertEqual(self.loop.scan(None), 1)
        self.assertEqual(self.selected, [1])
        self.assertEqual(len(manual.pending(self.folder, self.loop.state)), 1)
        self.assertEqual(self.loop.scan(None), 1)
        self.assertEqual(self.selected, [1, 1])

    def test_two_roles_are_both_processed_sequentially(self):
        self.init()
        self.add("00", "test-cycle-0010")
        self.add("06", "test-cycle-0011")
        self.answers = iter(["y", "y"])
        self.assertEqual(self.loop.scan(34), 2)
        self.assertEqual(sorted(self.selected), [1, 7])

    def test_run_loop_periodic_repoll_until_quit(self):
        self.init()
        self.add("06", "test-cycle-0012")
        self.answers = iter(["y"])
        intervals = []
        with redirect_stdout(StringIO()):
            code = cycle_mod.run_loop(self.loop, issue=34, interval=60,
                                      max_rounds=3, sleeper=intervals.append)
        self.assertEqual(code, 0)
        self.assertEqual(self.router.scanned, [34, 34, 34])
        self.assertEqual(intervals, [60, 60])
        self.assertEqual(self.selected, [7])

    def test_recover_after_transient_github_error(self):
        self.init()
        calls = [0]
        def sometimes_fail(issue):
            calls[0] += 1
            if calls[0] == 1:
                raise RuntimeError("GitHub HTTP 503")
            return self.router.scan(issue)
        class Router:
            scan = staticmethod(sometimes_fail)
        self.loop.router = Router()
        intervals = []
        with redirect_stdout(StringIO()):
            code = cycle_mod.run_loop(self.loop, issue=34, interval=10,
                                      max_rounds=2, sleeper=intervals.append)
        self.assertEqual(code, 0)
        self.assertEqual(calls, [2])
        self.assertEqual(intervals, [20])


if __name__ == "__main__":
    unittest.main()