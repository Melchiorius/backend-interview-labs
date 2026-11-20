"""Guard the mandatory GitHub handoff policy against accidental removal.

This is a static documentation consistency test, not proof that ChatGPT
actually published a handoff, nor proof of a G0–G6 gate.
"""
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
ROLE_FILES = [
    "00-project-coordinator.md",
    "01-research-coverage.md",
    "02-knowledge-author.md",
    "03-examples-exercises.md",
    "04-independent-reviewer.md",
    "05-knowledge-editor.md",
    "06-research-operations.md",
]


def document(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class MandatoryHandoffPolicyTests(unittest.TestCase):
    def test_agents_declares_mandatory_rule_and_single_exception(self):
        rules = document("AGENTS.md")
        self.assertIn("## ОБЯЗАТЕЛЬНОЕ ПРАВИЛО ЗАВЕРШЕНИЯ РАБОТЫ", rules)
        for token in (
            "backend-handoff", "независимо от результата", "общая остановка",
            'action="QUESTION"', 'action="BLOCKED"', 'action="READY"',
            "не заменяют", "GitHub",
        ):
            with self.subTest(token=token):
                self.assertIn(token, rules)

    def test_all_seven_roles_require_handoff_after_any_outcome(self):
        self.assertEqual(len(ROLE_FILES), 7)
        for filename in ROLE_FILES:
            with self.subTest(role=filename):
                text = document(f"docs/roles/{filename}")
                for token in (
                    "**ОБЯЗАТЕЛЬНОЕ ПРАВИЛО ЗАВЕРШЕНИЯ РАБОТЫ:**",
                    "независимо от исхода", "backend-handoff",
                    "CHANGES", "BLOCKED", "QUESTION", "READY",
                    "общая остановка", "event_id", "AGENTS.md",
                ):
                    self.assertIn(token, text)

    def test_workflow_and_protocol_cannot_make_delivery_optional(self):
        workflow = document("docs/WORKFLOW.md")
        protocol = document("docs/ROLE_HANDOFF_PROTOCOL.md")
        self.assertIn("## Обязательная передача результата между ролями", workflow)
        self.assertIn("ОБЯЗАТЕЛЬНО при любом результате", protocol)
        self.assertIn("Инвариант: после любого результата следует передача", protocol)
        self.assertIn("Единственное штатное исключение", protocol)
        self.assertIn("не допустимые значения", protocol)
        self.assertIn("issue", protocol)
        self.assertNotIn("## Необязательная маршрутизация поручений", workflow)


if __name__ == "__main__":
    unittest.main()
