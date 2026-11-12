package me.feseis.fish.logistics.vo.waybill;

/**
 * 取消、异常、解除统一返回：只回运单标识与操作后的状态。
 */
public record WaybillOperationRespVO(
        String waybillId,
        String waybillNo,
        Integer status
) {
}
