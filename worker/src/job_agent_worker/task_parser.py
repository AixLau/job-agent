import re
from typing import Dict, List, Optional


KNOWN_CITIES = (
    "北京",
    "上海",
    "深圳",
    "广州",
    "杭州",
    "苏州",
    "成都",
    "武汉",
    "南京",
    "西安",
)

AUTOMATION_ALIASES = {
    "AUTO": "AUTO",
    "自动": "AUTO",
    "全自动": "AUTO",
    "SEMI": "SEMI",
    "半自动": "SEMI",
    "保守": "MANUAL",
    "MANUAL": "MANUAL",
}

SALARY_PATTERN = re.compile(r"(\d+\s*[kK](?:\s*-\s*\d+\s*[kK])?|\d+\s*[kK]\+)")
EXPERIENCE_PATTERN = re.compile(r"(\d+\s*-\s*\d+\s*年|\d+\s*年以上)")


def parse_task_strategy(text: str) -> Dict[str, object]:
    strategy_text = (text or "").strip()
    keywords = [token for token in re.split(r"\s+", strategy_text) if token]
    payload: Dict[str, object] = {
        "raw": strategy_text,
        "keywords": keywords if keywords else ([strategy_text] if strategy_text else []),
    }

    city = extract_city(strategy_text)
    if city:
        payload["city"] = city

    salary = extract_match(SALARY_PATTERN, strategy_text)
    if salary:
        payload["salary"] = salary

    experience = extract_match(EXPERIENCE_PATTERN, strategy_text)
    if experience:
        payload["experience"] = experience

    automation_level = extract_automation_level(strategy_text)
    if automation_level:
        payload["automationLevel"] = automation_level

    exclude = extract_tag_values(strategy_text, "排除", ("偏好", "AUTO", "SEMI", "MANUAL", "自动", "半自动", "保守"))
    if exclude:
        payload["exclude"] = exclude

    preferences = extract_tag_values(strategy_text, "偏好", ("排除", "AUTO", "SEMI", "MANUAL", "自动", "半自动", "保守"))
    if preferences:
        payload["preferences"] = preferences

    title = extract_title(strategy_text, city, salary, experience, automation_level, exclude, preferences)
    if title:
        payload["title"] = title

    return payload


def extract_city(text: str) -> Optional[str]:
    for city in KNOWN_CITIES:
        if city in text:
            return city
    match = re.search(r"([^\s]{2,8}市)", text)
    if match:
        return match.group(1)
    return None


def extract_match(pattern: re.Pattern[str], text: str) -> Optional[str]:
    match = pattern.search(text)
    if not match:
        return None
    return compact_spaces(match.group(1))


def extract_automation_level(text: str) -> Optional[str]:
    for alias, normalized in AUTOMATION_ALIASES.items():
        if alias in text:
            return normalized
    return None


def extract_tag_values(text: str, label: str, stop_tokens: tuple[str, ...]) -> List[str]:
    marker = text.find(label)
    if marker < 0:
        return []
    segment = text[marker + len(label):]
    stop_index = len(segment)
    for token in stop_tokens:
        candidate = segment.find(token)
        if candidate >= 0:
            stop_index = min(stop_index, candidate)
    raw_values = segment[:stop_index].strip(" ：:，,")
    if not raw_values:
        return []
    parts = [
        compact_spaces(part)
        for part in re.split(r"[、,，/]|和", raw_values)
        if compact_spaces(part)
    ]
    return parts


def extract_title(text: str,
                  city: Optional[str],
                  salary: Optional[str],
                  experience: Optional[str],
                  automation_level: Optional[str],
                  exclude: List[str],
                  preferences: List[str]) -> Optional[str]:
    candidate = text
    for token in filter(None, [city, salary, experience, automation_level]):
        candidate = candidate.replace(token, " ")
    for label, values in (("排除", exclude), ("偏好", preferences)):
        if label in candidate:
            start = candidate.find(label)
            end = len(candidate)
            for stopper in ("排除", "偏好", "AUTO", "SEMI", "MANUAL", "自动", "半自动", "保守"):
                next_index = candidate.find(stopper, start + len(label))
                if next_index >= 0:
                    end = min(end, next_index)
            candidate = candidate[:start] + " " + candidate[end:]
        for value in values:
            candidate = candidate.replace(value, " ")
    title = compact_spaces(candidate)
    return title or None


def compact_spaces(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()
