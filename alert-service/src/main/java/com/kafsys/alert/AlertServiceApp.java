package com.kafsys.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableDiscoveryClient
@EnableKafka
public class AlertServiceApp {

    public static void main(String[] args) {
        SpringApplication.run(AlertServiceApp.class, args);
    }
}
