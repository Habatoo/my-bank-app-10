package io.github.habatoo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CashApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashApplication.class, args);
    }
}
