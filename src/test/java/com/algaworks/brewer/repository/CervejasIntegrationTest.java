package com.algaworks.brewer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Import;

import com.algaworks.brewer.config.FlywayTestConfig;

import com.algaworks.brewer.config.TestConfig;
import com.algaworks.brewer.model.Cerveja;
import com.algaworks.brewer.model.Estilo;
import com.algaworks.brewer.model.Origem;
import com.algaworks.brewer.model.Sabor;
import com.algaworks.brewer.repository.filter.CervejaFilter;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@EnableAutoConfiguration(exclude = {
	SecurityAutoConfiguration.class,
	UserDetailsServiceAutoConfiguration.class
})
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(FlywayTestConfig.class)
@Import(TestConfig.class)
@DisplayName("Testes de Integração - CervejasRepository")
class CervejasIntegrationTest {

	@Autowired
	private Cervejas cervejas;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private Estilos estilos;

	private Estilo estiloIPA;
	private Estilo estiloPilsen;

	@BeforeEach
	void setUp() {
		// Criar estilos de teste
		estiloIPA = new Estilo();
		estiloIPA.setNome("IPA");
		estiloIPA = entityManager.persistAndFlush(estiloIPA);

		estiloPilsen = new Estilo();
		estiloPilsen.setNome("Pilsen");
		estiloPilsen = entityManager.persistAndFlush(estiloPilsen);
	}

	@Test
	@DisplayName("Deve salvar uma cerveja com sucesso")
	void deveSalvarCervejaComSucesso() {
		// Given
		Cerveja cerveja = criarCerveja("BR0001", "Brahma", "Cerveja tradicional",
				new BigDecimal("3.50"), estiloIPA, Origem.NACIONAL, Sabor.SUAVE);

		// When
		Cerveja cervejaSalva = cervejas.save(cerveja);

		// Then
		assertThat(cervejaSalva).isNotNull();
		assertThat(cervejaSalva.getCodigo()).isNotNull();
		assertThat(cervejaSalva.getSku()).isEqualTo("BR0001");
		assertThat(cervejaSalva.getNome()).isEqualTo("Brahma");
	}

	@Test
	@DisplayName("Deve buscar cerveja por código")
	void deveBuscarCervejaPorCodigo() {
		// Given
		Cerveja cerveja = criarCerveja("SK0001", "Skol", "Cerveja leve",
				new BigDecimal("2.90"), estiloPilsen, Origem.NACIONAL, Sabor.SUAVE);
		Cerveja cervejaSalva = cervejas.save(cerveja);

		// When
		Cerveja cervejaEncontrada = cervejas.findById(cervejaSalva.getCodigo()).orElse(null);

		// Then
		assertThat(cervejaEncontrada).isNotNull();
		assertThat(cervejaEncontrada.getCodigo()).isEqualTo(cervejaSalva.getCodigo());
		assertThat(cervejaEncontrada.getSku()).isEqualTo("SK0001");
	}

	@Test
	@DisplayName("Deve listar todas as cervejas")
	void deveListarTodasCervejas() {
		// Given
		cervejas.save(criarCerveja("BR0001", "Brahma", "Cerveja 1",
				new BigDecimal("3.50"), estiloIPA, Origem.NACIONAL, Sabor.SUAVE));
		cervejas.save(criarCerveja("SK0001", "Skol", "Cerveja 2",
				new BigDecimal("2.90"), estiloPilsen, Origem.NACIONAL, Sabor.SUAVE));
		cervejas.save(criarCerveja("HN0001", "Heineken", "Cerveja 3",
				new BigDecimal("5.50"), estiloPilsen, Origem.INTERNACIONAL, Sabor.FORTE));

		// When
		Iterable<Cerveja> todasCervejas = cervejas.findAll();

		// Then
		assertThat(todasCervejas).hasSize(3);
	}

	@Test
	@DisplayName("Deve filtrar cervejas por SKU")
	void deveFiltrarCervejasPorSku() {
		// Given
		cervejas.save(criarCerveja("BR0001", "Brahma", "Cerveja 1",
				new BigDecimal("3.50"), estiloIPA, Origem.NACIONAL, Sabor.SUAVE));
		cervejas.save(criarCerveja("SK0001", "Skol", "Cerveja 2",
				new BigDecimal("2.90"), estiloPilsen, Origem.NACIONAL, Sabor.SUAVE));

		// When
		CervejaFilter filtro = new CervejaFilter();
		filtro.setSku("BR0001");
		Pageable pageable = PageRequest.of(0, 10);
		Page<Cerveja> resultado = cervejas.filtrar(filtro, pageable);

		// Then
		assertThat(resultado.getContent()).hasSize(1);
		assertThat(resultado.getContent().get(0).getSku()).isEqualTo("BR0001");
	}

	@Test
	@DisplayName("Deve filtrar cervejas por nome (case insensitive)")
	void deveFiltrarCervejasPorNome() {
		// Given
		cervejas.save(criarCerveja("BR0001", "Brahma Extra", "Cerveja 1",
				new BigDecimal("3.50"), estiloIPA, Origem.NACIONAL, Sabor.SUAVE));
		cervejas.save(criarCerveja("SK0001", "Skol Beats", "Cerveja 2",
				new BigDecimal("2.90"), estiloPilsen, Origem.NACIONAL, Sabor.SUAVE));
		cervejas.save(criarCerveja("HN0001", "Heineken", "Cerveja 3",
				new BigDecimal("5.50"), estiloPilsen, Origem.INTERNACIONAL, Sabor.FORTE));

		// When
		CervejaFilter filtro = new CervejaFilter();
		filtro.setNome("brahma");
		Pageable pageable = PageRequest.of(0, 10);
		Page<Cerveja> resultado = cervejas.filtrar(filtro, pageable);

		// Then
		assertThat(resultado.getContent()).hasSize(1);
		assertThat(resultado.getContent().get(0).getNome()).containsIgnoringCase("brahma");
	}

	@Test
	@DisplayName("Deve filtrar cervejas por estilo")
	void deveFiltrarCervejasPorEstilo() {
		// Given
		cervejas.save(criarCerveja("BR0001", "Brahma", "Cerveja 1",
				new BigDecimal("3.50"), estiloIPA, Origem.NACIONAL, Sabor.SUAVE));
		cervejas.save(criarCerveja("SK0001", "Skol", "Cerveja 2",
				new BigDecimal("2.90"), estiloPilsen, Origem.NACIONAL, Sabor.SUAVE));
		cervejas.save(criarCerveja("HN0001", "Heineken", "Cerveja 3",
				new BigDecimal("5.50"), estiloPilsen, Origem.INTERNACIONAL, Sabor.FORTE));

		// When
		CervejaFilter filtro = new CervejaFilter();
		filtro.setEstilo(estiloPilsen);
		Pageable pageable = PageRequest.of(0, 10);
		Page<Cerveja> resultado = cervejas.filtrar(filtro, pageable);

		// Then
		assertThat(resultado.getContent()).hasSize(2);
		assertThat(resultado.getContent())
				.allMatch(c -> c.getEstilo().getCodigo().equals(estiloPilsen.getCodigo()));
	}

	@Test
	@DisplayName("Deve filtrar cervejas por origem")
	void deveFiltrarCervejasPorOrigem() {
		// Given
		cervejas.save(criarCerveja("BR0001", "Brahma", "Cerveja 1",
				new BigDecimal("3.50"), estiloIPA, Origem.NACIONAL, Sabor.SUAVE));
		cervejas.save(criarCerveja("SK0001", "Skol", "Cerveja 2",
				new BigDecimal("2.90"), estiloPilsen, Origem.NACIONAL, Sabor.SUAVE));
		cervejas.save(criarCerveja("HN0001", "Heineken", "Cerveja 3",
				new BigDecimal("5.50"), estiloPilsen, Origem.INTERNACIONAL, Sabor.FORTE));

		// When
		CervejaFilter filtro = new CervejaFilter();
		filtro.setOrigem(Origem.INTERNACIONAL);
		Pageable pageable = PageRequest.of(0, 10);
		Page<Cerveja> resultado = cervejas.filtrar(filtro, pageable);

		// Then
		assertThat(resultado.getContent()).hasSize(1);
		assertThat(resultado.getContent().get(0).getOrigem()).isEqualTo(Origem.INTERNACIONAL);
	}

	@Test
	@DisplayName("Deve filtrar cervejas por sabor")
	void deveFiltrarCervejasPorSabor() {
		// Given
		cervejas.save(criarCerveja("BR0001", "Brahma", "Cerveja 1",
				new BigDecimal("3.50"), estiloIPA, Origem.NACIONAL, Sabor.SUAVE));
		cervejas.save(criarCerveja("SK0001", "Skol", "Cerveja 2",
				new BigDecimal("2.90"), estiloPilsen, Origem.NACIONAL, Sabor.SUAVE));
		cervejas.save(criarCerveja("HN0001", "Heineken", "Cerveja 3",
				new BigDecimal("5.50"), estiloPilsen, Origem.INTERNACIONAL, Sabor.FORTE));

		// When
		CervejaFilter filtro = new CervejaFilter();
		filtro.setSabor(Sabor.FORTE);
		Pageable pageable = PageRequest.of(0, 10);
		Page<Cerveja> resultado = cervejas.filtrar(filtro, pageable);

		// Then
		assertThat(resultado.getContent()).hasSize(1);
		assertThat(resultado.getContent().get(0).getSabor()).isEqualTo(Sabor.FORTE);
	}

	@Test
	@DisplayName("Deve filtrar cervejas por faixa de valor")
	void deveFiltrarCervejasPorFaixaDeValor() {
		// Given
		cervejas.save(criarCerveja("BR0001", "Brahma", "Cerveja 1",
				new BigDecimal("3.50"), estiloIPA, Origem.NACIONAL, Sabor.SUAVE));
		cervejas.save(criarCerveja("SK0001", "Skol", "Cerveja 2",
				new BigDecimal("2.90"), estiloPilsen, Origem.NACIONAL, Sabor.SUAVE));
		cervejas.save(criarCerveja("HN0001", "Heineken", "Cerveja 3",
				new BigDecimal("5.50"), estiloPilsen, Origem.INTERNACIONAL, Sabor.FORTE));

		// When - filtrar cervejas entre R$3,00 e R$4,00
		CervejaFilter filtro = new CervejaFilter();
		filtro.setValorDe(new BigDecimal("3.00"));
		filtro.setValorAte(new BigDecimal("4.00"));
		Pageable pageable = PageRequest.of(0, 10);
		Page<Cerveja> resultado = cervejas.filtrar(filtro, pageable);

		// Then
		assertThat(resultado.getContent()).hasSize(1);
		assertThat(resultado.getContent().get(0).getValor()).isBetween(
				new BigDecimal("3.00"), new BigDecimal("4.00"));
	}

	@Test
	@DisplayName("Deve atualizar cerveja existente")
	void deveAtualizarCervejaExistente() {
		// Given
		Cerveja cerveja = criarCerveja("BR0001", "Brahma", "Cerveja tradicional",
				new BigDecimal("3.50"), estiloIPA, Origem.NACIONAL, Sabor.SUAVE);
		Cerveja cervejaSalva = cervejas.save(cerveja);

		// When
		cervejaSalva.setNome("Brahma Extra");
		cervejaSalva.setValor(new BigDecimal("4.50"));
		Cerveja cervejaAtualizada = cervejas.save(cervejaSalva);

		// Then
		assertThat(cervejaAtualizada.getCodigo()).isEqualTo(cervejaSalva.getCodigo());
		assertThat(cervejaAtualizada.getNome()).isEqualTo("Brahma Extra");
		assertThat(cervejaAtualizada.getValor()).isEqualByComparingTo(new BigDecimal("4.50"));
	}

	@Test
	@DisplayName("Deve deletar cerveja")
	void deveDeletarCerveja() {
		// Given
		Cerveja cerveja = criarCerveja("BR0001", "Brahma", "Cerveja tradicional",
				new BigDecimal("3.50"), estiloIPA, Origem.NACIONAL, Sabor.SUAVE);
		Cerveja cervejaSalva = cervejas.save(cerveja);
		Long codigo = cervejaSalva.getCodigo();

		// When
		cervejas.deleteById(codigo);

		// Then
		assertThat(cervejas.findById(codigo)).isEmpty();
	}

	@Test
	@DisplayName("Deve retornar página vazia quando não encontrar resultados")
	void deveRetornarPaginaVaziaQuandoNaoEncontrarResultados() {
		// Given
		cervejas.save(criarCerveja("BR0001", "Brahma", "Cerveja 1",
				new BigDecimal("3.50"), estiloIPA, Origem.NACIONAL, Sabor.SUAVE));

		// When
		CervejaFilter filtro = new CervejaFilter();
		filtro.setSku("INEXISTENTE");
		Pageable pageable = PageRequest.of(0, 10);
		Page<Cerveja> resultado = cervejas.filtrar(filtro, pageable);

		// Then
		assertThat(resultado.getContent()).isEmpty();
		assertThat(resultado.getTotalElements()).isZero();
	}

	@Test
	@DisplayName("Deve paginar resultados corretamente")
	void devePaginarResultadosCorretamente() {
		// Given - criar 5 cervejas
		for (int i = 1; i <= 5; i++) {
			cervejas.save(criarCerveja(String.format("SK%04d", i), "Cerveja " + i, "Descrição " + i,
					new BigDecimal("3.50"), estiloIPA, Origem.NACIONAL, Sabor.SUAVE));
		}

		// When - buscar página 0 com 2 elementos
		Pageable pageable = PageRequest.of(0, 2);
		Page<Cerveja> resultado = cervejas.filtrar(new CervejaFilter(), pageable);

		// Then
		assertThat(resultado.getContent()).hasSize(2);
		assertThat(resultado.getTotalElements()).isEqualTo(5);
		assertThat(resultado.getTotalPages()).isEqualTo(3);
		assertThat(resultado.getNumber()).isZero(); // página 0
	}

	@Test
	@DisplayName("Deve converter SKU para maiúsculas ao salvar")
	void deveConverterSkuParaMaiusculasAoSalvar() {
		// Given
		Cerveja cerveja = criarCerveja("br0001", "Brahma", "Cerveja tradicional",
				new BigDecimal("3.50"), estiloIPA, Origem.NACIONAL, Sabor.SUAVE);

		// When
		Cerveja cervejaSalva = cervejas.save(cerveja);
		entityManager.flush();
		entityManager.clear();

		// Then
		Cerveja cervejaRecuperada = cervejas.findById(cervejaSalva.getCodigo()).orElse(null);
		assertThat(cervejaRecuperada).isNotNull();
		assertThat(cervejaRecuperada.getSku()).isEqualTo("BR0001"); // convertido para maiúsculas
	}

	// Helper method - SKU format: XX9999 (2 letters + 4 digits)
	private Cerveja criarCerveja(String sku, String nome, String descricao,
			BigDecimal valor, Estilo estilo, Origem origem, Sabor sabor) {
		Cerveja cerveja = new Cerveja();
		cerveja.setSku(sku);
		cerveja.setNome(nome);
		cerveja.setDescricao(descricao);
		cerveja.setValor(valor);
		cerveja.setTeorAlcoolico(new BigDecimal("5.0"));
		cerveja.setComissao(new BigDecimal("10.0"));
		cerveja.setQuantidadeEstoque(100);
		cerveja.setOrigem(origem);
		cerveja.setSabor(sabor);
		cerveja.setEstilo(estilo);
		return cerveja;
	}
}