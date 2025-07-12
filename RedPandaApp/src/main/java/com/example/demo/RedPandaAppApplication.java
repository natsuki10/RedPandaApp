package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RedPandaAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedPandaAppApplication.class, args);
		
		System.out.println("Red Pandaアプリが起動しました！");
	}

}
