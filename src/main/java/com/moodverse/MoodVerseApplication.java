package com.moodverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MoodVerseApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoodVerseApplication.class, args);
    }
}
