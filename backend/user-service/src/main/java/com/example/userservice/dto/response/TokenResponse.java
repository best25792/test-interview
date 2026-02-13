package com.example.userservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    /** Access token expiry in seconds */
    private long expiresIn;
}
