package com.algaworks.brewer.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testes - VendaMes DTO")
class VendaMesTest {

	@Test
	@DisplayName("Deve criar VendaMes com construtor padrão")
	void deveCriarVendaMesComConstrutorPadrao() {
		// When
		VendaMes vendaMes = new VendaMes();

		// Then
		assertThat(vendaMes).isNotNull();
		assertThat(vendaMes.getMes()).isNull();
		assertThat(vendaMes.getTotalVendas()).isNull();
	}

	@Test
	@DisplayName("Deve criar VendaMes com construtor completo")
	void deveCriarVendaMesComConstrutorCompleto() {
		// Given
		String mes = "Jan";
		Integer totalVendas = 10;

		// When
		VendaMes vendaMes = new VendaMes(mes, totalVendas);

		// Then
		assertThat(vendaMes).isNotNull();
		assertThat(vendaMes.getMes()).isEqualTo("Jan");
		assertThat(vendaMes.getTotalVendas()).isEqualTo(10);
	}

	@Test
	@DisplayName("Deve definir e obter mês corretamente")
	void deveDefinirEObterMesCorretamente() {
		// Given
		VendaMes vendaMes = new VendaMes();

		// When
		vendaMes.setMes("Fev");

		// Then
		assertThat(vendaMes.getMes()).isEqualTo("Fev");
	}

	@Test
	@DisplayName("Deve definir e obter total de vendas corretamente")
	void deveDefinirEObterTotalVendasCorretamente() {
		// Given
		VendaMes vendaMes = new VendaMes();

		// When
		vendaMes.setTotalVendas(25);

		// Then
		assertThat(vendaMes.getTotalVendas()).isEqualTo(25);
	}

	@Test
	@DisplayName("Deve aceitar valores nulos")
	void deveAceitarValoresNulos() {
		// Given
		VendaMes vendaMes = new VendaMes("Jan", 10);

		// When
		vendaMes.setMes(null);
		vendaMes.setTotalVendas(null);

		// Then
		assertThat(vendaMes.getMes()).isNull();
		assertThat(vendaMes.getTotalVendas()).isNull();
	}

	@Test
	@DisplayName("Deve aceitar total de vendas zero")
	void deveAceitarTotalVendasZero() {
		// When
		VendaMes vendaMes = new VendaMes("Mar", 0);

		// Then
		assertThat(vendaMes.getTotalVendas()).isEqualTo(0);
	}

	@Test
	@DisplayName("Deve aceitar total de vendas negativo")
	void deveAceitarTotalVendasNegativo() {
		// When
		VendaMes vendaMes = new VendaMes("Abr", -5);

		// Then
		assertThat(vendaMes.getTotalVendas()).isEqualTo(-5);
	}

	@Test
	@DisplayName("Deve aceitar strings vazias para mês")
	void deveAceitarStringsVazias() {
		// When
		VendaMes vendaMes = new VendaMes("", 5);

		// Then
		assertThat(vendaMes.getMes()).isEmpty();
	}
}