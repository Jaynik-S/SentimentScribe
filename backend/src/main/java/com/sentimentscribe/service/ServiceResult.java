package com.sentimentscribe.service;

public record ServiceResult<T>(boolean success, T data, String errorMessage) {

    public static <T> ServiceResult<T> success(T data) {
        return new ServiceResult<>(true, data, null);
    }

    public static <T> ServiceResult<T> failure(String errorMessage) {
        return new ServiceResult<>(false, null, errorMessage);
    }
}
