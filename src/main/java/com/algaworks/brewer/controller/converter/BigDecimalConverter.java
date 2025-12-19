package com.algaworks.brewer.controller.converter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

/**
 * Conversor de String para BigDecimal que aceita formato brasileiro.
 *
 * Converte strings no formato brasileiro (ex: "8,50", "1.234,56") para BigDecimal.
 * Remove pontos (separador de milhares) e substitui vírgula por ponto.
 */
public class BigDecimalConverter implements Converter<String, BigDecimal> {

	@Override
	public BigDecimal convert(String source) {
		if (!StringUtils.hasText(source)) {
			return null;
		}

		// Remove espaços em branco
		source = source.trim();

		try {
			// Configura formato brasileiro
			DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
			symbols.setGroupingSeparator('.');
			symbols.setDecimalSeparator(',');

			DecimalFormat format = new DecimalFormat("#,##0.##", symbols);
			format.setParseBigDecimal(true);

			return (BigDecimal) format.parse(source);
		} catch (ParseException e) {
			// Se falhar, tenta formato americano (ponto como decimal)
			try {
				return new BigDecimal(source.replace(".", "").replace(",", "."));
			} catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Formato de número inválido: " + source, ex);
			}
		}
	}
}
