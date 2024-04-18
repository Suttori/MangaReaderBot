package com.suttori;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableJpaRepositories("com.suttori.demobottty3.dao")
@EnableScheduling
@EnableFeignClients
public class MangaManhwaBot {

    public static void main(String[] args) {
        SpringApplication.run(MangaManhwaBot.class, args);
    }

}
