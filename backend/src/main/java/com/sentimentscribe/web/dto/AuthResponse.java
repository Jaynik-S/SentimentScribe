package com.sentimentscribe.web.dto;

import java.util.List;

public record AuthResponse(String status, List<EntrySummaryResponse> entries) {
}
