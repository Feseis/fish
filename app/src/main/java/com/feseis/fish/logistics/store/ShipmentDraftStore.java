package com.feseis.fish.logistics.store;

import com.feseis.fish.logistics.model.UserAddress;

/**
 * 寄收地址草稿。内存单例，切 tab 重建 Fragment 后仍保留本次选择。
 */
public final class ShipmentDraftStore {

    private static final ShipmentDraftStore INSTANCE = new ShipmentDraftStore();

    private UserAddress sender;
    private UserAddress receiver;

    private ShipmentDraftStore() {
    }

    public static ShipmentDraftStore get() {
        return INSTANCE;
    }

    public synchronized UserAddress getSender() {
        return this.sender;
    }

    public synchronized void setSender(UserAddress sender) {
        this.sender = sender;
    }

    public synchronized UserAddress getReceiver() {
        return this.receiver;
    }

    public synchronized void setReceiver(UserAddress receiver) {
        this.receiver = receiver;
    }

    /** 寄收件对调。 */
    public synchronized void swap() {
        UserAddress temp = this.sender;
        this.sender = this.receiver;
        this.receiver = temp;
    }

    public synchronized boolean isComplete() {
        return this.sender != null && this.receiver != null;
    }
}
