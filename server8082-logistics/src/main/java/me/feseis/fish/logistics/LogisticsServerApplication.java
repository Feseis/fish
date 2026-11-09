package me.feseis.fish.logistics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("me.feseis.fish.logistics.mapper")
public class LogisticsServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsServerApplication.class, args);
    }
}
