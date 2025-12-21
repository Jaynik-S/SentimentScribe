package com.sentimentscribe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SentimentScribeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentimentScribeApplication.class, args);
    }
}
