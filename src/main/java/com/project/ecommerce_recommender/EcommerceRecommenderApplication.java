package com.project.ecommerce_recommender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class EcommerceRecommenderApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcommerceRecommenderApplication.class, args);
	}

}
