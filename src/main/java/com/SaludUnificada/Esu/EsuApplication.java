package com.SaludUnificada.Esu;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
public class EsuApplication {

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Cordoba"));
	}

	public static void main(String[] args) {
		SpringApplication.run(EsuApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		System.out.println("======================================");
		System.out.println("Swagger UI: http://localhost:8080/swagger-ui.html");
		System.out.println("API Docs: http://localhost:8080/v3/api-docs");
		System.out.println("======================================");
	}
}
