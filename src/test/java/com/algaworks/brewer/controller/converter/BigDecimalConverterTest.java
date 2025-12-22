package com.algaworks.brewer.controller.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BigDecimalConverter
 *
 * Tests the ThreadLocal optimization that replaced synchronized block
 * to ensure thread safety and correct parsing of Brazilian decimal format.
 */
class BigDecimalConverterTest {

	private BigDecimalConverter converter;

	@BeforeEach
	void setUp() {
		converter = new BigDecimalConverter();
	}

	@Test
	void deveConverterFormatoBrasileiroSimples() {
		// Given: Simple Brazilian format with comma as decimal separator
		String valor = "8,50";

		// When: Converting to BigDecimal
		BigDecimal result = converter.convert(valor);

		// Then: Should parse correctly
		assertEquals(new BigDecimal("8.50"), result);
	}

	@Test
	void deveConverterFormatoBrasileiroComMilhares() {
		// Given: Brazilian format with thousands separator
		String valor = "1.234,56";

		// When: Converting to BigDecimal
		BigDecimal result = converter.convert(valor);

		// Then: Should parse correctly removing dots and converting comma
		assertEquals(new BigDecimal("1234.56"), result);
	}

	@Test
	void deveConverterFormatoBrasileiroGrande() {
		// Given: Large number in Brazilian format
		String valor = "1.234.567,89";

		// When: Converting to BigDecimal
		BigDecimal result = converter.convert(valor);

		// Then: Should parse correctly
		assertEquals(new BigDecimal("1234567.89"), result);
	}

	@Test
	void deveRetornarNullParaStringVazia() {
		// Given: Empty string
		String valor = "";

		// When: Converting to BigDecimal
		BigDecimal result = converter.convert(valor);

		// Then: Should return null
		assertNull(result);
	}

	@Test
	void deveRetornarNullParaNull() {
		// Given: Null input
		String valor = null;

		// When: Converting to BigDecimal
		BigDecimal result = converter.convert(valor);

		// Then: Should return null
		assertNull(result);
	}

	@Test
	void deveRetornarNullParaStringComApenasEspacos() {
		// Given: String with only whitespace
		String valor = "   ";

		// When: Converting to BigDecimal
		BigDecimal result = converter.convert(valor);

		// Then: Should return null
		assertNull(result);
	}

	@Test
	void deveLancarExcecaoParaFormatoInvalido() {
		// Given: Invalid format
		String valor = "abc";

		// When/Then: Should throw IllegalArgumentException
		assertThrows(IllegalArgumentException.class, () -> converter.convert(valor));
	}

	@Test
	void deveConverterStringComEspacos() {
		// Given: String with leading/trailing spaces
		String valor = "  1.234,56  ";

		// When: Converting to BigDecimal
		BigDecimal result = converter.convert(valor);

		// Then: Should trim and parse correctly
		assertEquals(new BigDecimal("1234.56"), result);
	}

	@Test
	void deveSerThreadSafeEmAltaConcorrencia() throws InterruptedException, ExecutionException {
		// Given: ThreadLocal-based converter (performance fix from Part 3)
		// 10 threads processing 100 conversions each = 1000 total conversions
		int threadCount = 10;
		int conversionsPerThread = 100;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		List<Future<BigDecimal>> futures = new ArrayList<>();

		// When: Multiple threads convert simultaneously
		for (int i = 0; i < threadCount * conversionsPerThread; i++) {
			futures.add(executor.submit(() -> converter.convert("1.234,56")));
		}

		// Then: All conversions should succeed and return correct value
		for (Future<BigDecimal> future : futures) {
			BigDecimal result = future.get();
			assertEquals(new BigDecimal("1234.56"), result,
				"Thread safety issue detected: incorrect conversion result");
		}

		executor.shutdown();
		assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
			"Thread pool did not terminate in time");
	}

	@Test
	void deveUsarFallbackManualQuandoDecimalFormatFalha() {
		// Given: Format that DecimalFormat might not handle but fallback will
		String valor = "1234,56"; // No thousands separator

		// When: Converting to BigDecimal
		BigDecimal result = converter.convert(valor);

		// Then: Should use fallback and parse correctly
		assertEquals(new BigDecimal("1234.56"), result);
	}

	@Test
	void deveConverterZero() {
		// Given: Zero value
		String valor = "0,00";

		// When: Converting to BigDecimal
		BigDecimal result = converter.convert(valor);

		// Then: Should parse correctly
		assertEquals(new BigDecimal("0.00"), result);
	}

	@Test
	void deveConverterNumeroNegativo() {
		// Given: Negative number in Brazilian format
		String valor = "-1.234,56";

		// When: Converting to BigDecimal
		BigDecimal result = converter.convert(valor);

		// Then: Should parse correctly maintaining sign
		assertEquals(new BigDecimal("-1234.56"), result);
	}

	@Test
	void deveConverterNumeroPequeno() {
		// Given: Small decimal number
		String valor = "0,01";

		// When: Converting to BigDecimal
		BigDecimal result = converter.convert(valor);

		// Then: Should parse correctly
		assertEquals(new BigDecimal("0.01"), result);
	}
}