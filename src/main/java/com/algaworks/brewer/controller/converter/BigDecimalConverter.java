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
 * PERFORMANCE FIX: Usa ThreadLocal para DecimalFormat ao invés de synchronized,
 * evitando contenção de threads em ambientes de alta concorrência.
 */
public class BigDecimalConverter implements Converter<String, BigDecimal> {

	// PERFORMANCE FIX: ThreadLocal para evitar sincronização
	// Cada thread tem sua própria instância de DecimalFormat
	private static final ThreadLocal<DecimalFormat> FORMAT_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
		symbols.setGroupingSeparator('.');
		symbols.setDecimalSeparator(',');

		DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
		format.setParseBigDecimal(true);
		return format;
	});

	@Override
	public BigDecimal convert(String source) {
		if (!StringUtils.hasText(source)) {
			return null;
		}

		source = source.trim();

		try {
			// Sem necessidade de sincronização - cada thread tem sua própria instância
			return (BigDecimal) FORMAT_THREAD_LOCAL.get().parse(source);
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
