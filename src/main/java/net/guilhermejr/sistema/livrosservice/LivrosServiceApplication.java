package net.guilhermejr.sistema.livrosservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LivrosServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LivrosServiceApplication.class, args);
	}

}
