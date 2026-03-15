from typing import Any, Dict, List, Optional


HIGH_PRIORITY_TOKENS = ("面试", "电话沟通", "约时间", "到面", "线上面试")
MATERIAL_TOKENS = ("作品集", "简历", "补充", "材料", "附件", "发一下")
WAITING_TOKENS = ("后续", "推进", "有进展", "再同步", "流程")


def build_follow_up_plan(
    messages: List[Dict[str, Any]],
    last_message_id: Optional[str],
) -> Dict[str, Any]:
    message = _find_target_message(messages, last_message_id)
    text = str(
        message.get("text")
        or message.get("content")
        or message.get("message")
        or ""
    )

    if _contains_any(text, HIGH_PRIORITY_TOKENS):
        return {
            "priority": "HIGH",
            "suggested_status": "INTERVIEW",
            "next_action": "确认面试时间",
            "follow_up_hours": 0,
            "draft_content": "你好，我已收到面试邀约，可以配合确认面试时间。",
            "requires_review": False,
        }

    if _contains_any(text, MATERIAL_TOKENS):
        return {
            "priority": "HIGH",
            "suggested_status": "NEEDS_REPLY",
            "next_action": "补充所需材料",
            "follow_up_hours": 0,
            "draft_content": "好的，我这边补充整理相关材料，稍后发您确认。",
            "requires_review": False,
        }

    if _contains_any(text, WAITING_TOKENS):
        return {
            "priority": "NORMAL",
            "suggested_status": "WAITING_HR",
            "next_action": "24 小时后自动跟进",
            "follow_up_hours": 24,
            "draft_content": "你好，想跟进一下当前流程进展，方便时请告知最新情况。",
            "requires_review": False,
        }

    return {
        "priority": "NORMAL",
        "suggested_status": "NEEDS_REPLY",
        "next_action": "继续沟通推进",
        "follow_up_hours": 4,
        "draft_content": "你好，我已看到你的消息，这边继续配合推进。",
        "requires_review": False,
    }


def _find_target_message(
    messages: List[Dict[str, Any]], last_message_id: Optional[str]
) -> Dict[str, Any]:
    if not messages:
        return {}
    if last_message_id is None:
        return messages[-1]
    target_id = str(last_message_id)
    for message in messages:
        if str(message.get("id")) == target_id:
            return message
    return messages[-1]


def _contains_any(text: str, tokens: tuple[str, ...]) -> bool:
    return any(token in text for token in tokens)
