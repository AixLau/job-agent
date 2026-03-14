package com.jobagent.server;

import com.jobagent.server.store.DashboardStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobAgentServerApplication.class)
@AutoConfigureMockMvc
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DashboardStore store;

    @BeforeEach
    void clearStore() {
        store.clear();
    }

    @Test
    void dashboard_returnsAggregatedSnapshot() throws Exception {
        String pageBody = """
            {
              "task_id": "t1",
              "page_type": "detail",
              "raw_text": "资深 产品 需要经验",
              "extracted_json": { "title": "资深产品经理", "company": "智聘科技" },
              "source_url": "https://example.com/job/1",
              "dom_hash": "abc"
            }
            """;

        mockMvc.perform(post("/plugin/page/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pageBody))
            .andExpect(status().isOk());

        String chatBody = """
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
                .content(chatBody))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metrics.recommendations").value(1))
            .andExpect(jsonPath("$.metrics.drafts").value(1))
            .andExpect(jsonPath("$.metrics.replies").value(1))
            .andExpect(jsonPath("$.metrics.interviews").value(1))
            .andExpect(jsonPath("$.recommendations[0].title").value("资深产品经理"))
            .andExpect(jsonPath("$.drafts[0].company").value("智聘科技"))
            .andExpect(jsonPath("$.replies[0].intent").value("INTERVIEW"));
    }
}
