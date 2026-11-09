package me.feseis.fish.logistics.config;

import me.feseis.fish.logistics.config.properties.LogisticsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class LogisticsBeanConfig {

    /** 高德客户端：超时压短，地址录入不能被上游拖住。 */
    @Bean
    public RestClient amapRestClient(LogisticsProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(3000);
        return RestClient.builder()
                .baseUrl(props.amap().baseUrl())
                .requestFactory(factory)
                .build();
    }

    /** auth 服务客户端：令牌校验，超时同样压短。 */
    @Bean
    public RestClient authRestClient(LogisticsProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(3000);
        return RestClient.builder()
                .baseUrl(props.auth().baseUrl())
                .requestFactory(factory)
                .build();
    }
}
