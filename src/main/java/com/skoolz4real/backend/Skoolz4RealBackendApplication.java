package com.skoolz4real.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class Skoolz4RealBackendApplication {
    public static void main(String[] args) {
        try {
            SpringApplication.run(Skoolz4RealBackendApplication.class, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}