import unittest

from job_agent_worker.follow_up import build_follow_up_plan


class FollowUpPlanTest(unittest.TestCase):
    def test_interview_message_is_high_priority(self):
        plan = build_follow_up_plan(
            messages=[
                {"id": "m1", "role": "hr", "text": "你好，方便安排面试时间吗？"}
            ],
            last_message_id="m1",
        )

        self.assertEqual(plan["priority"], "HIGH")
        self.assertEqual(plan["suggested_status"], "INTERVIEW")
        self.assertEqual(plan["follow_up_hours"], 0)
        self.assertIn("面试", plan["draft_content"])

    def test_material_request_requires_immediate_reply(self):
        plan = build_follow_up_plan(
            messages=[
                {"id": "m2", "role": "hr", "text": "方便补充一下作品集和最新简历吗？"}
            ],
            last_message_id="m2",
        )

        self.assertEqual(plan["priority"], "HIGH")
        self.assertEqual(plan["suggested_status"], "NEEDS_REPLY")
        self.assertEqual(plan["follow_up_hours"], 0)
        self.assertIn("补充", plan["next_action"])

    def test_generic_progress_message_schedules_follow_up(self):
        plan = build_follow_up_plan(
            messages=[
                {"id": "m3", "role": "hr", "text": "收到，先帮你推进流程，有进展再同步。"}
            ],
            last_message_id="m3",
        )

        self.assertEqual(plan["priority"], "NORMAL")
        self.assertEqual(plan["suggested_status"], "WAITING_HR")
        self.assertEqual(plan["follow_up_hours"], 24)
        self.assertIn("跟进", plan["next_action"])


if __name__ == "__main__":
    unittest.main()
