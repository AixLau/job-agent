package com.jobagent.server.store;

import com.jobagent.server.dto.ResumeRequest;
import com.jobagent.server.dto.ResumeResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ResumeStore {

    private final AtomicReference<ResumeResponse> current = new AtomicReference<>();

    public ResumeResponse save(ResumeRequest request) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> parsed = request.parsedJson() == null
            ? Map.of()
            : request.parsedJson();
        ResumeResponse response = new ResumeResponse(id, request.content(), parsed);
        current.set(response);
        return response;
    }

    public ResumeResponse latest() {
        ResumeResponse response = current.get();
        if (response == null) {
            return new ResumeResponse("", "", Map.of());
        }
        return response;
    }
}
