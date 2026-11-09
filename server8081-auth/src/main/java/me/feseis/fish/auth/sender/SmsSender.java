package me.feseis.fish.auth.sender;

/**
 * 验证码发送通道。实现类负责把验证码投递到目标手机号。
 */
public interface SmsSender {

    void send(String phone, String code);
}
