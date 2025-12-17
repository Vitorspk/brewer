package com.algaworks.brewer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal da aplicação Spring Boot Brewer.
 *
 * @SpringBootApplication é uma anotação de conveniência que combina:
 * - @Configuration: Marca a classe como fonte de definições de beans
 * - @EnableAutoConfiguration: Habilita auto-configuration do Spring Boot
 * - @ComponentScan: Escaneia componentes no pacote e subpacotes
 *
 * FASE 3: Migração para Spring Boot 2.7
 * - Substituiu AppInitializer (Servlet 3.0 initializer)
 * - Auto-configuration elimina necessidade de configurações manuais
 * - Embedded Tomcat elimina necessidade de web.xml e context.xml
 */
@SpringBootApplication
public class BrewerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BrewerApplication.class, args);
	}

}