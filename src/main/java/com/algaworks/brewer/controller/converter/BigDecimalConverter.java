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
 *
 * Usa static final para DecimalFormat para melhor performance, evitando
 * criação de objetos a cada conversão.
 */
public class BigDecimalConverter implements Converter<String, BigDecimal> {

	private static final DecimalFormatSymbols SYMBOLS;
	private static final DecimalFormat FORMAT;

	static {
		SYMBOLS = new DecimalFormatSymbols(new Locale("pt", "BR"));
		SYMBOLS.setGroupingSeparator('.');
		SYMBOLS.setDecimalSeparator(',');

		FORMAT = new DecimalFormat("#,##0.00", SYMBOLS);
		FORMAT.setParseBigDecimal(true);
	}

	@Override
	public BigDecimal convert(String source) {
		if (!StringUtils.hasText(source)) {
			return null;
		}

		source = source.trim();

		try {
			// DecimalFormat não é thread-safe, então sincronizamos o acesso
			synchronized (FORMAT) {
				return (BigDecimal) FORMAT.parse(source);
			}
		} catch (ParseException e) {
			// Se falhar, tenta um fallback manual
			try {
				return new BigDecimal(source.replace(".", "").replace(",", "."));
			} catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Formato de número inválido: " + source, ex);
			}
		}
	}
}
