package com.template.OAuth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.template.OAuth")
public class OAuthApplication {
	public static void main(String[] args) {
		SpringApplication.run(OAuthApplication.class, args);
	}

}
