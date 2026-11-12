package com.feseis.fish.logistics.model;

/**
 * 运费试算结果。金额单位元，展示时统一保留 2 位小数。
 */
public final class PriceQuote {

    private final double freightAmount;
    private final double insuredFee;
    private final double totalAmount;

    public PriceQuote(double freightAmount, double insuredFee, double totalAmount) {
        this.freightAmount = freightAmount;
        this.insuredFee = insuredFee;
        this.totalAmount = totalAmount;
    }

    public double getFreightAmount() {
        return this.freightAmount;
    }

    public double getInsuredFee() {
        return this.insuredFee;
    }

    public double getTotalAmount() {
        return this.totalAmount;
    }
}
