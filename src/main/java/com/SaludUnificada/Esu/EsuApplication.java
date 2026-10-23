package com.SaludUnificada.Esu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@EnableAsync
public class EsuApplication {

	public static void main(String[] args) {
		SpringApplication.run(EsuApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		System.out.println("======================================");
		System.out.println("🌐 Swagger UI: http://localhost:8080/swagger-ui.html");
		System.out.println("📄 API Docs:  http://localhost:8080/v3/api-docs");
		System.out.println("======================================");
	}
}