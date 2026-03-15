import unittest

from job_agent_worker.validators import (
    ValidationError,
    validate_draft_payload,
    validate_job_match_payload,
    validate_reply_payload,
    validate_task_strategy,
)


class WorkerValidatorsTest(unittest.TestCase):
    def test_validate_task_strategy_rejects_missing_raw(self):
        with self.assertRaises(ValidationError):
            validate_task_strategy({"keywords": ["产品经理"]})

    def test_validate_job_match_rejects_out_of_range_score(self):
        with self.assertRaises(ValidationError):
            validate_job_match_payload({"score": 101, "reasons": ["ok"], "risk_tags": []})

    def test_validate_reply_payload_rejects_unknown_intent(self):
        with self.assertRaises(ValidationError):
            validate_reply_payload({"intent": "UNKNOWN", "summary": "summary", "next_action": "next"})

    def test_validate_draft_payload_rejects_contact_info(self):
        with self.assertRaises(ValidationError):
            validate_draft_payload({"content": "你好，我的微信是 test_user，欢迎联系"})


if __name__ == "__main__":
    unittest.main()
