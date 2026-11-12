package me.feseis.fish.payment.client;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

import lombok.extern.slf4j.Slf4j;
import me.feseis.fish.payment.config.properties.AlipayProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 极简支付宝开放平台客户端（公钥模式 + RSA2），只覆盖扫码下单、主动查单、异步通知验签。
 *
 * <p>三条签名规则：</p>
 * <ol>
 *   <li>请求签名：除 sign 外的非空参数（包含 sign_type）按 key 字典序拼 k=v，用原始值，RSA2 签名后 Base64。</li>
 *   <li>同步响应验签：对响应 JSON 里业务节点的原始文本验签，不能重新序列化。</li>
 *   <li>异步通知验签：剔除 sign、sign_type，其余参数（已解码）排序拼串后验签。</li>
 * </ol>
 */
@Slf4j
@Component
public class AlipayClient {

    private static final String CHARSET = "UTF-8";
    private static final String SIGN_TYPE_RSA2 = "RSA2";
    private static final String ALGORITHM = "SHA256withRSA";
    private static final String CODE_SUCCESS = "10000";
    private static final String CODE_SIGN_ERROR = "40002";
    private static final String PRECREATE_NODE = "alipay_trade_precreate_response";
    private static final String QUERY_NODE = "alipay_trade_query_response";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId TIMESTAMP_ZONE = ZoneId.of("Asia/Shanghai");

    private final AlipayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile PrivateKey appPrivateKey;
    private volatile PublicKey alipayPublicKey;

    public AlipayClient(AlipayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public AlipayResult precreate(String outTradeNo, BigDecimal amount, String subject, int timeoutMinutes) {
        Map<String, String> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", amount.toPlainString());
        bizContent.put("subject", subject);
        if (timeoutMinutes > 0) {
            bizContent.put("timeout_express", timeoutMinutes + "m");
        }

        Map<String, String> params = baseParams("alipay.trade.precreate");
        params.put("biz_content", toJson(bizContent));
        if (properties.hasNotifyUrl()) {
            params.put("notify_url", properties.notifyUrl().trim());
        }
        return execute(params, PRECREATE_NODE);
    }

    public AlipayResult query(String outTradeNo) {
        Map<String, String> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", outTradeNo);

        Map<String, String> params = baseParams("alipay.trade.query");
        params.put("biz_content", toJson(bizContent));
        return execute(params, QUERY_NODE);
    }

    public boolean verifyNotify(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        String sign = trimToNull(params.get("sign"));
        if (sign == null) {
            return false;
        }
        String appId = trimToNull(params.get("app_id"));
        if (appId != null && !appId.equals(properties.appId())) {
            return false;
        }
        return verifyContent(notifySignContent(params), sign) || verifyContent(signContent(params), sign);
    }

    private Map<String, String> baseParams(String method) {
        Map<String, String> params = new TreeMap<>();
        params.put("app_id", properties.appId());
        params.put("method", method);
        params.put("format", "JSON");
        params.put("charset", "utf-8");
        params.put("sign_type", SIGN_TYPE_RSA2);
        params.put("version", "1.0");
        params.put("timestamp", ZonedDateTime.now(TIMESTAMP_ZONE).format(TIMESTAMP_FORMAT));
        return params;
    }

    private AlipayResult execute(Map<String, String> params, String nodeName) {
        if (!properties.isConfigured()) {
            return AlipayResult.failure("NOT_CONFIGURED", null, "支付宝沙箱未配置", null);
        }

        String raw;
        try {
            params.put("sign", signRequest(params));
            raw = post(formBody(params));
        } catch (RuntimeException ex) {
            return AlipayResult.failure("CLIENT_ERROR", null, "调用支付宝失败：" + ex.getMessage(), null);
        }

        String nodeJson = extractNodeJson(raw, nodeName);
        if (nodeJson == null) {
            return AlipayResult.failure("BAD_RESPONSE", null, "支付宝响应格式异常", raw);
        }

        String responseSign = extractSign(raw, nodeJson);
        if (responseSign == null || !verifyContent(nodeJson, responseSign)) {
            return AlipayResult.failure("BAD_SIGNATURE", null, "支付宝响应验签失败", raw);
        }

        Map<String, String> node = parseNode(nodeJson);
        String code = node.get("code");
        if (!CODE_SUCCESS.equals(code)) {
            String subCode = node.get("sub_code");
            if (CODE_SIGN_ERROR.equals(code) || (subCode != null && subCode.toLowerCase().contains("signature"))) {
                log.warn("支付宝验签失败，本地待签串：{}", signContent(params));
            }
            return AlipayResult.failure(code, subCode, pickMessage(node), raw);
        }
        return new AlipayResult(true, code, node.get("sub_code"), node.get("msg"),
                node.get("qr_code"), node.get("trade_status"), node.get("trade_no"),
                node.get("total_amount"), raw);
    }

    private String post(String body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.gateway().trim()))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = this.httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.body() == null ? "" : response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("调用支付宝被中断", ex);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private String signRequest(Map<String, String> params) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(appPrivateKey());
            signature.update(signContent(params).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception ex) {
            throw new IllegalStateException("请求签名失败：" + ex.getMessage(), ex);
        }
    }

    private boolean verifyContent(String content, String sign) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(alipayPublicKey());
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(decodeBase64(sign));
        } catch (Exception ex) {
            return false;
        }
    }

    private static String signContent(Map<String, String> params) {
        return joinSorted(params, false);
    }

    private static String notifySignContent(Map<String, String> params) {
        return joinSorted(params, true);
    }

    private static String joinSorted(Map<String, String> params, boolean dropSignType) {
        StringJoiner joiner = new StringJoiner("&");
        new TreeMap<>(params).forEach((key, value) -> {
            if (value == null || value.isEmpty() || "sign".equals(key)) {
                return;
            }
            if (dropSignType && "sign_type".equals(key)) {
                return;
            }
            joiner.add(key + "=" + value);
        });
        return joiner.toString();
    }

    private static String formBody(Map<String, String> params) {
        StringJoiner joiner = new StringJoiner("&");
        new TreeMap<>(params).forEach((key, value) -> {
            if (value == null) {
                return;
            }
            joiner.add(URLEncoder.encode(key, StandardCharsets.UTF_8)
                    + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
        });
        return joiner.toString();
    }

    private static String extractNodeJson(String body, String nodeName) {
        if (body == null) {
            return null;
        }
        int keyIndex = body.indexOf('"' + nodeName + '"');
        if (keyIndex < 0) {
            return null;
        }
        int colon = body.indexOf(':', keyIndex + nodeName.length() + 2);
        if (colon < 0) {
            return null;
        }
        int start = colon + 1;
        while (start < body.length() && Character.isWhitespace(body.charAt(start))) {
            start++;
        }
        if (start >= body.length() || body.charAt(start) != '{') {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < body.length(); index++) {
            char current = body.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return body.substring(start, index + 1);
                }
            }
        }
        return null;
    }

    private static String extractSign(String body, String nodeJson) {
        int from = body.indexOf(nodeJson);
        if (from < 0) {
            return null;
        }
        int keyIndex = body.indexOf("\"sign\"", from + nodeJson.length());
        if (keyIndex < 0) {
            return null;
        }
        int colon = body.indexOf(':', keyIndex + 6);
        if (colon < 0) {
            return null;
        }
        int index = colon + 1;
        while (index < body.length() && Character.isWhitespace(body.charAt(index))) {
            index++;
        }
        if (index >= body.length() || body.charAt(index) != '"') {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (index = index + 1; index < body.length(); index++) {
            char current = body.charAt(index);
            if (current == '\\') {
                index++;
                continue;
            }
            if (current == '"') {
                return builder.toString();
            }
            builder.append(current);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseNode(String nodeJson) {
        Map<String, Object> raw = (Map<String, Object>) this.objectMapper.readValue(nodeJson, Map.class);
        Map<String, String> node = new LinkedHashMap<>();
        raw.forEach((key, value) -> node.put(key, value == null ? null : String.valueOf(value)));
        return node;
    }

    private String toJson(Map<String, String> content) {
        return this.objectMapper.writeValueAsString(content);
    }

    private static String pickMessage(Map<String, String> node) {
        String subMessage = node.get("sub_msg");
        if (subMessage != null && !subMessage.isBlank()) {
            return subMessage;
        }
        String message = node.get("msg");
        return message == null ? "支付宝返回未知错误" : message;
    }

    private PrivateKey appPrivateKey() {
        PrivateKey cached = this.appPrivateKey;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (this.appPrivateKey == null) {
                this.appPrivateKey = parsePrivateKey(properties.privateKey());
            }
            return this.appPrivateKey;
        }
    }

    private PublicKey alipayPublicKey() {
        PublicKey cached = this.alipayPublicKey;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (this.alipayPublicKey == null) {
                this.alipayPublicKey = parsePublicKey(properties.publicKey());
            }
            return this.alipayPublicKey;
        }
    }

    private static PrivateKey parsePrivateKey(String base64) {
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decodeBase64(base64)));
        } catch (Exception ex) {
            throw new IllegalStateException("应用私钥解析失败，请确认是 PKCS8 格式的 RSA2 私钥", ex);
        }
    }

    private static PublicKey parsePublicKey(String base64) {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodeBase64(base64)));
        } catch (Exception ex) {
            throw new IllegalStateException("支付宝公钥解析失败，请确认填的是「支付宝公钥」而不是「应用公钥」", ex);
        }
    }

    private static byte[] decodeBase64(String value) {
        if (value == null) {
            throw new IllegalArgumentException("密钥为空");
        }
        String normalized = value
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
