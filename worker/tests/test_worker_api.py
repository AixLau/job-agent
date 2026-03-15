import os
import unittest

from fastapi.testclient import TestClient

from job_agent_worker.app import app


class WorkerApiTest(unittest.TestCase):
    def setUp(self):
        os.environ["WORKER_TOKEN"] = "worker-secret"
        self.client = TestClient(app)
        self.headers = {"X-Worker-Token": "worker-secret"}

    def test_missing_token_returns_401(self):
        response = self.client.post("/worker/goal-parse", json={
            "task_id": "t1",
            "stage": "GOAL_PARSE",
            "strategy_text": "test",
            "idempotency_key": "k1"
        })
        self.assertEqual(response.status_code, 401)

    def test_invalid_token_returns_401(self):
        response = self.client.post(
            "/worker/goal-parse",
            headers={"X-Worker-Token": "bad"},
            json={
                "task_id": "t1",
                "stage": "GOAL_PARSE",
                "strategy_text": "test",
                "idempotency_key": "k1"
            },
        )
        self.assertEqual(response.status_code, 401)

    def test_goal_parse(self):
        response = self.client.post(
            "/worker/goal-parse",
            headers=self.headers,
            json={
                "task_id": "t1",
                "stage": "GOAL_PARSE",
                "strategy_text": "focus 产品",
                "idempotency_key": "k2"
            },
        )
        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["strategy_json"]["raw"], "focus 产品")
        self.assertIn("focus", payload["strategy_json"]["keywords"])

    def test_job_match(self):
        response = self.client.post(
            "/worker/job-match",
            headers=self.headers,
            json={
                "task_id": "t1",
                "stage": "JOB_MATCH",
                "job_post": {"raw_text": "这是产品岗位"},
                "resume": {"content": "产品经验"},
                "strategy": {},
                "idempotency_key": "k3"
            },
        )
        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["score"], 80)
        self.assertTrue(payload["reasons"])

    def test_draft(self):
        response = self.client.post(
            "/worker/draft",
            headers=self.headers,
            json={
                "task_id": "t1",
                "stage": "DRAFT",
                "conversation": {"id": "c1"},
                "job_post": {"company": "公司A", "title": "产品经理"},
                "resume": {"content": "经验"},
                "idempotency_key": "k4"
            },
        )
        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["content"], "你好，我对公司A的产品经理岗位很感兴趣，期待沟通。")

    def test_reply_classify(self):
        response = self.client.post(
            "/worker/reply-classify",
            headers=self.headers,
            json={
                "task_id": "t1",
                "stage": "REPLY_CLASSIFY",
                "conversation": {"id": "c1"},
                "messages": [{"id": "m1", "text": "安排面试时间"}],
                "last_message_id": "m1",
                "idempotency_key": "k5"
            },
        )
        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["intent"], "INTERVIEW")
        self.assertTrue(payload["summary"])
        self.assertTrue(payload["next_action"])


if __name__ == "__main__":
    unittest.main()
