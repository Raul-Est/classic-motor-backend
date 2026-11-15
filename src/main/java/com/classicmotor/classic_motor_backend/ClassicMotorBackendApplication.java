package com.classicmotor.classic_motor_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport
public class ClassicMotorBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClassicMotorBackendApplication.class, args);
	}

}
