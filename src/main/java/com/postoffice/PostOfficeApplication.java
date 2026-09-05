package com.postoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PostOfficeApplication {
    public static void main(String[] args) {
        SpringApplication.run(PostOfficeApplication.class, args);
    }
}
