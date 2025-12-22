package com.algaworks.brewer.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public class PeriodoRelatorio {

	@NotNull(message = "Data de início é obrigatória")
	private LocalDate dataInicio;

	@NotNull(message = "Data de fim é obrigatória")
	private LocalDate dataFim;

	public LocalDate getDataInicio() {
		return dataInicio;
	}

	public void setDataInicio(LocalDate dataInicio) {
		this.dataInicio = dataInicio;
	}

	public LocalDate getDataFim() {
		return dataFim;
	}

	public void setDataFim(LocalDate dataFim) {
		this.dataFim = dataFim;
	}
}