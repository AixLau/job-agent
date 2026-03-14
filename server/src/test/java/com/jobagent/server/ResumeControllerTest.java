package com.jobagent.server;

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
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadAndFetchResume() throws Exception {
        String body = """
            {
              "content": "张三\\n产品经理\\n5年经验",
              "parsed_json": { "name": "张三", "title": "产品经理" }
            }
            """;

        mockMvc.perform(post("/api/resume")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resume_id").exists());

        mockMvc.perform(get("/api/resume"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("张三\n产品经理\n5年经验"))
            .andExpect(jsonPath("$.parsed_json.name").value("张三"));
    }
}
