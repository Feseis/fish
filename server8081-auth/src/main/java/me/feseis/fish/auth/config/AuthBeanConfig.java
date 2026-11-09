package me.feseis.fish.auth.config;

import me.feseis.fish.auth.config.properties.AuthProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;

@Configuration
public class AuthBeanConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** pbox 投递客户端：超时压短，验证码接口不能被上游拖住。 */
    @Bean
    public RestClient pboxRestClient(AuthProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        return RestClient.builder()
                .baseUrl(props.sms().pbox().baseUrl())
                .requestFactory(factory)
                .build();
    }
}
