package com.jobagent.server.dto;

import java.util.Map;

public record ResumeParseResponse(
    Map<String, Object> parsedJson
) {}
