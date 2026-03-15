from typing import Any, Dict, List, Optional

from pydantic import BaseModel


class GoalParseRequest(BaseModel):
    task_id: str
    stage: str
    strategy_text: str
    idempotency_key: str


class JobMatchRequest(BaseModel):
    task_id: str
    stage: str
    job_post: Dict[str, Any]
    resume: Dict[str, Any]
    strategy: Dict[str, Any]
    idempotency_key: str


class DraftRequest(BaseModel):
    task_id: str
    stage: str
    conversation: Dict[str, Any]
    job_post: Dict[str, Any]
    resume: Dict[str, Any]
    idempotency_key: str


class ReplyClassifyRequest(BaseModel):
    task_id: str
    stage: str
    conversation: Dict[str, Any]
    messages: List[Dict[str, Any]]
    last_message_id: str
    idempotency_key: str


class ResumeParseRequest(BaseModel):
    content: str
    format: str
    file_name: Optional[str] = None
    source: Optional[str] = None
    idempotency_key: str
