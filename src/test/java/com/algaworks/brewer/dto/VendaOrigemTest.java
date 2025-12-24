package com.algaworks.brewer.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testes - VendaOrigem DTO")
class VendaOrigemTest {

	@Test
	@DisplayName("Deve criar VendaOrigem com construtor padrão")
	void deveCriarVendaOrigemComConstrutorPadrao() {
		// When
		VendaOrigem vendaOrigem = new VendaOrigem();

		// Then
		assertThat(vendaOrigem).isNotNull();
		assertThat(vendaOrigem.getOrigem()).isNull();
		assertThat(vendaOrigem.getTotalVendas()).isNull();
	}

	@Test
	@DisplayName("Deve criar VendaOrigem com construtor completo")
	void deveCriarVendaOrigemComConstrutorCompleto() {
		// Given
		String origem = "Nacional";
		Integer totalVendas = 15;

		// When
		VendaOrigem vendaOrigem = new VendaOrigem(origem, totalVendas);

		// Then
		assertThat(vendaOrigem).isNotNull();
		assertThat(vendaOrigem.getOrigem()).isEqualTo("Nacional");
		assertThat(vendaOrigem.getTotalVendas()).isEqualTo(15);
	}

	@Test
	@DisplayName("Deve definir e obter origem corretamente")
	void deveDefinirEObterOrigemCorretamente() {
		// Given
		VendaOrigem vendaOrigem = new VendaOrigem();

		// When
		vendaOrigem.setOrigem("Internacional");

		// Then
		assertThat(vendaOrigem.getOrigem()).isEqualTo("Internacional");
	}

	@Test
	@DisplayName("Deve definir e obter total de vendas corretamente")
	void deveDefinirEObterTotalVendasCorretamente() {
		// Given
		VendaOrigem vendaOrigem = new VendaOrigem();

		// When
		vendaOrigem.setTotalVendas(42);

		// Then
		assertThat(vendaOrigem.getTotalVendas()).isEqualTo(42);
	}

	@Test
	@DisplayName("Deve aceitar valores nulos")
	void deveAceitarValoresNulos() {
		// Given
		VendaOrigem vendaOrigem = new VendaOrigem("Nacional", 10);

		// When
		vendaOrigem.setOrigem(null);
		vendaOrigem.setTotalVendas(null);

		// Then
		assertThat(vendaOrigem.getOrigem()).isNull();
		assertThat(vendaOrigem.getTotalVendas()).isNull();
	}

	@Test
	@DisplayName("Deve aceitar total de vendas zero")
	void deveAceitarTotalVendasZero() {
		// When
		VendaOrigem vendaOrigem = new VendaOrigem("Nacional", 0);

		// Then
		assertThat(vendaOrigem.getTotalVendas()).isEqualTo(0);
	}

	@Test
	@DisplayName("Deve aceitar total de vendas negativo")
	void deveAceitarTotalVendasNegativo() {
		// When
		VendaOrigem vendaOrigem = new VendaOrigem("Internacional", -3);

		// Then
		assertThat(vendaOrigem.getTotalVendas()).isEqualTo(-3);
	}

	@Test
	@DisplayName("Deve aceitar strings vazias para origem")
	void deveAceitarStringsVazias() {
		// When
		VendaOrigem vendaOrigem = new VendaOrigem("", 8);

		// Then
		assertThat(vendaOrigem.getOrigem()).isEmpty();
	}
}