package com.bbpms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * BBPMS - Modular Monolith Application Entry Point.
 *
 * <p>Replaces 11 microservices + 10 middleware with a single Spring Boot application.
 * Cross-module communication uses Spring {@code ApplicationEventPublisher}
 * (replacing RabbitMQ), local service injection (replacing Feign), and
 * {@code @Transactional} (replacing Seata).
 */
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@MapperScan("com.bbpms.**.mapper")
@SpringBootApplication
public class BbpmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BbpmsApplication.class, args);
    }
}
