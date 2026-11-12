package me.feseis.fish.logistics.controller;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.vo.address.PoiVO;
import me.feseis.fish.logistics.vo.address.RegeoRespVO;
import me.feseis.fish.logistics.vo.address.UserAddressRespVO;
import me.feseis.fish.logistics.vo.address.UserAddressSaveReqVO;
import me.feseis.fish.logistics.config.interceptor.AuthInterceptor;
import me.feseis.fish.logistics.service.AddressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 地址接口：逆地理编码与地址簿。全部需要登录，由 AuthInterceptor 注入账号。
 */
@RestController
@RequestMapping("/logistics/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/regeo")
    public RegeoRespVO regeo(@RequestParam BigDecimal lng, @RequestParam BigDecimal lat) {
        return addressService.regeo(lng, lat);
    }

    @GetMapping("/search")
    public List<PoiVO> search(@RequestParam String keyword,
                              @RequestParam(required = false) String city) {
        return addressService.search(keyword, city);
    }

    @GetMapping("/user")
    public List<UserAddressRespVO> list(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId) {
        return addressService.list(accountId);
    }

    @PostMapping("/user")
    public ResponseEntity<UserAddressRespVO> create(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @Valid @RequestBody UserAddressSaveReqVO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.create(accountId, req));
    }

    @PutMapping("/user/{userAddressId}")
    public UserAddressRespVO update(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @PathVariable String userAddressId,
            @Valid @RequestBody UserAddressSaveReqVO req) {
        return addressService.update(accountId, userAddressId, req);
    }

    @DeleteMapping("/user/{userAddressId}")
    public ResponseEntity<Void> delete(
            @RequestAttribute(AuthInterceptor.ATTR_ACCOUNT_ID) String accountId,
            @PathVariable String userAddressId) {
        addressService.delete(accountId, userAddressId);
        return ResponseEntity.ok().build();
    }
}
