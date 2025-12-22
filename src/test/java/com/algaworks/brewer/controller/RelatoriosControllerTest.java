package com.algaworks.brewer.controller;

import com.algaworks.brewer.dto.PeriodoRelatorio;
import com.algaworks.brewer.service.RelatorioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RelatoriosController
 *
 * Tests the validation chain completed in Part 4:
 * - PeriodoRelatorio has @NotNull annotations
 * - Controller has @Valid to trigger validation
 * - Spring MVC validates and returns HTTP 400 for invalid requests
 */
@WebMvcTest(RelatoriosController.class)
class RelatoriosControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RelatorioService relatorioService;

	@Test
	void deveRejeitarRequisicaoComDatasNulas() throws Exception {
		// Given: Request with null dates (missing required fields)
		// When: POST to /relatorios/vendasEmitidas without dates
		// Then: Should return HTTP 400 with validation errors
		mockMvc.perform(post("/relatorios/vendasEmitidas")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(status().isBadRequest());
	}

	@Test
	void deveGerarRelatorioComDatasValidas() throws Exception {
		// Given: Valid date range
		LocalDate dataInicio = LocalDate.of(2025, 1, 1);
		LocalDate dataFim = LocalDate.of(2025, 1, 31);
		byte[] pdfBytes = "PDF_CONTENT".getBytes();

		when(relatorioService.gerarRelatorioVendasEmitidas(any(PeriodoRelatorio.class)))
			.thenReturn(pdfBytes);

		// When: POST to /relatorios/vendasEmitidas with valid dates
		// Then: Should return HTTP 200 with PDF content
		mockMvc.perform(post("/relatorios/vendasEmitidas")
				.param("dataInicio", dataInicio.toString())
				.param("dataFim", dataFim.toString())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
			.andExpect(content().bytes(pdfBytes));
	}

	@Test
	void deveRejeitarRequisicaoComDataInicioNula() throws Exception {
		// Given: Request with only dataFim (dataInicio is null)
		LocalDate dataFim = LocalDate.of(2025, 1, 31);

		// When: POST with missing dataInicio
		// Then: Should return HTTP 400 (validation error)
		mockMvc.perform(post("/relatorios/vendasEmitidas")
				.param("dataFim", dataFim.toString())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(status().isBadRequest());
	}

	@Test
	void deveRejeitarRequisicaoComDataFimNula() throws Exception {
		// Given: Request with only dataInicio (dataFim is null)
		LocalDate dataInicio = LocalDate.of(2025, 1, 1);

		// When: POST with missing dataFim
		// Then: Should return HTTP 400 (validation error)
		mockMvc.perform(post("/relatorios/vendasEmitidas")
				.param("dataInicio", dataInicio.toString())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(status().isBadRequest());
	}

	@Test
	void deveAceitarPeriodoDeUmDia() throws Exception {
		// Given: Same date for inicio and fim (valid edge case)
		LocalDate data = LocalDate.of(2025, 1, 15);
		byte[] pdfBytes = "PDF_CONTENT".getBytes();

		when(relatorioService.gerarRelatorioVendasEmitidas(any(PeriodoRelatorio.class)))
			.thenReturn(pdfBytes);

		// When: POST with same date for both fields
		// Then: Should accept and generate report
		mockMvc.perform(post("/relatorios/vendasEmitidas")
				.param("dataInicio", data.toString())
				.param("dataFim", data.toString())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE));
	}

	@Test
	void deveAceitarPeriodoLongo() throws Exception {
		// Given: Long date range (1 year)
		LocalDate dataInicio = LocalDate.of(2024, 1, 1);
		LocalDate dataFim = LocalDate.of(2024, 12, 31);
		byte[] pdfBytes = "PDF_CONTENT".getBytes();

		when(relatorioService.gerarRelatorioVendasEmitidas(any(PeriodoRelatorio.class)))
			.thenReturn(pdfBytes);

		// When: POST with long date range
		// Then: Should accept and generate report
		mockMvc.perform(post("/relatorios/vendasEmitidas")
				.param("dataInicio", dataInicio.toString())
				.param("dataFim", dataFim.toString())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE));
	}
}