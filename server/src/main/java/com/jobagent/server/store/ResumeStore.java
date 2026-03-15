package com.jobagent.server.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.server.dto.ResumeRequest;
import com.jobagent.server.dto.ResumeResponse;
import com.jobagent.server.repository.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Component
public class ResumeStore {

    private static final Map<String, Object> EMPTY_PARSED = Map.of();

    private final ResumeRepository repository;
    private final ObjectMapper objectMapper;

    public ResumeStore(ResumeRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public ResumeResponse save(ResumeRequest request, String userId) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> parsed = Map.of("content", request.content());
        String parsedJson = writeParsedJson(parsed);
        ResumeEntity entity = new ResumeEntity(id, userId, request.content(), parsedJson);
        repository.save(entity);
        return new ResumeResponse(new ResumeResponse.ResumePayload(id, parsed, entity.getCreatedAt()));
    }

    public ResumeResponse latest(String userId) {
        return repository.findFirstByUserIdOrderByCreatedAtDesc(userId)
            .map(this::toResponse)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "resume not found"));
    }

    private ResumeResponse toResponse(ResumeEntity entity) {
        return new ResumeResponse(new ResumeResponse.ResumePayload(
            entity.getId(),
            readParsedJson(entity.getParsedJson()),
            entity.getCreatedAt()
        ));
    }

    private String writeParsedJson(Map<String, Object> parsed) {
        try {
            return objectMapper.writeValueAsString(parsed);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> readParsedJson(String parsedJson) {
        if (parsedJson == null || parsedJson.isBlank()) {
            return EMPTY_PARSED;
        }
        try {
            return objectMapper.readValue(parsedJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return EMPTY_PARSED;
        }
    }
}
