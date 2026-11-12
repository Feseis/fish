package com.feseis.fish.core.api;

/**
 * 异步回调，Mock 与真实实现都保证在主线程回调。
 */
public interface ApiCallback<T> {

    void onResult(ApiResult<T> result);
}
