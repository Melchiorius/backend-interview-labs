"""Offline tests; never touch real clipboard or a real browser."""
import importlib.util
import ctypes
from pathlib import Path
import tempfile
import unittest

MODULE = Path(__file__).resolve().parents[1] / "manual_chat_delivery.py"
spec = importlib.util.spec_from_file_location("manual_chat_delivery", MODULE)
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

URL_02 = "https://chatgpt.com/c/abc12345-1111-2222-3333-aabbccddeeff"
URL_06 = "https://chatgpt.com/g/g-p-12345/c/aabbccdd-1111-2222-3333-ffeeddccbbaa"


class ManualTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.folder = Path(self.tmp.name) / ".dispatcher"
        self.folder.mkdir()
        self.state = m.DeliveryState(self.folder)
        self.messages = []
        self.opened = []
        self.switched = []

    def tearDown(self):
        self.state.close()
        self.tmp.cleanup()

    def make_inbox(self, role="02", event="event-0001", body="Задача номер 67"):
        path = self.folder / "inbox" / role / f"{event}.md"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body, encoding="utf-8")
        return path

    def configured(self):
        m.configure(self.folder, "02", URL_02)
        m.configure(self.folder, "06", URL_06)

    def prepare(self, role=None, event=None, dry_run=False, open_chat=True, use_tab=False):
        return m.prepare(self.folder, self.state, role, event, dry_run=dry_run,
                         open_chat=open_chat, use_tab=use_tab,
                         clipboard=self.messages.append,
                         opener=lambda url: self.opened.append(url) or True,
                         tab_switcher=self.switched.append)

    def test_input_layout_matches_windows_32_and_64_bit_abi(self):
        layout64, union64, key64 = m.windows_input_layout(ctypes.c_uint64)
        layout32, union32, key32 = m.windows_input_layout(ctypes.c_uint32)
        self.assertEqual(ctypes.sizeof(layout64), 40)
        self.assertEqual(ctypes.sizeof(union64), 32)
        self.assertEqual(ctypes.sizeof(key64), 24)
        self.assertEqual(ctypes.sizeof(layout32), 28)
        self.assertEqual(ctypes.sizeof(union32), 24)
        self.assertEqual(ctypes.sizeof(key32), 16)
        self.assertEqual(layout64.u.offset, 8)
        self.assertEqual(layout32.u.offset, 4)

    def test_tab_shortcut_sequence_includes_ninth_and_eleventh(self):
        ctrl = 0x11
        pgdn = 0x22
        self.assertEqual(m.tab_steps(1), [(ctrl, ord("1"))])
        self.assertEqual(m.tab_steps(8), [(ctrl, ord("8"))])
        self.assertEqual(m.tab_steps(9), [(ctrl, ord("1"))] + [(ctrl, pgdn)] * 8)
        self.assertEqual(len(m.tab_steps(11)), 11)

    def test_invalid_tab_number_or_mapping_rejected(self):
        for value in (0, -1, 65, True, "2"):
            with self.subTest(value=value):
                with self.assertRaises(m.DeliveryError):
                    m.validate_tab_index(value)
        (self.folder / "chat_tabs.json").write_text('{"02": true}', encoding="utf-8")
        with self.assertRaises(m.DeliveryError):
            m.read_tab_mapping(self.folder)

    def test_tab_mapping_persists_independent_of_urls(self):
        m.configure_tab(self.folder, "06", 11)
        self.assertEqual(m.read_tab_mapping(self.folder), {"06": 11})
        self.assertEqual(m.read_mapping(self.folder), {})

    def test_tab_delivery_switches_without_opening_a_url(self):
        m.configure_tab(self.folder, "06", 11)
        self.make_inbox("06", "tab-demo-0001", "Сообщение для 06")
        self.assertEqual(self.prepare("06", use_tab=True), ("06", "tab-demo-0001"))
        self.assertEqual(self.switched, [11])
        self.assertEqual(self.opened, [])
        self.assertEqual(self.messages, ["Сообщение для 06"])
        self.assertEqual(self.state.get("06", "tab-demo-0001")[1], "prepared")

    def test_tab_dry_run_neither_switches_nor_copies(self):
        m.configure_tab(self.folder, "06", 7)
        self.make_inbox("06", "tab-demo-0002")
        self.assertEqual(self.prepare("06", use_tab=True, dry_run=True), ("06", "tab-demo-0002"))
        self.assertEqual(self.switched, [])
        self.assertEqual(self.messages, [])
        self.assertIsNone(self.state.get("06", "tab-demo-0002"))

    def test_tab_switch_error_keeps_message_pending(self):
        m.configure_tab(self.folder, "06", 9)
        self.make_inbox("06", "tab-demo-0003")
        with self.assertRaises(m.DeliveryError):
            m.prepare(self.folder, self.state, "06", None, use_tab=True,
                      clipboard=self.messages.append,
                      tab_switcher=lambda _: (_ for _ in ()).throw(m.DeliveryError("not foreground")))
        self.assertFalse(self.messages)
        self.assertIsNone(self.state.get("06", "tab-demo-0003"))
        self.assertEqual(len(m.pending(self.folder, self.state)), 1)

    def test_chat_url_validation(self):
        self.assertEqual(m.validate_chat_url(URL_06), URL_06)
        for bad in ["http://chatgpt.com/c/abc", "https://evil.com/c/abc",
                    "https://chatgpt.com.evil.com/c/abc", "https://chatgpt.com/",
                    "https://chatgpt.com/c/abc?token=key", "javascript:alert(1)",
                    "https://user:pass@chatgpt.com/c/abc"]:
            with self.subTest(url=bad):
                with self.assertRaises(m.DeliveryError):
                    m.validate_chat_url(bad)

    def test_mapping_is_persisted(self):
        self.configured()
        self.assertEqual(m.read_mapping(self.folder)["02"], URL_02)
        self.assertEqual(len(m.read_mapping(self.folder)), 2)

    def test_bad_mapping_rejected(self):
        (self.folder / "chat_urls.json").write_text('{"99":"https://chatgpt.com/c/a"}')
        with self.assertRaises(m.DeliveryError):
            m.read_mapping(self.folder)

    def test_empty_queue(self):
        self.assertEqual(m.pending(self.folder, self.state), [])
        self.assertIsNone(self.prepare())

    def test_prepare_opens_right_role_and_copies_text(self):
        self.configured()
        self.make_inbox("02", "event-0002", "Содержимое поручения")
        self.assertEqual(self.prepare("02"), ("02", "event-0002"))
        self.assertEqual(self.messages, ["Содержимое поручения"])
        self.assertEqual(self.opened, [URL_02])
        self.assertEqual(self.state.get("02", "event-0002")[1], "prepared")

    def test_no_open_copies_but_does_not_open_tab(self):
        self.configured()
        self.make_inbox("02", "event-0002", "Поручение без новой вкладки")
        self.assertEqual(self.prepare("02", open_chat=False), ("02", "event-0002"))
        self.assertEqual(self.messages, ["Поручение без новой вкладки"])
        self.assertEqual(self.opened, [])
        self.assertEqual(self.state.get("02", "event-0002")[1], "prepared")
        self.assertEqual(len(m.pending(self.folder, self.state)), 1)

    def test_no_open_does_not_ack_when_reprepared(self):
        self.configured()
        self.make_inbox()
        self.prepare("02", open_chat=False)
        self.prepare("02", open_chat=False)
        self.assertEqual(self.opened, [])
        self.assertEqual(len(self.messages), 2)
        self.assertEqual(self.state.get("02", "event-0001")[1], "prepared")

    def test_dry_run_does_not_copy_open_or_mutate(self):
        self.configured()
        self.make_inbox()
        self.prepare("02", dry_run=True)
        self.assertFalse(self.messages)
        self.assertFalse(self.opened)
        self.assertIsNone(self.state.get("02", "event-0001"))

    def test_requires_mapping_before_sending(self):
        self.make_inbox()
        with self.assertRaises(m.DeliveryError):
            self.prepare("02")
        self.assertEqual(self.messages, [])

    def test_prepare_not_ack_until_confirmed(self):
        self.configured()
        self.make_inbox()
        self.prepare("02")
        self.assertEqual(len(m.pending(self.folder, self.state)), 1)
        message, digest = m.read_message(self.folder / "inbox" / "02" / "event-0001.md")
        self.state.confirm("02", "event-0001", digest)
        self.assertEqual(m.pending(self.folder, self.state), [])
        self.assertEqual(self.state.get("02", "event-0001")[1], "confirmed")

    def test_confirm_before_prepare_refused(self):
        self.make_inbox()
        _, digest = m.read_message(self.folder / "inbox" / "02" / "event-0001.md")
        with self.assertRaises(m.DeliveryError):
            self.state.confirm("02", "event-0001", digest)

    def test_changed_payload_after_prepare_rejected(self):
        self.configured()
        p = self.make_inbox()
        self.prepare("02")
        p.write_text("Изменённое поручение")
        with self.assertRaises(m.DeliveryError):
            m.pending(self.folder, self.state)
        with self.assertRaises(m.DeliveryError):
            self.state.confirm("02", "event-0001", m.read_message(p)[1])

    def test_reprepare_after_restart_is_not_confirmation(self):
        self.configured()
        self.make_inbox()
        self.prepare("02")
        self.state.close()
        self.state = m.DeliveryState(self.folder)
        self.assertEqual(self.state.get("02", "event-0001")[1], "prepared")
        self.assertEqual(self.prepare("02"), ("02", "event-0001"))

    def test_filter_role_and_event(self):
        self.configured()
        self.make_inbox("02", "event-0002")
        self.make_inbox("06", "event-0006")
        self.assertEqual(len(m.pending(self.folder, self.state, "06")), 1)
        self.assertEqual(self.prepare("06", "event-0006"), ("06", "event-0006"))
        self.assertEqual(self.opened, [URL_06])

    def test_reject_ambiguous_event_without_role(self):
        self.configured()
        self.make_inbox("02", "same-event-0001")
        self.make_inbox("06", "same-event-0001")
        with self.assertRaises(m.DeliveryError):
            self.prepare(event="same-event-0001")

    def test_invalid_filename_ignored(self):
        self.make_inbox(event="x")
        self.assertEqual(m.list_inbox(self.folder), [])

    def test_large_message_rejected(self):
        self.make_inbox(body="x" * (m.MAX_MESSAGE + 1))
        with self.assertRaises(m.DeliveryError):
            m.pending(self.folder, self.state)


if __name__ == "__main__":
    unittest.main()