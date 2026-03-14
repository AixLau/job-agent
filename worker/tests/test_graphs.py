import unittest

from job_agent_worker import graphs


class GraphSpecTests(unittest.TestCase):
    def test_goal_graph_spec(self) -> None:
        spec = graphs.build_goal_graph()
        self.assertEqual(spec.name, "GoalGraph")
        self.assertIn("goal_parser", spec.nodes)
        self.assertIn("search_strategy", spec.nodes)

    def test_job_match_graph_spec(self) -> None:
        spec = graphs.build_job_match_graph()
        self.assertEqual(spec.name, "JobMatchGraph")
        self.assertIn("jd_extractor", spec.nodes)
        self.assertIn("match_scoring", spec.nodes)

    def test_conversation_graph_spec(self) -> None:
        spec = graphs.build_conversation_graph()
        self.assertEqual(spec.name, "ConversationGraph")
        self.assertIn("draft_generator", spec.nodes)
        self.assertIn("reply_classifier", spec.nodes)


if __name__ == "__main__":
    unittest.main()
