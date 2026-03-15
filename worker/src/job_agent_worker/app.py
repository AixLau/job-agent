import os
import re
from typing import Any, Dict, List

from fastapi import Depends, FastAPI, Header, HTTPException

from job_agent_worker.models import (
    DraftRequest,
    GoalParseRequest,
    JobMatchRequest,
    ReplyClassifyRequest,
)

app = FastAPI()


def require_worker_token(
    x_worker_token: str = Header(None, alias="X-Worker-Token"),
) -> str:
    expected = os.getenv("WORKER_TOKEN", "worker-secret")
    if x_worker_token is None or x_worker_token != expected:
        raise HTTPException(status_code=401, detail="unauthorized")
    return x_worker_token


def cached_response(cache: Dict[str, Dict[str, Any]], key: str, builder):
    if key and key in cache:
        return cache[key]
    response = builder()
    if key:
        cache[key] = response
    return response


goal_parse_cache: Dict[str, Dict[str, Any]] = {}
job_match_cache: Dict[str, Dict[str, Any]] = {}
draft_cache: Dict[str, Dict[str, Any]] = {}
reply_cache: Dict[str, Dict[str, Any]] = {}


@app.post("/worker/goal-parse")
def goal_parse(request: GoalParseRequest, _: str = Depends(require_worker_token)) -> Dict[str, Any]:
    def build() -> Dict[str, Any]:
        text = request.strategy_text or ""
        keywords = [token for token in re.split(r"\s+", text) if token]
        if not keywords and text:
            keywords = [text]
        return {"strategy_json": {"keywords": keywords, "raw": text}}

    return cached_response(goal_parse_cache, request.idempotency_key, build)


@app.post("/worker/job-match")
def job_match(request: JobMatchRequest, _: str = Depends(require_worker_token)) -> Dict[str, Any]:
    def build() -> Dict[str, Any]:
        job_post = request.job_post or {}
        resume = request.resume or {}
        jd_raw = job_post.get("jd_raw") or job_post.get("raw_text") or ""
        resume_text = resume.get("content") or resume.get("raw") or ""
        combined = f"{jd_raw}{resume_text}"
        if "产品" in combined:
            score = 80
            reasons = ["匹配：产品相关"]
        else:
            score = 60
            reasons = ["匹配度一般"]
        risks: List[str] = []
        return {"score": score, "reasons": reasons, "risks": risks}

    return cached_response(job_match_cache, request.idempotency_key, build)


@app.post("/worker/draft")
def draft(request: DraftRequest, _: str = Depends(require_worker_token)) -> Dict[str, Any]:
    def build() -> Dict[str, Any]:
        job_post = request.job_post or {}
        company = job_post.get("company") or "贵司"
        title = job_post.get("title") or "岗位"
        content = f"你好，我对{company}的{title}岗位很感兴趣，期待沟通。"
        return {"content": content}

    return cached_response(draft_cache, request.idempotency_key, build)


@app.post("/worker/reply-classify")
def reply_classify(
    request: ReplyClassifyRequest, _: str = Depends(require_worker_token)
) -> Dict[str, Any]:
    def build() -> Dict[str, Any]:
        message_text = extract_last_message_text(request.messages, request.last_message_id)
        if "面试" in message_text:
            intent = "INTERVIEW"
            summary = "检测到面试安排"
            next_action = "确认面试时间"
        else:
            intent = "FOLLOW_UP"
            summary = "需要跟进对话"
            next_action = "继续沟通"
        return {"intent": intent, "summary": summary, "next_action": next_action}

    return cached_response(reply_cache, request.idempotency_key, build)


def extract_last_message_text(messages: List[Dict[str, Any]], last_message_id: str) -> str:
    if not messages:
        return ""
    target = None
    for message in messages:
        if str(message.get("id")) == str(last_message_id):
            target = message
            break
    if target is None:
        target = messages[-1]
    return (
        str(target.get("text"))
        if target.get("text") is not None
        else str(target.get("content") or target.get("message") or "")
    )
