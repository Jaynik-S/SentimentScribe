package com.moodverse.web.dto;

import java.util.List;

public record AuthResponse(String status, List<EntrySummaryResponse> entries) {
}
