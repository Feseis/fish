package com.feseis.fish.logistics.api;

import com.feseis.fish.core.api.ApiCallback;
import com.feseis.fish.logistics.model.AddressDraft;
import com.feseis.fish.logistics.model.Poi;
import com.feseis.fish.logistics.model.PriceQuote;
import com.feseis.fish.logistics.model.RegeoResult;
import com.feseis.fish.logistics.model.StaffApplyResult;
import com.feseis.fish.logistics.model.UserAddress;

import java.util.List;

/**
 * 物流服务接口：员工入职与地址簿。
 */
public interface LogisticsApi {

    void applyStaff(String voucherCode, String applyRemark,
                    ApiCallback<StaffApplyResult> callback);

    void listAddresses(ApiCallback<List<UserAddress>> callback);

    void createAddress(AddressDraft draft, ApiCallback<UserAddress> callback);

    void updateAddress(String userAddressId, AddressDraft draft,
                       ApiCallback<UserAddress> callback);

    void deleteAddress(String userAddressId, ApiCallback<Void> callback);

    void regeo(double lng, double lat, ApiCallback<RegeoResult> callback);

    void searchAddressPois(String keyword, String city, ApiCallback<List<Poi>> callback);

    /** 运费试算。declaredValue 传 0 表示不保价。 */
    void quote(String senderAdcode, String receiverAdcode, double itemWeight,
               double declaredValue, ApiCallback<PriceQuote> callback);
}
