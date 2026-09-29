package com.eftworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EftWorkerServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(EftWorkerServiceApplication.class, args);
  }
}
