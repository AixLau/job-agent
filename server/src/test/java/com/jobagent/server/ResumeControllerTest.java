package com.jobagent.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.repository.AuditLogRepository;
import com.jobagent.server.repository.ResumeRepository;
import com.jobagent.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobAgentServerApplication.class)
@AutoConfigureMockMvc
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void uploadAndFetchResume() throws Exception {
        auditLogRepository.deleteAll();
        resumeRepository.deleteAll();
        userRepository.deleteAll();

        String token = registerAndLogin("alice");

        String body = """
            {
              "content": "张三\\n产品经理\\n5年经验",
              "format": "TEXT",
              "source": "upload"
            }
            """;

        mockMvc.perform(post("/api/resume")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resume.id").exists())
            .andExpect(jsonPath("$.resume.parsed_json").exists())
            .andExpect(jsonPath("$.resume.created_at").exists());

        mockMvc.perform(get("/api/resume")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resume.parsed_json").exists());
    }

    @Test
    void upload_missing_authorization_returns_unauthorized() throws Exception {
        String body = """
            {
              "content": "张三\\n产品经理\\n5年经验",
              "format": "TEXT"
            }
            """;

        mockMvc.perform(post("/api/resume")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void fetch_missing_authorization_returns_unauthorized() throws Exception {
        mockMvc.perform(get("/api/resume"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void upload_missing_content_returns_bad_request() throws Exception {
        auditLogRepository.deleteAll();
        resumeRepository.deleteAll();
        userRepository.deleteAll();

        String token = registerAndLogin("alice");

        String body = """
            {
              "format": "TEXT"
            }
            """;

        mockMvc.perform(post("/api/resume")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void upload_missing_format_returns_bad_request() throws Exception {
        auditLogRepository.deleteAll();
        resumeRepository.deleteAll();
        userRepository.deleteAll();

        String token = registerAndLogin("alice");

        String body = """
            {
              "content": "张三\\n产品经理\\n5年经验"
            }
            """;

        mockMvc.perform(post("/api/resume")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void fetch_without_resume_returns_not_found() throws Exception {
        auditLogRepository.deleteAll();
        resumeRepository.deleteAll();
        userRepository.deleteAll();

        String token = registerAndLogin("alice");

        mockMvc.perform(get("/api/resume")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    @Test
    void parse_resume_returns_preview_for_file_upload() throws Exception {
        auditLogRepository.deleteAll();
        resumeRepository.deleteAll();
        userRepository.deleteAll();

        String token = registerAndLogin("alice");

        String body = """
            {
              "content": "Alice Zhang\\nProduct Manager\\n5 years",
              "format": "PDF",
              "source": "upload",
              "file_name": "resume.pdf"
            }
            """;

        mockMvc.perform(post("/api/resume/parse")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parsed_json.file_name").value("resume.pdf"))
            .andExpect(jsonPath("$.parsed_json.format").value("PDF"))
            .andExpect(jsonPath("$.parsed_json.raw_text").value("Alice Zhang\nProduct Manager\n5 years"));
    }

    @Test
    void confirm_resume_persists_parsed_result() throws Exception {
        auditLogRepository.deleteAll();
        resumeRepository.deleteAll();
        userRepository.deleteAll();

        String token = registerAndLogin("alice");

        String body = """
            {
              "content": "Alice Zhang\\nProduct Manager\\n5 years",
              "format": "PDF",
              "source": "upload",
              "parsed_json": {
                "file_name": "resume.pdf",
                "format": "PDF",
                "raw_text": "Alice Zhang\\nProduct Manager\\n5 years"
              }
            }
            """;

        mockMvc.perform(post("/api/resume/confirm")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resume.id").exists())
            .andExpect(jsonPath("$.resume.parsed_json.file_name").value("resume.pdf"))
            .andExpect(jsonPath("$.resume.parsed_json.format").value("PDF"));
    }

    private String registerAndLogin(String account) throws Exception {
        String registerBody = mapper.writeValueAsString(Map.of(
            "account", account,
            "password", "pwd123",
            "email", account + "@example.com"
        ));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
            .andExpect(status().isOk());

        String loginBody = mapper.writeValueAsString(Map.of(
            "account", account,
            "password", "pwd123"
        ));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Map<String, Object> loginPayload = mapper.readValue(loginResponse, new TypeReference<>() {});
        return (String) loginPayload.get("access_token");
    }
}
