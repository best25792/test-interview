package com.example.paymentservice.mapper;

import com.example.paymentservice.client.dto.UserResponse;
import com.example.paymentservice.domain.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserResponse response) {
        if (response == null) return null;
        return User.builder()
                .id(response.id())
                .username(response.username())
                .email(response.email())
                .active(Boolean.TRUE.equals(response.isActive()))
                .verified(Boolean.TRUE.equals(response.isVerified()))
                .build();
    }
}
