package com.example.serviceapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.serviceapp", "com.example.loggingwrapper"})
public class ServiceappApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceappApplication.class, args);
	}

}
