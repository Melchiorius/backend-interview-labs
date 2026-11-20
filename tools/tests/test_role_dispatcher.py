"""Offline unit tests for dispatcher; no GitHub token or network needed."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

MODULE = Path(__file__).resolve().parents[1] / "role_dispatcher.py"
spec = importlib.util.spec_from_file_location("role_dispatcher", MODULE)
d = importlib.util.module_from_spec(spec)
spec.loader.exec_module(d)

SHA = "a" * 40


def payload(**overrides):
    base = {
        "protocol": "backend-labs-handoff/v1",
        "event_id": "inheritance-g1-001",
        "from": "01",
        "to": ["02", "03"],
        "issue": 33,
        "gate": "G1",
        "action": "READY",
        "message": "Проверь исследовательский план и подготовь материал.",
        "pr": 35,
        "head_sha": SHA,
        "requires": ["pr_merged", "head_matches"],
    }
    base.update(overrides)
    return base


def comment(body, user="Melchiorius", id=55):
    return {"id": id, "body": body, "user": {"login": user},
            "html_url": f"https://github.com/example/issues/33#issuecomment-{id}"}


def fenced(obj):
    import json
    return "Результат исследования.\n\n```backend-handoff\n" + json.dumps(obj, ensure_ascii=False) + "\n```"


class MockGitHub:
    def __init__(self):
        self.repo = "Melchiorius/backend-interview-labs"
        self.issue = {"number": 33, "state": "open", "html_url": "https://github.com/example/issues/33"}
        self.item = {"head": {"sha": SHA}, "state": "closed", "merged_at": "2026-11-20T01:00:00Z"}
        self.messages = [comment(fenced(payload()))]
        self.posts = []
        self.checks = {"total_count": 1, "check_runs": [{"status": "completed", "conclusion": "success"}]}

    def issues(self, selected):
        return [self.issue]

    def comments(self, issue):
        return self.messages + [comment(body, id=100 + n) for n, body in enumerate(self.posts)]

    def pr(self, n):
        return self.item

    def check_runs(self, sha):
        return self.checks

    def publish(self, issue, body):
        self.posts.append(body)


class ProtocolTests(unittest.TestCase):
    def test_parse_handoff(self):
        obj = d.parse_handoff(fenced(payload()))
        self.assertEqual(obj["to"], ["02", "03"])
        self.assertIsNone(d.parse_handoff("Обычный отзыв без блока"))

    def test_reject_invalid_json_or_schema(self):
        for body in ["```backend-handoff\n{bad}\n```", fenced(payload(to=["01"])),
                     fenced(payload(issue=True)), fenced(payload(head_sha="bad")),
                     fenced(payload(requires=["execute_shell"]))]:
            with self.subTest(body=body):
                with self.assertRaises(d.HandoffError):
                    d.parse_handoff(body)

    def test_multiple_blocks_rejected(self):
        with self.assertRaises(d.HandoffError):
            d.parse_handoff(fenced(payload()) + "\n" + fenced(payload()))

    def test_conditions_require_exact_pr_head(self):
        gh = MockGitHub()
        gh.item["head"]["sha"] = "b" * 40
        self.assertIn("PR head_sha differs", " ".join(d.check_conditions(gh, payload(), gh.issue)))

    def test_pr_unmerged_blocks_delivery(self):
        gh = MockGitHub()
        gh.item["merged_at"] = None
        self.assertIn("PR has not been merged", d.check_conditions(gh, payload(), gh.issue))

    def test_ci_success_fail_closed(self):
        gh = MockGitHub()
        obj = payload(requires=["ci_success", "head_matches"])
        self.assertEqual(d.check_conditions(gh, obj, gh.issue), [])
        gh.checks = {"total_count": 0, "check_runs": []}
        self.assertTrue(d.check_conditions(gh, obj, gh.issue))


class DispatchTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.gh = MockGitHub()
        self.dispatcher = d.Dispatcher(self.gh, Path(self.temp.name), {"melchiorius"})

    def tearDown(self):
        self.dispatcher.close()
        self.temp.cleanup()

    def test_routes_two_targets_and_is_idempotent(self):
        self.assertEqual(self.dispatcher.scan(33)["new"], 2)
        for target in ("02", "03"):
            text = (Path(self.temp.name) / "inbox" / target / "inheritance-g1-001.md").read_text()
            self.assertIn("Проверь исследовательский план", text)
            self.assertIn("# Поручение роли", text)
            self.assertIn("Источник сообщения:", text)
            self.assertIn("## Задание", text)
            self.assertNotIn("Перед началом повторно сверьте фактический GitHub", text)
            self.assertNotIn("Это автоматическая доставка, а не доказательство", text)
        self.assertEqual(self.dispatcher.scan(33)["new"], 0)

    def test_only_allowlisted_actor(self):
        self.gh.messages = [comment(fenced(payload()), user="outsider")]
        self.assertEqual(self.dispatcher.scan()["new"], 0)

    def test_waits_until_pr_merged(self):
        self.gh.item["merged_at"] = None
        self.assertEqual(self.dispatcher.scan(33)["waiting"], 1)
        self.assertEqual(self.dispatcher.scan(33)["new"], 0)
        self.gh.item["merged_at"] = "2026-11-20T02:00:00Z"
        self.assertEqual(self.dispatcher.scan(33)["new"], 2)

    def test_reject_id_reuse_with_different_payload(self):
        self.dispatcher.scan(33)
        self.gh.messages.append(comment(fenced(payload(message="Новый текст под тем же ID")), id=56))
        self.assertEqual(self.dispatcher.scan(33)["invalid"], 1)

    def test_publishing_is_opt_in_and_not_repeated(self):
        self.dispatcher.publish = True
        self.dispatcher.scan(33)
        self.assertEqual(len(self.gh.posts), 2)
        self.dispatcher.scan(33)
        self.assertEqual(len(self.gh.posts), 2)
        self.assertIn("<!-- backend-delivery:v1:", self.gh.posts[0])
        self.assertNotIn("```backend-handoff", self.gh.posts[0])


if __name__ == "__main__":
    unittest.main()
