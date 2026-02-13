package com.example.userservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RequestOtpResponse {

    private String message;
}
