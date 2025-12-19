package com.algaworks.brewer.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.algaworks.brewer.model.Cerveja;
import com.algaworks.brewer.model.Cliente;
import com.algaworks.brewer.model.ItemVenda;
import com.algaworks.brewer.model.StatusVenda;
import com.algaworks.brewer.model.TipoPessoa;
import com.algaworks.brewer.model.Venda;

@DisplayName("Testes do Template ResumoVenda")
class ResumoVendaTemplateTest {

	private TemplateEngine thymeleaf;
	private Venda venda;
	private Cliente cliente;

	@BeforeEach
	void setUp() {
		// Configure Thymeleaf template engine
		ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
		templateResolver.setPrefix("templates/");
		templateResolver.setSuffix(".html");
		templateResolver.setTemplateMode(TemplateMode.HTML);
		templateResolver.setCharacterEncoding("UTF-8");
		templateResolver.setCheckExistence(true);
		templateResolver.setCacheable(false);

		thymeleaf = new SpringTemplateEngine();
		((SpringTemplateEngine) thymeleaf).setTemplateResolver(templateResolver);

		// Setup cliente
		cliente = new Cliente();
		cliente.setCodigo(1L);
		cliente.setNome("João Silva");
		cliente.setEmail("joao.silva@example.com");
		cliente.setTipoPessoa(TipoPessoa.FISICA);
		cliente.setCpfOuCnpj("123.456.789-00");

		// Setup venda
		venda = new Venda();
		venda.setCodigo(1L);
		venda.setCliente(cliente);
		venda.setValorTotal(new BigDecimal("150.00"));
		venda.setValorFrete(new BigDecimal("10.00"));
		venda.setValorDesconto(new BigDecimal("5.00"));
		venda.setStatus(StatusVenda.EMITIDA);
		venda.setDataCriacao(LocalDate.now());
		venda.setDataEntrega(LocalDate.now().plusDays(3));
		venda.setHorarioEntrega(LocalTime.of(14, 30));
		venda.setObservacao("Entregar na portaria");
		venda.setItens(new ArrayList<>());

		// Add sample item
		ItemVenda item = new ItemVenda();
		Cerveja cerveja = new Cerveja();
		cerveja.setCodigo(1L);
		cerveja.setSku("AA1234");
		cerveja.setNome("Cerveja Pilsen");
		item.setCerveja(cerveja);
		item.setQuantidade(5);
		item.setValorUnitario(new BigDecimal("20.00"));
		venda.getItens().add(item);
	}

	@Test
	@DisplayName("Deve processar template de e-mail sem erros")
	void deveProcessarTemplateEmailSemErros() {
		// Given
		Context context = new Context(new Locale("pt", "BR"));
		Map<String, Object> variaveis = new HashMap<>();
		variaveis.put("venda", venda);
		variaveis.put("logo", "http://localhost:8080/layout/images/logo.png");
		context.setVariables(variaveis);

		// When
		String html = thymeleaf.process("mail/ResumoVenda", context);

		// Then
		assertThat(html).isNotNull();
		assertThat(html).isNotEmpty();
	}

	@Test
	@DisplayName("Template de e-mail deve conter informações da venda")
	void templateDeveConterInformacoesDaVenda() {
		// Given
		Context context = new Context(new Locale("pt", "BR"));
		Map<String, Object> variaveis = new HashMap<>();
		variaveis.put("venda", venda);
		variaveis.put("logo", "http://localhost:8080/layout/images/logo.png");
		context.setVariables(variaveis);

		// When
		String html = thymeleaf.process("mail/ResumoVenda", context);

		// Then
		assertThat(html).contains("João Silva");
		assertThat(html).contains("150");
		assertThat(html).contains("Venda");
	}

	@Test
	@DisplayName("Template de e-mail deve conter informações do cliente")
	void templateDeveConterInformacoesDoCliente() {
		// Given
		Context context = new Context(new Locale("pt", "BR"));
		Map<String, Object> variaveis = new HashMap<>();
		variaveis.put("venda", venda);
		variaveis.put("logo", "http://localhost:8080/layout/images/logo.png");
		context.setVariables(variaveis);

		// When
		String html = thymeleaf.process("mail/ResumoVenda", context);

		// Then
		assertThat(html).contains(cliente.getNome());
		assertThat(html).contains(cliente.getEmail());
	}

	@Test
	@DisplayName("Template de e-mail deve conter itens da venda incluindo SKU")
	void templateDeveConterItensDaVenda() {
		// Given
		Context context = new Context(new Locale("pt", "BR"));
		Map<String, Object> variaveis = new HashMap<>();
		variaveis.put("venda", venda);
		variaveis.put("logo", "http://localhost:8080/layout/images/logo.png");
		context.setVariables(variaveis);

		// When
		String html = thymeleaf.process("mail/ResumoVenda", context);

		// Then
		assertThat(html).contains("Cerveja Pilsen");
		assertThat(html).contains("AA1234"); // SKU deve estar presente
	}

	@Test
	@DisplayName("Template de e-mail deve ser HTML válido")
	void templateDeveSerHtmlValido() {
		// Given
		Context context = new Context(new Locale("pt", "BR"));
		Map<String, Object> variaveis = new HashMap<>();
		variaveis.put("venda", venda);
		variaveis.put("logo", "http://localhost:8080/layout/images/logo.png");
		context.setVariables(variaveis);

		// When
		String html = thymeleaf.process("mail/ResumoVenda", context);

		// Then
		assertThat(html).contains("<!DOCTYPE html>");
		assertThat(html).contains("<html");
		assertThat(html).contains("</html>");
		assertThat(html).contains("<body");
		assertThat(html).contains("</body>");
	}
}