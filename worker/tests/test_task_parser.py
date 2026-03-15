import unittest

from job_agent_worker.task_parser import parse_task_strategy


class TaskParserTest(unittest.TestCase):
    def test_parse_task_strategy_extracts_structured_fields(self):
        payload = parse_task_strategy("上海 产品经理 20k-30k 3-5年 排除外包 偏好B端 AUTO")

        self.assertEqual(payload["title"], "产品经理")
        self.assertEqual(payload["city"], "上海")
        self.assertEqual(payload["salary"], "20k-30k")
        self.assertEqual(payload["experience"], "3-5年")
        self.assertEqual(payload["automationLevel"], "AUTO")
        self.assertEqual(payload["exclude"], ["外包"])
        self.assertEqual(payload["preferences"], ["B端"])


if __name__ == "__main__":
    unittest.main()
