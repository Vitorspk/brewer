package com.algaworks.brewer.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de Clock para injeção de dependência.
 *
 * Permite testar código que depende de data/hora atual substituindo
 * o Clock por um mock nos testes.
 *
 * TESTABILITY FIX: Phase 12 - Medium Priority Issue #2
 * Antes: LocalDate.now() hardcoded não era testável
 * Depois: Clock injetável permite testes determinísticos
 */
@Configuration
public class ClockConfig {

	/**
	 * Fornece um Clock baseado no timezone do sistema.
	 *
	 * Nos testes, este bean pode ser substituído por Clock.fixed()
	 * para criar cenários de teste determinísticos.
	 */
	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}
}