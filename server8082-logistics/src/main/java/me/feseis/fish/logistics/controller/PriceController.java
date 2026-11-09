package me.feseis.fish.logistics.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.dto.price.PriceQuoteRequest;
import me.feseis.fish.logistics.dto.price.PriceQuoteResponse;
import me.feseis.fish.logistics.service.PriceQuoteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运费试算接口。只试算不落库。
 */
@RestController
@RequestMapping("/logistics/price")
@RequiredArgsConstructor
public class PriceController {

    private final PriceQuoteService priceQuoteService;

    @PostMapping("/quote")
    public PriceQuoteResponse quote(@Valid @RequestBody PriceQuoteRequest req) {
        return priceQuoteService.quote(req);
    }
}
