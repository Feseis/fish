package me.feseis.fish.logistics.config;

import lombok.RequiredArgsConstructor;
import me.feseis.fish.logistics.config.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/logistics/address/**")
                .addPathPatterns("/logistics/waybill/**")
                .addPathPatterns("/logistics/handover/**")
                .addPathPatterns("/logistics/staff/**");
    }
}
