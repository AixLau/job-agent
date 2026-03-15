import os
import re
from typing import Any, Dict, List, Optional, Tuple

from fastapi import Depends, FastAPI, Header, HTTPException

from job_agent_worker.models import (
    DraftRequest,
    GoalParseRequest,
    JobMatchRequest,
    ResumeParseRequest,
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
resume_parse_cache: Dict[str, Dict[str, Any]] = {}


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
        risk_tags = detect_risk_tags(jd_raw)
        parsed_job = build_parsed_job(job_post)
        return {
            "score": score,
            "reasons": reasons,
            "risks": risk_tags,
            "risk_tags": risk_tags,
            "parsed_job": parsed_job,
        }

    return cached_response(job_match_cache, request.idempotency_key, build)


@app.post("/worker/draft")
def draft(request: DraftRequest, _: str = Depends(require_worker_token)) -> Dict[str, Any]:
    def build() -> Dict[str, Any]:
        job_post = request.job_post or {}
        conversation = request.conversation or {}
        company = job_post.get("company") or "贵司"
        title = job_post.get("title") or "岗位"
        content = f"你好，我对{company}的{title}岗位很感兴趣，期待沟通。"
        payload = {"content": content}
        if str(conversation.get("intent") or "").upper() == "INTERVIEW":
            payload["interview_draft"] = f"你好，我已收到{company}{title}岗位的面试邀约，可以配合确认时间。"
        return payload

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


@app.post("/worker/resume-parse")
def resume_parse(request: ResumeParseRequest, _: str = Depends(require_worker_token)) -> Dict[str, Any]:
    def build() -> Dict[str, Any]:
        parsed_json = {
            "raw_text": request.content,
            "format": request.format,
        }
        if request.file_name:
            parsed_json["file_name"] = request.file_name
        if request.source:
            parsed_json["source"] = request.source
        lines = request.content.splitlines()
        if lines:
            parsed_json["candidate_name"] = lines[0]
        if len(lines) > 1:
            parsed_json["headline"] = lines[1]
        return {"parsed_json": parsed_json}

    return cached_response(resume_parse_cache, request.idempotency_key, build)


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


def detect_risk_tags(text: str) -> List[str]:
    tags: List[str] = []
    for keyword in ("外包", "大小周", "派遣", "996"):
        if keyword in text:
            tags.append(keyword)
    return tags


def build_parsed_job(job_post: Dict[str, Any]) -> Dict[str, Any]:
    salary_text = str(job_post.get("salary") or "")
    experience_text = str(job_post.get("experience") or "")
    salary_min, salary_max = parse_salary_range(salary_text)
    exp_min, exp_max = parse_experience_range(experience_text)
    return {
        "salary_min": salary_min,
        "salary_max": salary_max,
        "exp_min": exp_min,
        "exp_max": exp_max,
    }


def parse_salary_range(text: str) -> Tuple[Optional[int], Optional[int]]:
    match = re.search(r"(\d+)\s*-\s*(\d+)\s*[kK]", text)
    if match:
        return int(match.group(1)) * 1000, int(match.group(2)) * 1000
    plus_match = re.search(r"(\d+)\s*[kK]\+", text)
    if plus_match:
        return int(plus_match.group(1)) * 1000, None
    return None, None


def parse_experience_range(text: str) -> Tuple[Optional[int], Optional[int]]:
    match = re.search(r"(\d+)\s*-\s*(\d+)\s*年", text)
    if match:
        return int(match.group(1)), int(match.group(2))
    lower_match = re.search(r"(\d+)\s*年以上", text)
    if lower_match:
        return int(lower_match.group(1)), None
    return None, None
