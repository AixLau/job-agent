package com.jobagent.server.dto;

import java.util.List;

public record ReplyListResponse(
    List<ReplyItem> items
) {}
