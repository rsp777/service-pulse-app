package com.example.collector;

import io.micronaut.context.annotation.Value;
import io.micronaut.runtime.Micronaut;
import jakarta.inject.Singleton;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.context.event.StartupEvent;

@Singleton
public class Application {
    @Value("${app.message:Default Hello}")
    String message;

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }

    @EventListener
    public void onStartup(StartupEvent event) {
        System.out.println("Application started with message: " + message);
    }
}
