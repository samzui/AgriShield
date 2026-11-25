package com.examly.springapp.dto.response;
import java.time.Instant;
public record ApiResponse<T>(boolean success,String message,T data,Instant timestamp) {
    public static <T> ApiResponse<T> ok(String m,T d) {
        return new ApiResponse<>(true,m,d,Instant.now());
    } public static <T> ApiResponse<T> fail(String m) {
        return new ApiResponse<>(false,m,null,Instant.now());
    }
}
