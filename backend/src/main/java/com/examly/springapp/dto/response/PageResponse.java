package com.examly.springapp.dto.response;
import java.util.*;
public record PageResponse<T>(List<T> content,int page,int size,long totalElements,int totalPages,boolean last) {
}
