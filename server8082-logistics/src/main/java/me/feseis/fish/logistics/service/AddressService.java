package me.feseis.fish.logistics.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.client.AmapClient;
import me.feseis.fish.logistics.dto.address.RegeoResponse;
import me.feseis.fish.logistics.dto.address.UserAddressResponse;
import me.feseis.fish.logistics.dto.address.UserAddressSaveRequest;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import me.feseis.fish.logistics.mapper.GeoAddressMapper;
import me.feseis.fish.logistics.mapper.UserAddressMapper;
import me.feseis.fish.logistics.mapper.model.GeoAddressRow;
import me.feseis.fish.logistics.mapper.model.UserAddressRow;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 地址业务：逆地理编码 + 地址簿。geo_address 是全局缓存，user_address 是用户条目。
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private static final BigDecimal LNG_MAX = new BigDecimal("136.0");
    private static final BigDecimal LNG_MIN = new BigDecimal("73.0");
    private static final BigDecimal LAT_MAX = new BigDecimal("54.0");
    private static final BigDecimal LAT_MIN = new BigDecimal("3.0");

    private final AmapClient amapClient;
    private final GeoAddressMapper geoAddressMapper;
    private final UserAddressMapper userAddressMapper;

    public RegeoResponse regeo(BigDecimal lng, BigDecimal lat) {
        if (lng == null || lat == null
                || lng.compareTo(LNG_MIN) < 0 || lng.compareTo(LNG_MAX) > 0
                || lat.compareTo(LAT_MIN) < 0 || lat.compareTo(LAT_MAX) > 0) {
            throw BizException.of(ErrorCode.INVALID_PARAM, "坐标超出中国范围");
        }
        return amapClient.regeo(lng, lat);
    }

    public List<UserAddressResponse> list(String accountId) {
        return userAddressMapper.listByAccount(accountId).stream().map(AddressService::toResponse).toList();
    }

    @Transactional
    public UserAddressResponse create(String accountId, UserAddressSaveRequest req) {
        String geoAddressId = upsertGeoAddress(req);

        var existing = userAddressMapper.findNatural(accountId, geoAddressId,
                req.detailAddr(), req.contactName(), req.contactPhone());
        if (existing.isPresent()) {
            UserAddressRow row = existing.get();
            if (row.getDeletedAt() == null) {
                throw BizException.of(ErrorCode.ADDRESS_DUPLICATED);
            }
            userAddressMapper.revive(row.getUserAddressId(), req.tag());
            return loadOwned(row.getUserAddressId(), accountId);
        }

        String userAddressId = UUID.randomUUID().toString();
        try {
            userAddressMapper.insert(userAddressId, accountId, geoAddressId,
                    req.contactName(), req.contactPhone(), req.detailAddr(), req.tag());
        } catch (DuplicateKeyException e) {
            // 并发插入由 uk_account_recipient 兜底
            throw BizException.of(ErrorCode.ADDRESS_DUPLICATED);
        }
        return loadOwned(userAddressId, accountId);
    }

    @Transactional
    public UserAddressResponse update(String accountId, String userAddressId, UserAddressSaveRequest req) {
        String geoAddressId = upsertGeoAddress(req);
        var current = userAddressMapper.findOwned(userAddressId, accountId)
                .filter(row -> row.getDeletedAt() == null)
                .orElseThrow(() -> BizException.of(ErrorCode.ADDRESS_NOT_FOUND));

        try {
            userAddressMapper.update(userAddressId, accountId,
                    req.detailAddr(), req.contactName(), req.contactPhone(), req.tag());
        } catch (DuplicateKeyException e) {
            throw BizException.of(ErrorCode.ADDRESS_DUPLICATED);
        }

        // 地址簿条目换了 POI 时，把 geo_address_id 一并改到新缓存行
        if (!geoAddressId.equals(current.getGeoAddressId())) {
            userAddressMapper.moveGeoAddress(userAddressId, accountId, geoAddressId);
        }
        return loadOwned(userAddressId, accountId);
    }

    public void delete(String accountId, String userAddressId) {
        int affected = userAddressMapper.softDelete(userAddressId, accountId);
        if (affected == 0) {
            throw BizException.of(ErrorCode.ADDRESS_NOT_FOUND);
        }
    }

    /** 按 poi_id 命中缓存就复用并刷新字段，未命中才插入，缓存永不物理删除。 */
    private String upsertGeoAddress(UserAddressSaveRequest req) {
        String city = req.city() == null || req.city().isBlank() ? req.province() : req.city();
        var cached = geoAddressMapper.findByPoiId(req.poiId());
        if (cached.isPresent()) {
            GeoAddressRow row = cached.get();
            geoAddressMapper.update(row.getGeoAddressId(), req.poiName(), req.adcode(),
                    req.province(), city, req.district(), req.lng(), req.lat());
            return row.getGeoAddressId();
        }

        String geoAddressId = UUID.randomUUID().toString();
        try {
            geoAddressMapper.insert(geoAddressId, req.poiId(), req.poiName(), req.adcode(),
                    req.province(), city, req.district(), req.lng(), req.lat());
        } catch (DuplicateKeyException e) {
            // 并发写同一 POI：回查已有行，保证 poi_id 唯一
            return geoAddressMapper.findByPoiId(req.poiId())
                    .orElseThrow(() -> BizException.of(ErrorCode.INTERNAL))
                    .getGeoAddressId();
        }
        return geoAddressId;
    }

    private UserAddressResponse loadOwned(String userAddressId, String accountId) {
        UserAddressRow row = userAddressMapper.findOwned(userAddressId, accountId)
                .orElseThrow(() -> BizException.of(ErrorCode.ADDRESS_NOT_FOUND));
        return toResponse(row);
    }

    private static UserAddressResponse toResponse(UserAddressRow row) {
        return new UserAddressResponse(
                row.getUserAddressId(),
                row.getPoiId(),
                row.getPoiName(),
                row.getAdcode(),
                row.getProvince(),
                row.getCity(),
                row.getDistrict(),
                row.getLng(),
                row.getLat(),
                row.getContactName(),
                row.getContactPhone(),
                row.getDetailAddr(),
                row.getTag(),
                row.getCreatedAt());
    }
}
