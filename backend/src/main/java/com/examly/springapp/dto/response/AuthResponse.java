package com.examly.springapp.dto.response;
public record AuthResponse(String accessToken,String refreshToken,String tokenType,long expiresIn,Long userId,String userIdValue,String email,String role) {
}
