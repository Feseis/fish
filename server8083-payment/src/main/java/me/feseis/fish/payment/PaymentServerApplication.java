package me.feseis.fish.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("me.feseis.fish.payment.mapper")
public class PaymentServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServerApplication.class, args);
    }
}
