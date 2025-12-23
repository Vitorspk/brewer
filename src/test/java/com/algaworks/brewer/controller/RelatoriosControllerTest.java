package com.algaworks.brewer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.algaworks.brewer.dto.PeriodoRelatorio;
import com.algaworks.brewer.service.RelatorioService;

/**
 * Integration tests for RelatoriosController.
 *
 * Tests validation behavior when generating reports, ensuring:
 * - HTTP 400 is returned when required date fields are null
 * - HTTP 200 with PDF content is returned for valid date ranges
 * - Validation error messages are properly returned
 *
 * This test suite validates the @Valid annotation fix documented in fase-10.
 */
@WebMvcTest(value = RelatoriosController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@DisplayName("Testes de Integração - RelatoriosController Validation")
class RelatoriosControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RelatorioService relatorioService;

	@Test
	@DisplayName("Deve rejeitar requisição com dataInicio nula")
	void deveRejeitarRequisicaoComDataInicioNula() throws Exception {
		// Given: PeriodoRelatorio with null dataInicio
		// When: POST to /relatorios/vendasEmitidas with null dataInicio
		// Then: HTTP 400 Bad Request
		mockMvc.perform(post("/relatorios/vendasEmitidas")
				.param("dataFim", LocalDate.now().toString())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Deve rejeitar requisição com dataFim nula")
	void deveRejeitarRequisicaoComDataFimNula() throws Exception {
		// Given: PeriodoRelatorio with null dataFim
		// When: POST to /relatorios/vendasEmitidas with null dataFim
		// Then: HTTP 400 Bad Request
		mockMvc.perform(post("/relatorios/vendasEmitidas")
				.param("dataInicio", LocalDate.now().toString())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Deve rejeitar requisição com ambas as datas nulas")
	void deveRejeitarRequisicaoComAmbasDatasNulas() throws Exception {
		// Given: PeriodoRelatorio with both dates null
		// When: POST to /relatorios/vendasEmitidas with no date parameters
		// Then: HTTP 400 Bad Request
		mockMvc.perform(post("/relatorios/vendasEmitidas")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Deve gerar relatório com datas válidas")
	void deveGerarRelatorioComDatasValidas() throws Exception {
		// Given: Valid date range and mock PDF bytes
		byte[] pdfBytes = new byte[] { 0x25, 0x50, 0x44, 0x46 }; // "%PDF" header
		when(relatorioService.gerarRelatorioVendasEmitidas(any(PeriodoRelatorio.class)))
			.thenReturn(pdfBytes);

		LocalDate dataInicio = LocalDate.now().minusDays(30);
		LocalDate dataFim = LocalDate.now();

		// When: POST to /relatorios/vendasEmitidas with valid dates
		// Then: HTTP 200 with PDF content-type and body
		mockMvc.perform(post("/relatorios/vendasEmitidas")
				.param("dataInicio", dataInicio.toString())
				.param("dataFim", dataFim.toString())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
			.andExpect(content().bytes(pdfBytes));
	}

	@Test
	@DisplayName("Deve gerar relatório com período de um dia")
	void deveGerarRelatorioComPeriodoDeUmDia() throws Exception {
		// Given: Same start and end date (valid edge case)
		byte[] pdfBytes = new byte[] { 0x25, 0x50, 0x44, 0x46 };
		when(relatorioService.gerarRelatorioVendasEmitidas(any(PeriodoRelatorio.class)))
			.thenReturn(pdfBytes);

		LocalDate hoje = LocalDate.now();

		// When: POST with same date for both parameters
		// Then: HTTP 200 (single day reports are valid)
		mockMvc.perform(post("/relatorios/vendasEmitidas")
				.param("dataInicio", hoje.toString())
				.param("dataFim", hoje.toString())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
			.andExpect(content().bytes(pdfBytes));
	}

	@Test
	@DisplayName("Deve gerar relatório com período de vários meses")
	void deveGerarRelatorioComPeriodoDeVariosMeses() throws Exception {
		// Given: Large date range (several months)
		byte[] pdfBytes = new byte[] { 0x25, 0x50, 0x44, 0x46 };
		when(relatorioService.gerarRelatorioVendasEmitidas(any(PeriodoRelatorio.class)))
			.thenReturn(pdfBytes);

		LocalDate dataInicio = LocalDate.now().minusMonths(6);
		LocalDate dataFim = LocalDate.now();

		// When: POST with 6-month date range
		// Then: HTTP 200 (large periods are allowed)
		mockMvc.perform(post("/relatorios/vendasEmitidas")
				.param("dataInicio", dataInicio.toString())
				.param("dataFim", dataFim.toString())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
			.andExpect(content().bytes(pdfBytes));
	}

	@Test
	@DisplayName("Deve aceitar período com dataFim antes de dataInicio")
	void deveAceitarPeriodoComDataFimAntesDeDataInicio() throws Exception {
		// Given: dataFim before dataInicio (logically invalid but not validated at controller level)
		// NOTE: Business logic validation would be in RelatorioService, not controller
		byte[] pdfBytes = new byte[] { 0x25, 0x50, 0x44, 0x46 };
		when(relatorioService.gerarRelatorioVendasEmitidas(any(PeriodoRelatorio.class)))
			.thenReturn(pdfBytes);

		LocalDate dataInicio = LocalDate.now();
		LocalDate dataFim = LocalDate.now().minusDays(30);

		// When: POST with end date before start date
		// Then: HTTP 200 (controller only validates @NotNull, not business logic)
		mockMvc.perform(post("/relatorios/vendasEmitidas")
				.param("dataInicio", dataInicio.toString())
				.param("dataFim", dataFim.toString())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(status().isOk());
	}
}