package com.example.fileingester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FileIngesterApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileIngesterApplication.class, args);
    }
}
