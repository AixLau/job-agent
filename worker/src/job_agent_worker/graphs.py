from dataclasses import dataclass
from typing import List


@dataclass(frozen=True)
class GraphSpec:
    name: str
    nodes: List[str]


def build_goal_graph() -> GraphSpec:
    return GraphSpec(
        name="GoalGraph",
        nodes=[
            "goal_parser",
            "search_strategy",
            "task_state",
        ],
    )


def build_job_match_graph() -> GraphSpec:
    return GraphSpec(
        name="JobMatchGraph",
        nodes=[
            "jd_extractor",
            "hard_filter",
            "match_scoring",
            "risk_tagging",
        ],
    )


def build_conversation_graph() -> GraphSpec:
    return GraphSpec(
        name="ConversationGraph",
        nodes=[
            "draft_generator",
            "reply_classifier",
            "next_action",
            "human_in_the_loop",
        ],
    )
