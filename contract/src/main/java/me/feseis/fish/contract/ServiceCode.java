package me.feseis.fish.contract;

/**
 * 全库统一服务码，对应各服务间调用的 target_service / source_service。
 */
public final class ServiceCode {

    public static final int AUTH = 1;
    public static final int LOGISTICS = 2;
    public static final int PAYMENT = 3;
    public static final int SMS = 4;

    private ServiceCode() {
    }
}
