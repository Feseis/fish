package me.feseis.fish.sms.enums;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import me.feseis.fish.sms.exp.BizException;
import me.feseis.fish.sms.exp.ErrorCode;

/**
 * 短信模板。文案进代码，不在数据库维护，改动必须发版。
 * 参数用 {1}..{n} 占位按顺序替换；签名前缀统一拼接。
 * 落库前按模板声明脱敏：验证码整段替换，支付链接只留域名与末段短码。
 */
public enum SmsTemplate {

    LOGIN_CODE("CODE", "验证码提醒：尊敬的用户，您正在使用验证码登录，验证码为 {1}", 1, 0, -1),
    PAY_REMIND("NOTICE", "您的到付快件 {1} 应付 {2} 元，请在 {3} 前完成支付：{4}", 4, -1, 3),
    PAY_REMIND_FINAL("NOTICE", "您的到付快件 {1} 应付 {2} 元，已逾期，请联系快递员处理", 2, -1, -1),
    ARREARS_REMIND("NOTICE", "您的快件 {1} 退回运费 {2} 元尚未支付，请及时结清：{3}", 3, -1, 2),
    ARRIVAL_NOTICE("NOTICE", "您的快件 {1} 已到达 {2}，请及时领取", 2, -1, -1);

    private static final String SIGN_PREFIX = "【飞鱼速递】";
    private static final String MASKED = "******";

    private final String messageType;
    private final String text;
    private final int paramCount;
    /** 需要整段脱敏的参数下标（0 起），-1 表示无。 */
    private final int codeIndex;
    /** 需要按 URL 规则脱敏的参数下标（0 起），-1 表示无。 */
    private final int urlIndex;

    SmsTemplate(String messageType, String text, int paramCount, int codeIndex, int urlIndex) {
        this.messageType = messageType;
        this.text = text;
        this.paramCount = paramCount;
        this.codeIndex = codeIndex;
        this.urlIndex = urlIndex;
    }

    public static SmsTemplate fromCode(String code) {
        for (SmsTemplate value : values()) {
            if (value.name().equals(code)) {
                return value;
            }
        }
        throw BizException.of(ErrorCode.TEMPLATE_NOT_FOUND, "未知模板码: " + code);
    }

    public void checkParams(List<String> params) {
        if (params == null || params.size() != paramCount) {
            throw BizException.of(ErrorCode.INVALID_PARAM,
                    "模板 " + name() + " 需要 " + paramCount + " 个参数");
        }
    }

    /** 发送用文案：参数原样填充。 */
    public String render(List<String> params) {
        return SIGN_PREFIX + fill(text, params);
    }

    /** 落库用文案：验证码与支付链接脱敏。 */
    public String renderMasked(List<String> params) {
        List<String> masked = new ArrayList<>(params);
        if (codeIndex >= 0) {
            masked.set(codeIndex, MASKED);
        }
        if (urlIndex >= 0) {
            masked.set(urlIndex, maskUrl(masked.get(urlIndex)));
        }
        return SIGN_PREFIX + fill(text, masked);
    }

    public String messageType() {
        return messageType;
    }

    private static String fill(String template, List<String> params) {
        String result = template;
        for (int i = 0; i < params.size(); i++) {
            result = result.replace("{" + (i + 1) + "}", params.get(i));
        }
        return result;
    }

    /** 支付链接只保留 scheme://host 与末段短码，避免完整 pay_token 进库、进日志。 */
    private static String maskUrl(String url) {
        if (url == null || url.isBlank()) {
            return MASKED;
        }
        try {
            URI uri = URI.create(url);
            if (uri.getHost() == null) {
                return MASKED;
            }
            String base = uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            String path = uri.getPath();
            if (path == null || path.isBlank() || "/".equals(path)) {
                return base;
            }
            String[] parts = path.split("/");
            return base + "/…/" + parts[parts.length - 1];
        } catch (IllegalArgumentException e) {
            return MASKED;
        }
    }
}
