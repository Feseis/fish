package me.feseis.fish.sms.config;

import me.feseis.fish.sms.config.properties.SmsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class SmsBeanConfig {

    /** pbox 投递客户端：超时压短，调用方不能被 pbox 拖住。 */
    @Bean
    public RestClient pboxRestClient(SmsProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        return RestClient.builder()
                .baseUrl(props.pbox().baseUrl())
                .requestFactory(factory)
                .build();
    }
}
