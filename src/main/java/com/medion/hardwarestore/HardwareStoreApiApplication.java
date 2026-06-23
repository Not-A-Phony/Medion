package com.medion.hardwarestore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class HardwareStoreApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HardwareStoreApiApplication.class, args);
	}

}
