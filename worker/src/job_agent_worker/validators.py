import re
from typing import Any, Dict, Iterable, List


class ValidationError(ValueError):
    pass


ALLOWED_INTENTS = {"INTERVIEW", "FOLLOW_UP", "REJECTED"}
ALLOWED_PRIORITIES = {"HIGH", "NORMAL", "LOW"}
ALLOWED_STATUSES = {"INTERVIEW", "WAITING_HR", "NEEDS_REPLY", "CLOSED"}
EMAIL_PATTERN = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")
PHONE_PATTERN = re.compile(r"\b\d{11}\b")


def validate_task_strategy(payload: Dict[str, Any]) -> None:
    raw = str(payload.get("raw") or "").strip()
    if not raw:
        raise ValidationError("raw strategy text required")
    if not isinstance(payload.get("keywords"), list):
        raise ValidationError("keywords must be a list")
    _validate_string_list(payload.get("exclude"), "exclude")
    _validate_string_list(payload.get("preferences"), "preferences")


def validate_job_match_payload(payload: Dict[str, Any]) -> None:
    score = payload.get("score")
    if not isinstance(score, int) or score < 0 or score > 100:
        raise ValidationError("score out of range")
    reasons = payload.get("reasons")
    if not isinstance(reasons, list) or not reasons or any(not str(item).strip() for item in reasons):
        raise ValidationError("reasons required")
    _validate_string_list(payload.get("risk_tags") or payload.get("risks"), "risk_tags")
    parsed_job = payload.get("parsed_job") or {}
    if not isinstance(parsed_job, dict):
        raise ValidationError("parsed_job must be dict")


def validate_reply_payload(payload: Dict[str, Any]) -> None:
    intent = str(payload.get("intent") or "").strip().upper()
    if intent not in ALLOWED_INTENTS:
        raise ValidationError("invalid intent")
    summary = str(payload.get("summary") or "").strip()
    next_action = str(payload.get("next_action") or "").strip()
    if not summary or len(summary) > 200:
        raise ValidationError("invalid summary")
    if not next_action or len(next_action) > 100:
        raise ValidationError("invalid next action")
    _validate_common_text(summary)
    _validate_common_text(next_action)


def validate_draft_payload(payload: Dict[str, Any]) -> None:
    content = str(payload.get("content") or "").strip()
    if len(content) < 10 or len(content) > 500:
        raise ValidationError("invalid draft content")
    _validate_common_text(content)
    interview_draft = payload.get("interview_draft")
    if interview_draft is not None:
        interview_text = str(interview_draft).strip()
        if len(interview_text) < 10 or len(interview_text) > 500:
            raise ValidationError("invalid interview draft")
        _validate_common_text(interview_text)


def validate_follow_up_payload(payload: Dict[str, Any]) -> None:
    priority = str(payload.get("priority") or "").strip().upper()
    suggested_status = str(payload.get("suggested_status") or "").strip().upper()
    next_action = str(payload.get("next_action") or "").strip()
    draft_content = str(payload.get("draft_content") or "").strip()
    follow_up_hours = payload.get("follow_up_hours")

    if priority not in ALLOWED_PRIORITIES:
        raise ValidationError("invalid priority")
    if suggested_status not in ALLOWED_STATUSES:
        raise ValidationError("invalid suggested status")
    if not next_action or len(next_action) > 100:
        raise ValidationError("invalid next action")
    if not draft_content or len(draft_content) > 500:
        raise ValidationError("invalid draft content")
    if not isinstance(follow_up_hours, int) or follow_up_hours < 0 or follow_up_hours > 168:
        raise ValidationError("invalid follow up hours")

    _validate_common_text(next_action)
    _validate_common_text(draft_content)


def _validate_common_text(value: str) -> None:
    if EMAIL_PATTERN.search(value) or PHONE_PATTERN.search(value) or "微信" in value.lower():
        raise ValidationError("contact info not allowed")


def _validate_string_list(values: Any, field_name: str) -> None:
    if values is None:
        return
    if not isinstance(values, list):
        raise ValidationError(f"{field_name} must be list")
    invalid = [item for item in values if not str(item).strip()]
    if invalid:
        raise ValidationError(f"{field_name} contains blank value")
