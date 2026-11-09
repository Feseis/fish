package me.feseis.fish.contract;

/**
 * 全库统一业务类型词表，对应 approval / event_outbox / payment 等表的 biz_type。
 */
public final class BizType {

    public static final int WAYBILL = 1;
    public static final int WAYBILL_CANCEL = 2;
    public static final int STAFF_APPLY = 3;
    public static final int STAFF_LEAVE = 4;
    public static final int RETURN_FREIGHT = 5;

    private BizType() {
    }
}
