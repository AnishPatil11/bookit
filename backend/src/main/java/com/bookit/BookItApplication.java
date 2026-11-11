package com.bookit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BookItApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookItApplication.class, args);
    }
}
