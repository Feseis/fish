package com.feseis.fish.auth.model;

/**
 * 统一结果：成功带 data，失败带 error，二者互斥。
 */
public final class ApiResult<T> {

    private final T data;
    private final AuthError error;

    private ApiResult(T data, AuthError error) {
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(data, null);
    }

    public static <T> ApiResult<T> failure(AuthError error) {
        return new ApiResult<>(null, error);
    }

    public boolean isSuccess() {
        return this.error == null;
    }

    public T getData() {
        return this.data;
    }

    public AuthError getError() {
        return this.error;
    }
}
