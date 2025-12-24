package com.algaworks.brewer.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.algaworks.brewer.dto.VendaMes;
import com.algaworks.brewer.dto.VendaOrigem;
import com.algaworks.brewer.repository.Cervejas;
import com.algaworks.brewer.repository.Clientes;
import com.algaworks.brewer.repository.Estilos;
import com.algaworks.brewer.repository.Vendas;
import com.algaworks.brewer.security.AppUserDetailsService;

@WebMvcTest(DashboardController.class)
@ActiveProfiles("test")
@DisplayName("Testes - DashboardController")
class DashboardControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private Vendas vendas;

	@MockBean
	private Cervejas cervejas;

	@MockBean
	private Clientes clientes;

	@MockBean
	private Estilos estilos;

	@MockBean
	private AppUserDetailsService userDetailsService;

	@Test
	@DisplayName("Deve retornar total de vendas por mês com sucesso")
	@WithMockUser
	void deveRetornarTotalVendasPorMes() throws Exception {
		// Given
		var vendasMes = List.of(
			new VendaMes("Jan", 10),
			new VendaMes("Fev", 15),
			new VendaMes("Mar", 20),
			new VendaMes("Abr", 12),
			new VendaMes("Mai", 18),
			new VendaMes("Jun", 25)
		);

		when(vendas.totalPorMes()).thenReturn(vendasMes);

		// When & Then
		mockMvc.perform(get("/dashboard/vendas/totalPorMes")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$.length()").value(6))
			.andExpect(jsonPath("$[0].mes").value("Jan"))
			.andExpect(jsonPath("$[0].totalVendas").value(10))
			.andExpect(jsonPath("$[1].mes").value("Fev"))
			.andExpect(jsonPath("$[1].totalVendas").value(15))
			.andExpect(jsonPath("$[5].mes").value("Jun"))
			.andExpect(jsonPath("$[5].totalVendas").value(25));
	}

	@Test
	@DisplayName("Deve retornar lista vazia quando não houver vendas por mês")
	@WithMockUser
	void deveRetornarListaVaziaQuandoNaoHouverVendasPorMes() throws Exception {
		// Given
		when(vendas.totalPorMes()).thenReturn(List.of());

		// When & Then
		mockMvc.perform(get("/dashboard/vendas/totalPorMes")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Deve retornar vendas com total zero")
	@WithMockUser
	void deveRetornarVendasComTotalZero() throws Exception {
		// Given
		var vendasMes = List.of(
			new VendaMes("Jan", 0),
			new VendaMes("Fev", 0),
			new VendaMes("Mar", 0),
			new VendaMes("Abr", 0),
			new VendaMes("Mai", 0),
			new VendaMes("Jun", 0)
		);

		when(vendas.totalPorMes()).thenReturn(vendasMes);

		// When & Then
		mockMvc.perform(get("/dashboard/vendas/totalPorMes")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$.length()").value(6))
			.andExpect(jsonPath("$[0].totalVendas").value(0))
			.andExpect(jsonPath("$[5].totalVendas").value(0));
	}

	@Test
	@DisplayName("Deve retornar total de vendas por origem com sucesso")
	@WithMockUser
	void deveRetornarTotalVendasPorOrigem() throws Exception {
		// Given
		var vendasOrigem = List.of(
			new VendaOrigem("Nacional", 45),
			new VendaOrigem("Internacional", 30)
		);

		when(vendas.totalPorOrigem()).thenReturn(vendasOrigem);

		// When & Then
		mockMvc.perform(get("/dashboard/vendas/totalPorOrigem")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].origem").value("Nacional"))
			.andExpect(jsonPath("$[0].totalVendas").value(45))
			.andExpect(jsonPath("$[1].origem").value("Internacional"))
			.andExpect(jsonPath("$[1].totalVendas").value(30));
	}

	@Test
	@DisplayName("Deve retornar lista vazia quando não houver vendas por origem")
	@WithMockUser
	void deveRetornarListaVaziaQuandoNaoHouverVendasPorOrigem() throws Exception {
		// Given
		when(vendas.totalPorOrigem()).thenReturn(List.of());

		// When & Then
		mockMvc.perform(get("/dashboard/vendas/totalPorOrigem")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Deve retornar apenas uma origem quando houver vendas de um tipo")
	@WithMockUser
	void deveRetornarApenasUmaOrigemQuandoHouverVendasDeUmTipo() throws Exception {
		// Given
		var vendasOrigem = List.of(
			new VendaOrigem("Nacional", 50)
		);

		when(vendas.totalPorOrigem()).thenReturn(vendasOrigem);

		// When & Then
		mockMvc.perform(get("/dashboard/vendas/totalPorOrigem")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].origem").value("Nacional"))
			.andExpect(jsonPath("$[0].totalVendas").value(50));
	}

	@Test
	@DisplayName("Deve retornar vendas por origem com total zero")
	@WithMockUser
	void deveRetornarVendasPorOrigemComTotalZero() throws Exception {
		// Given
		var vendasOrigem = List.of(
			new VendaOrigem("Nacional", 0),
			new VendaOrigem("Internacional", 0)
		);

		when(vendas.totalPorOrigem()).thenReturn(vendasOrigem);

		// When & Then
		mockMvc.perform(get("/dashboard/vendas/totalPorOrigem")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].totalVendas").value(0))
			.andExpect(jsonPath("$[1].totalVendas").value(0));
	}

	@Test
	@DisplayName("Deve aceitar requisição GET no endpoint totalPorMes")
	@WithMockUser
	void deveAceitarRequisicaoGetNoEndpointTotalPorMes() throws Exception {
		// Given
		var vendasMes = List.of(
			new VendaMes("Dez", 5)
		);

		when(vendas.totalPorMes()).thenReturn(vendasMes);

		// When & Then
		mockMvc.perform(get("/dashboard/vendas/totalPorMes")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].mes").value("Dez"));
	}

	@Test
	@DisplayName("Deve retornar dados com valores altos de vendas")
	@WithMockUser
	void deveRetornarDadosComValoresAltosDeVendas() throws Exception {
		// Given
		var vendasMes = List.of(
			new VendaMes("Jan", 99999)
		);

		when(vendas.totalPorMes()).thenReturn(vendasMes);

		// When & Then
		mockMvc.perform(get("/dashboard/vendas/totalPorMes")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].totalVendas").value(99999));
	}
}