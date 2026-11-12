package me.feseis.fish.logistics.mapper.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * waybill 读写行。插入与查询复用同一结构，字段与列一一对应。
 */
@Data
public class WaybillRow {
    private String waybillId;
    private String waybillNo;
    private String authAccountId;

    private String senderName;
    private String senderPhone;
    private String senderProvince;
    private String senderCity;
    private String senderDistrict;
    private String senderDetailAddr;
    private String senderAdcode;
    private String senderPoiId;
    private String senderPoiName;
    private BigDecimal senderLng;
    private BigDecimal senderLat;

    private String receiverName;
    private String receiverPhone;
    private String receiverProvince;
    private String receiverCity;
    private String receiverDistrict;
    private String receiverDetailAddr;
    private String receiverAdcode;
    private String receiverPoiId;
    private String receiverPoiName;
    private BigDecimal receiverLng;
    private BigDecimal receiverLat;

    private String itemName;
    private Integer itemCategoryId;
    private BigDecimal itemWeight;
    private BigDecimal declaredValue;

    private BigDecimal freightAmount;
    private BigDecimal insuredFee;
    private BigDecimal totalAmount;
    private Long priceRuleId;
    private Long insuranceRuleId;
    private Integer payMethod;

    private Integer status;
    private String currentSiteId;
    private LocalDateTime pickedAt;
    private LocalDateTime deliveredAt;
    private Integer signType;
    private String signName;
    private String signPhone;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
