package br.com.neurohelp.tcc_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "br.com.neurohelp.tcc_backend")
@EntityScan("br.com.neurohelp.tcc_backend")
@EnableJpaRepositories("br.com.neurohelp.tcc_backend")
public class TccBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TccBackendApplication.class, args);
	}

}