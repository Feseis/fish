package me.feseis.fish.sms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("me.feseis.fish.sms.mapper")
public class SmsServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmsServerApplication.class, args);
    }
}
