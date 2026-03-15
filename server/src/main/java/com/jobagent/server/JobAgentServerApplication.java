package com.jobagent.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobAgentServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobAgentServerApplication.class, args);
    }
}
