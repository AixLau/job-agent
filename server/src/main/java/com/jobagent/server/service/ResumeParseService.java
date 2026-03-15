package com.jobagent.server.service;

import com.jobagent.server.dto.ResumeRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ResumeParseService {

    public Map<String, Object> parse(ResumeRequest request) {
        Map<String, Object> parsed = new LinkedHashMap<>();
        parsed.put("raw_text", request.content());
        parsed.put("format", request.format());
        if (request.source() != null) {
            parsed.put("source", request.source());
        }
        if (request.fileName() != null && !request.fileName().isBlank()) {
            parsed.put("file_name", request.fileName());
        }
        String[] lines = request.content().split("\\R");
        if (lines.length > 0 && !lines[0].isBlank()) {
            parsed.put("candidate_name", lines[0].trim());
        }
        if (lines.length > 1 && !lines[1].isBlank()) {
            parsed.put("headline", lines[1].trim());
        }
        return parsed;
    }
}
