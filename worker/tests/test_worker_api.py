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

    def test_goal_parse_returns_structured_task_fields(self):
        response = self.client.post(
            "/worker/goal-parse",
            headers=self.headers,
            json={
                "task_id": "t-structured",
                "stage": "GOAL_PARSE",
                "strategy_text": "上海 产品经理 20k-30k 3-5年 排除外包 偏好B端 AUTO",
                "idempotency_key": "goal-structured"
            },
        )
        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["strategy_json"]["title"], "产品经理")
        self.assertEqual(payload["strategy_json"]["city"], "上海")
        self.assertEqual(payload["strategy_json"]["salary"], "20k-30k")
        self.assertEqual(payload["strategy_json"]["experience"], "3-5年")
        self.assertEqual(payload["strategy_json"]["automationLevel"], "AUTO")
        self.assertIn("外包", payload["strategy_json"]["exclude"])
        self.assertIn("B端", payload["strategy_json"]["preferences"])

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

    def test_job_match_returns_parsed_job_and_risk_tags(self):
        response = self.client.post(
            "/worker/job-match",
            headers=self.headers,
            json={
                "task_id": "t1",
                "stage": "JOB_MATCH",
                "job_post": {
                    "jd_raw": "外包 大小周 产品岗位",
                    "salary": "20-30k",
                    "experience": "3-5年"
                },
                "resume": {"content": "产品经验"},
                "strategy": {},
                "idempotency_key": "k3b"
            },
        )
        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(
            set(payload["parsed_job"].keys()),
            {"salary_min", "salary_max", "exp_min", "exp_max"},
        )
        self.assertEqual(payload["parsed_job"]["salary_min"], 20000)
        self.assertEqual(payload["parsed_job"]["salary_max"], 30000)
        self.assertEqual(payload["parsed_job"]["exp_min"], 3)
        self.assertEqual(payload["parsed_job"]["exp_max"], 5)
        self.assertIn("外包", payload["risk_tags"])
        self.assertIn("大小周", payload["risk_tags"])

    def test_worker_requires_token_for_job_match(self):
        response = self.client.post(
            "/worker/job-match",
            json={
                "task_id": "t1",
                "stage": "JOB_MATCH",
                "job_post": {"raw_text": "这是产品岗位"},
                "resume": {"content": "产品经验"},
                "strategy": {},
                "idempotency_key": "k3c"
            },
        )
        self.assertEqual(response.status_code, 401)

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

    def test_draft_returns_interview_draft_when_intent_interview(self):
        response = self.client.post(
            "/worker/draft",
            headers=self.headers,
            json={
                "task_id": "t1",
                "stage": "DRAFT",
                "conversation": {"id": "c1", "intent": "INTERVIEW"},
                "job_post": {"company": "公司A", "title": "产品经理"},
                "resume": {"content": "经验"},
                "idempotency_key": "k4b"
            },
        )
        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertIn("interview_draft", payload)
        self.assertIn("公司A", payload["interview_draft"])

    def test_worker_requires_token_for_draft(self):
        response = self.client.post(
            "/worker/draft",
            json={
                "task_id": "t1",
                "stage": "DRAFT",
                "conversation": {"id": "c1"},
                "job_post": {"company": "公司A", "title": "产品经理"},
                "resume": {"content": "经验"},
                "idempotency_key": "k4c"
            },
        )
        self.assertEqual(response.status_code, 401)

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

    def test_follow_up_returns_interview_plan(self):
        response = self.client.post(
            "/worker/follow-up",
            headers=self.headers,
            json={
                "task_id": "t1",
                "stage": "FOLLOW_UP",
                "conversation": {"id": "c1"},
                "messages": [{"id": "m1", "role": "hr", "text": "方便安排面试吗"}],
                "last_message_id": "m1",
                "idempotency_key": "k-follow-up-1"
            },
        )
        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["priority"], "HIGH")
        self.assertEqual(payload["suggested_status"], "INTERVIEW")
        self.assertEqual(payload["follow_up_hours"], 0)
        self.assertIn("面试", payload["draft_content"])

    def test_follow_up_returns_waiting_hr_plan(self):
        response = self.client.post(
            "/worker/follow-up",
            headers=self.headers,
            json={
                "task_id": "t1",
                "stage": "FOLLOW_UP",
                "conversation": {"id": "c1"},
                "messages": [{"id": "m2", "role": "hr", "text": "收到，后续有进展再同步你。"}],
                "last_message_id": "m2",
                "idempotency_key": "k-follow-up-2"
            },
        )
        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["priority"], "NORMAL")
        self.assertEqual(payload["suggested_status"], "WAITING_HR")
        self.assertEqual(payload["follow_up_hours"], 24)

    def test_resume_parse_returns_preview_fields(self):
        response = self.client.post(
            "/worker/resume-parse",
            headers=self.headers,
            json={
                "content": "Alice Zhang\nProduct Manager\n5 years",
                "format": "PDF",
                "file_name": "resume.pdf",
                "source": "upload",
                "idempotency_key": "resume-1"
            },
        )
        self.assertEqual(response.status_code, 200)
        payload = response.json()
        self.assertEqual(payload["parsed_json"]["file_name"], "resume.pdf")
        self.assertEqual(payload["parsed_json"]["format"], "PDF")
        self.assertEqual(payload["parsed_json"]["raw_text"], "Alice Zhang\nProduct Manager\n5 years")


if __name__ == "__main__":
    unittest.main()
