package com.algaworks.brewer.venda;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import com.algaworks.brewer.model.Cerveja;
import com.algaworks.brewer.session.TabelaItensVenda;

@DisplayName("Tabela de Itens de Venda")
class TabelaItensVendaTest {

	private TabelaItensVenda tabelaItensVenda;

	@BeforeEach
	void setUp() {
		this.tabelaItensVenda = new TabelaItensVenda();
	}

	@Test
	@DisplayName("Deve calcular valor total sem itens")
	void deveCalcularValorTotalSemItens() {
		assertEquals(BigDecimal.ZERO, tabelaItensVenda.getValorTotal());
	}

	@Test
	@DisplayName("Deve calcular valor total com um item")
	void deveCalcularValorTotalComUmItem() {
		Cerveja cerveja = new Cerveja();
		BigDecimal valor = new BigDecimal("8.90");
		cerveja.setValor(valor);

		tabelaItensVenda.adicionarItem(cerveja, 1);

		assertEquals(valor, tabelaItensVenda.getValorTotal());
	}

	@Test
	@DisplayName("Deve calcular valor total com vários itens")
	void deveCalcularValorTotalComVariosItens() {
		Cerveja c1 = new Cerveja();
		BigDecimal v1 = new BigDecimal("8.90");
		c1.setValor(v1);

		Cerveja c2 = new Cerveja();
		BigDecimal v2 = new BigDecimal("4.99");
		c2.setValor(v2);

		Cerveja c3 = new Cerveja();
		BigDecimal v3 = new BigDecimal("5.90");
		c3.setValor(v3);

		Cerveja c4 = new Cerveja();
		BigDecimal v4 = new BigDecimal("5.99");
		c4.setValor(v4);

		tabelaItensVenda.adicionarItem(c1, 1);
		tabelaItensVenda.adicionarItem(c2, 2);
		tabelaItensVenda.adicionarItem(c3, 3);
		tabelaItensVenda.adicionarItem(c4, 4);

		assertEquals(new BigDecimal("60.54"), tabelaItensVenda.getValorTotal());
	}

}
