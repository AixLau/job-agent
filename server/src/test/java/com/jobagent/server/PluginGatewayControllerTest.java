package com.jobagent.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobAgentServerApplication.class)
@AutoConfigureMockMvc
class PluginGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pageReport_returnsOk() throws Exception {
        String body = """
            {
              "task_id": "t1",
              "page_type": "list",
              "raw_text": "资深 产品 需要经验",
              "extracted_json": { "k": "v" },
              "source_url": "https://example.com",
              "dom_hash": "abc"
            }
            """;

        mockMvc.perform(post("/plugin/page/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.analysis.score").value(85))
            .andExpect(jsonPath("$.analysis.reasons[0]").value("岗位匹配：产品相关"))
            .andExpect(jsonPath("$.analysis.reasons[1]").value("匹配资深要求"));
    }

    @Test
    void chatReport_returnsOk() throws Exception {
        String body = """
            {
              "task_id": "t1",
              "conversation_id": "c1",
              "messages": [
                { "id": "m1", "role": "hr", "text": "可以安排面试吗" }
              ],
              "last_message_id": "m1"
            }
            """;

        mockMvc.perform(post("/plugin/chat/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.reply.intent").value("INTERVIEW"))
            .andExpect(jsonPath("$.reply.next_action").value("确认面试时间"));
    }

    @Test
    void actionReport_returnsOk() throws Exception {
        String body = """
            {
              "task_id": "t1",
              "action_type": "send_message",
              "status": "success",
              "payload": { "draft_id": "d1" }
            }
            """;

        mockMvc.perform(post("/plugin/action/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void heartbeat_returnsOk() throws Exception {
        String body = """
            {
              "user_id": "u1",
              "task_id": "t1",
              "tab_id": "tab1",
              "status": "active",
              "ts": 1710000000
            }
            """;

        mockMvc.perform(post("/plugin/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }
}
