package com.sentimentscribe.web.dto;

public record AuthTokenResponse(String accessToken,
                                String tokenType,
                                long expiresIn,
                                UserResponse user,
                                E2eeParamsResponse e2ee) {
}
