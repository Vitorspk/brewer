package com.algaworks.brewer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.algaworks.brewer.config.FlywayTestConfig;
import com.algaworks.brewer.config.TestConfig;
import com.algaworks.brewer.dto.VendaMes;
import com.algaworks.brewer.dto.VendaOrigem;
import com.algaworks.brewer.model.Cerveja;
import com.algaworks.brewer.model.Cliente;
import com.algaworks.brewer.model.Endereco;
import com.algaworks.brewer.model.Estado;
import com.algaworks.brewer.model.Cidade;
import com.algaworks.brewer.model.Estilo;
import com.algaworks.brewer.model.ItemVenda;
import com.algaworks.brewer.model.Origem;
import com.algaworks.brewer.model.Sabor;
import com.algaworks.brewer.model.StatusVenda;
import com.algaworks.brewer.model.TipoPessoa;
import com.algaworks.brewer.model.Venda;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@EnableAutoConfiguration(exclude = {
	SecurityAutoConfiguration.class,
	UserDetailsServiceAutoConfiguration.class
})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Import({FlywayTestConfig.class, TestConfig.class})
@DisplayName("Testes de Integração - VendasRepository")
class VendasIntegrationTest extends BaseRepositoryIntegrationTest {

	@Autowired
	private Vendas vendas;

	@Autowired
	private TestEntityManager entityManager;

	private Cliente cliente;
	private Cerveja cervejaNacional;
	private Cerveja cervejaInternacional;
	private Estilo estilo;

	@BeforeEach
	void setUp() {
		// Criar Estado e Cidade
		Estado estado = new Estado();
		estado.setNome("São Paulo");
		estado.setSigla("SP");
		estado = entityManager.persistAndFlush(estado);

		Cidade cidade = new Cidade();
		cidade.setNome("São Paulo");
		cidade.setEstado(estado);
		cidade = entityManager.persistAndFlush(cidade);

		// Criar cliente de teste
		cliente = new Cliente();
		cliente.setNome("Cliente Teste");
		cliente.setTipoPessoa(TipoPessoa.FISICA);
		cliente.setCpfOuCnpj("34608514090"); // CPF válido para testes
		cliente.setEmail("cliente@teste.com");

		Endereco endereco = new Endereco();
		endereco.setLogradouro("Rua Teste");
		endereco.setNumero("123");
		endereco.setCidade(cidade);
		endereco.setCep("01234-567");
		cliente.setEndereco(endereco);

		cliente = entityManager.persistAndFlush(cliente);

		// Criar estilo de cerveja
		estilo = new Estilo();
		estilo.setNome("Lager");
		estilo = entityManager.persistAndFlush(estilo);

		// Criar cervejas de teste
		cervejaNacional = criarCerveja("BR001", "Brahma", estilo, Origem.NACIONAL, new BigDecimal("5.00"));
		cervejaInternacional = criarCerveja("HN001", "Heineken", estilo, Origem.INTERNACIONAL, new BigDecimal("8.00"));
	}

	@Test
	@DisplayName("Deve retornar total de vendas por mês dos últimos 6 meses")
	void deveRetornarTotalVendasPorMes() {
		// Given - criar vendas em diferentes meses
		LocalDate hoje = LocalDate.now();

		// Vendas no mês atual
		criarVendaEmitida(hoje, cervejaNacional, 2);
		criarVendaEmitida(hoje.minusDays(5), cervejaNacional, 1);

		// Vendas há 2 meses
		criarVendaEmitida(hoje.minusMonths(2), cervejaNacional, 3);

		// Vendas há 4 meses
		criarVendaEmitida(hoje.minusMonths(4), cervejaNacional, 1);

		// Venda com status ORCAMENTO não deve ser contada
		criarVendaOrcamento(hoje, cervejaNacional, 1);

		entityManager.flush();
		entityManager.clear();

		// When
		List<VendaMes> resultado = vendas.totalPorMes();

		// Then
		assertThat(resultado).isNotNull();
		assertThat(resultado).hasSize(6); // Últimos 6 meses

		// Verificar que todos os meses estão presentes
		for (int i = 0; i < 6; i++) {
			YearMonth mes = YearMonth.from(hoje.minusMonths(5 - i));
			String nomeMes = mes.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"));

			VendaMes vendaMes = resultado.get(i);
			assertThat(vendaMes.getMes()).isEqualTo(nomeMes);
		}

		// Verificar contagens específicas
		VendaMes mesAtual = resultado.get(5); // Último elemento (mês mais recente)
		assertThat(mesAtual.getTotalVendas()).isEqualTo(2); // 2 vendas emitidas
	}

	@Test
	@DisplayName("Deve retornar zeros quando não houver vendas")
	void deveRetornarZerosQuandoNaoHouverVendas() {
		// When - sem criar vendas
		List<VendaMes> resultado = vendas.totalPorMes();

		// Then
		assertThat(resultado).isNotNull();
		assertThat(resultado).hasSize(6);
		assertThat(resultado).allMatch(v -> v.getTotalVendas() == 0);
	}

	@Test
	@DisplayName("Deve contar apenas vendas EMITIDAS por mês")
	void deveContarApenasVendasEmitidasPorMes() {
		// Given
		LocalDate hoje = LocalDate.now();

		// Criar 3 vendas emitidas
		criarVendaEmitida(hoje, cervejaNacional, 1);
		criarVendaEmitida(hoje.minusDays(1), cervejaNacional, 1);
		criarVendaEmitida(hoje.minusDays(2), cervejaNacional, 1);

		// Criar vendas com outros status (não devem ser contadas)
		criarVendaOrcamento(hoje, cervejaNacional, 1);
		criarVendaCancelada(hoje, cervejaNacional, 1);

		entityManager.flush();
		entityManager.clear();

		// When
		List<VendaMes> resultado = vendas.totalPorMes();

		// Then
		VendaMes mesAtual = resultado.get(5);
		assertThat(mesAtual.getTotalVendas()).isEqualTo(3); // Apenas as 3 emitidas
	}

	@Test
	@DisplayName("Deve retornar total de vendas por origem das cervejas")
	void deveRetornarTotalVendasPorOrigem() {
		// Given - criar vendas com cervejas de diferentes origens
		criarVendaEmitida(LocalDate.now(), cervejaNacional, 2);
		criarVendaEmitida(LocalDate.now(), cervejaInternacional, 3);

		// Venda com múltiplos itens de mesma origem
		Venda vendaMista = criarVendaBase(LocalDate.now(), StatusVenda.EMITIDA);
		adicionarItem(vendaMista, cervejaNacional, 1);
		adicionarItem(vendaMista, cervejaNacional, 1);
		vendas.save(vendaMista);

		entityManager.flush();
		entityManager.clear();

		// When
		List<VendaOrigem> resultado = vendas.totalPorOrigem();

		// Then
		assertThat(resultado).isNotNull();
		assertThat(resultado).hasSize(2);

		// Verificar totais por origem
		VendaOrigem nacional = resultado.stream()
			.filter(v -> v.getOrigem().equals("Nacional"))
			.findFirst()
			.orElse(null);

		VendaOrigem internacional = resultado.stream()
			.filter(v -> v.getOrigem().equals("Internacional"))
			.findFirst()
			.orElse(null);

		assertThat(nacional).isNotNull();
		assertThat(nacional.getTotalVendas()).isEqualTo(3); // 1 item da primeira venda + 2 itens da vendaMista

		assertThat(internacional).isNotNull();
		assertThat(internacional.getTotalVendas()).isEqualTo(1); // 1 item da segunda venda
	}

	@Test
	@DisplayName("Deve retornar lista vazia quando não houver vendas por origem")
	void deveRetornarListaVaziaQuandoNaoHouverVendasPorOrigem() {
		// When - sem criar vendas
		List<VendaOrigem> resultado = vendas.totalPorOrigem();

		// Then
		assertThat(resultado).isNotNull();
		assertThat(resultado).isEmpty();
	}

	@Test
	@DisplayName("Deve contar apenas vendas EMITIDAS por origem")
	void deveContarApenasVendasEmitidasPorOrigem() {
		// Given
		LocalDate hoje = LocalDate.now();

		// Vendas emitidas
		criarVendaEmitida(hoje, cervejaNacional, 2);

		// Vendas não emitidas (não devem ser contadas)
		criarVendaOrcamento(hoje, cervejaNacional, 3);
		criarVendaCancelada(hoje, cervejaNacional, 1);

		entityManager.flush();
		entityManager.clear();

		// When
		List<VendaOrigem> resultado = vendas.totalPorOrigem();

		// Then
		assertThat(resultado).hasSize(1);
		assertThat(resultado.get(0).getTotalVendas()).isEqualTo(1); // Conta 1 item de venda, não a quantidade
	}

	@Test
	@DisplayName("Deve retornar apenas origens que possuem vendas")
	void deveRetornarApenasOrigensComVendas() {
		// Given - criar vendas apenas com cervejas nacionais
		criarVendaEmitida(LocalDate.now(), cervejaNacional, 5);

		entityManager.flush();
		entityManager.clear();

		// When
		List<VendaOrigem> resultado = vendas.totalPorOrigem();

		// Then
		assertThat(resultado).hasSize(1);
		assertThat(resultado.get(0).getOrigem()).isEqualTo("Nacional");
	}

	// Helper methods

	private Cerveja criarCerveja(String sku, String nome, Estilo estilo, Origem origem, BigDecimal valor) {
		Cerveja cerveja = new Cerveja();
		cerveja.setSku(sku);
		cerveja.setNome(nome);
		cerveja.setDescricao("Descrição " + nome);
		cerveja.setValor(valor);
		cerveja.setTeorAlcoolico(new BigDecimal("4.5"));
		cerveja.setComissao(new BigDecimal("10.0"));
		cerveja.setQuantidadeEstoque(100);
		cerveja.setOrigem(origem);
		cerveja.setSabor(Sabor.SUAVE);
		cerveja.setEstilo(estilo);
		return entityManager.persistAndFlush(cerveja);
	}

	private Venda criarVendaBase(LocalDate data, StatusVenda status) {
		Venda venda = new Venda();
		venda.setDataCriacao(data);
		venda.setStatus(status);
		venda.setCliente(cliente);
		venda.setValorFrete(BigDecimal.ZERO);
		venda.setValorDesconto(BigDecimal.ZERO);
		return venda;
	}

	private void adicionarItem(Venda venda, Cerveja cerveja, int quantidade) {
		ItemVenda item = new ItemVenda();
		item.setCerveja(cerveja);
		item.setQuantidade(quantidade);
		item.setValorUnitario(cerveja.getValor());
		item.setVenda(venda);
		venda.getItens().add(item);
	}

	private Venda criarVendaEmitida(LocalDate data, Cerveja cerveja, int quantidade) {
		Venda venda = criarVendaBase(data, StatusVenda.EMITIDA);
		adicionarItem(venda, cerveja, quantidade);
		return vendas.save(venda);
	}

	private Venda criarVendaOrcamento(LocalDate data, Cerveja cerveja, int quantidade) {
		Venda venda = criarVendaBase(data, StatusVenda.ORCAMENTO);
		adicionarItem(venda, cerveja, quantidade);
		return vendas.save(venda);
	}

	private Venda criarVendaCancelada(LocalDate data, Cerveja cerveja, int quantidade) {
		Venda venda = criarVendaBase(data, StatusVenda.CANCELADA);
		adicionarItem(venda, cerveja, quantidade);
		return vendas.save(venda);
	}
}