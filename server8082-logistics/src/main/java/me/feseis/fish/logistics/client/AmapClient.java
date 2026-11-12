package me.feseis.fish.logistics.client;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.logistics.client.model.amap.AmapPoi;
import me.feseis.fish.logistics.client.model.amap.AmapRegeoResult;
import me.feseis.fish.logistics.config.properties.LogisticsProperties;
import me.feseis.fish.logistics.exp.BizException;
import me.feseis.fish.logistics.exp.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * 高德逆地理编码客户端。只读，不落库；高德返回结构里 city 可能是数组，统一用 JsonNode 容错解析。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AmapClient {

    private static final int POI_RADIUS = 1000;
    private static final int POI_PAGE_SIZE = 20;

    private final RestClient amapRestClient;
    private final LogisticsProperties props;

    public AmapRegeoResult regeo(BigDecimal lng, BigDecimal lat) {
        JsonNode root;
        try {
            root = amapRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v3/geocode/regeo")
                            .queryParam("key", props.amap().key())
                            .queryParam("location", lng.toPlainString() + "," + lat.toPlainString())
                            .queryParam("extensions", "all")
                            .queryParam("radius", POI_RADIUS)
                            .queryParam("sortrule", "distance")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.warn("高德 regeo 调用失败: {}", e.getMessage());
            throw BizException.of(ErrorCode.AMAP_UNAVAILABLE);
        }

        if (root == null || !"1".equals(root.path("status").asText())) {
            log.warn("高德 regeo 返回失败 infocode={}", root == null ? null : root.path("infocode").asText());
            throw BizException.of(ErrorCode.AMAP_UNAVAILABLE);
        }

        JsonNode regeocode = root.path("regeocode");
        JsonNode component = regeocode.path("addressComponent");

        String adcode = text(component, "adcode");
        if (adcode == null) {
            throw BizException.of(ErrorCode.ADDRESS_NOT_FOUND, "该坐标没有可用的行政区划");
        }

        String province = text(component, "province");
        String city = text(component, "city");
        if (city == null) {
            city = province;
        }
        String district = text(component, "district");

        return new AmapRegeoResult(
                text(regeocode, "formatted_address"),
                province,
                city,
                district,
                adcode,
                parsePois(regeocode.path("pois"), province, city, district, adcode));
    }

    /**
     * 关键字搜索，返回候选 POI；city 非空时用 citylimit=true 限定城市。
     * 必须用 extensions=all：base 模式下 POI 的 adcode 返回空数组，会被解析层全部丢弃。
     */
    public List<AmapPoi> searchPlaces(String keyword, String city) {
        JsonNode root;
        try {
            root = amapRestClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/v3/place/text")
                                .queryParam("key", props.amap().key())
                                .queryParam("keywords", keyword)
                                .queryParam("offset", POI_PAGE_SIZE)
                                .queryParam("page", 1)
                                .queryParam("extensions", "all");
                        if (city != null && !city.isBlank()) {
                            builder.queryParam("city", city).queryParam("citylimit", "true");
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.warn("高德 place/text 调用失败: {}", e.getMessage());
            throw BizException.of(ErrorCode.AMAP_UNAVAILABLE);
        }

        if (root == null || !"1".equals(root.path("status").asText())) {
            log.warn("高德 place/text 返回失败 infocode={}", root == null ? null : root.path("infocode").asText());
            throw BizException.of(ErrorCode.AMAP_UNAVAILABLE);
        }

        List<AmapPoi> items = new ArrayList<>();
        JsonNode pois = root.path("pois");
        if (pois == null || !pois.isArray()) {
            return items;
        }
        for (JsonNode poi : pois) {
            String poiId = text(poi, "id");
            String location = text(poi, "location");
            String adcode = text(poi, "adcode");
            if (poiId == null || location == null || adcode == null) {
                continue;
            }
            String[] parts = location.split(",");
            if (parts.length != 2) {
                continue;
            }
            String province = text(poi, "pname");
            String cityName = text(poi, "cityname");
            if (cityName == null) {
                cityName = province;
            }
            items.add(new AmapPoi(
                    poiId,
                    text(poi, "name"),
                    adcode,
                    province,
                    cityName,
                    text(poi, "adname"),
                    text(poi, "address"),
                    new BigDecimal(parts[0]),
                    new BigDecimal(parts[1])));
        }
        return items;
    }

    /**
     * POI 只带 id/name/location/address，不带行政区划；行政区划一律取定位点的 addressComponent。
     * 这与业务语义一致：用户选的是自己位置附近的锚点，归属行政区由自己的定位决定。
     */
    private List<AmapPoi> parsePois(JsonNode pois, String province, String city, String district, String adcode) {
        List<AmapPoi> items = new ArrayList<>();
        if (pois == null || !pois.isArray()) {
            return items;
        }
        for (JsonNode poi : pois) {
            String poiId = text(poi, "id");
            String location = text(poi, "location");
            if (poiId == null || location == null) {
                continue;
            }
            String[] parts = location.split(",");
            if (parts.length != 2) {
                continue;
            }
            items.add(new AmapPoi(
                    poiId,
                    text(poi, "name"),
                    adcode,
                    province,
                    city,
                    district,
                    text(poi, "address"),
                    new BigDecimal(parts[0]),
                    new BigDecimal(parts[1])));
        }
        return items;
    }

    /** 高德字段可能是字符串、空数组或缺失，统一取第一个非空文本。 */
    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.isArray()
                ? (value.isEmpty() ? null : value.get(0).asText())
                : value.asText();
        return text == null || text.isBlank() ? null : text;
    }
}
