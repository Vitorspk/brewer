package com.algaworks.brewer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Import;

import com.algaworks.brewer.config.FlywayTestConfig;

import com.algaworks.brewer.model.Cidade;
import com.algaworks.brewer.model.Estado;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@EnableAutoConfiguration(exclude = {
	SecurityAutoConfiguration.class,
	UserDetailsServiceAutoConfiguration.class
})
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(FlywayTestConfig.class)
@DisplayName("Testes de Integração - CidadesRepository")
class CidadesIntegrationTest {

	@Autowired
	private Cidades cidades;

	@Autowired
	private TestEntityManager entityManager;

	private Estado estadoSP;
	private Estado estadoRJ;

	@BeforeEach
	void setUp() {
		// Criar estados de teste
		estadoSP = new Estado();
		estadoSP.setNome("São Paulo");
		estadoSP.setSigla("SP");
		estadoSP = entityManager.persistAndFlush(estadoSP);

		estadoRJ = new Estado();
		estadoRJ.setNome("Rio de Janeiro");
		estadoRJ.setSigla("RJ");
		estadoRJ = entityManager.persistAndFlush(estadoRJ);
	}

	@Test
	@DisplayName("Deve salvar uma cidade com sucesso")
	void deveSalvarCidadeComSucesso() {
		// Given
		Cidade cidade = criarCidade("Campinas", estadoSP);

		// When
		Cidade cidadeSalva = cidades.save(cidade);

		// Then
		assertThat(cidadeSalva).isNotNull();
		assertThat(cidadeSalva.getCodigo()).isNotNull();
		assertThat(cidadeSalva.getNome()).isEqualTo("Campinas");
		assertThat(cidadeSalva.getEstado().getSigla()).isEqualTo("SP");
	}

	@Test
	@DisplayName("Deve buscar cidade por código")
	void deveBuscarCidadePorCodigo() {
		// Given
		Cidade cidade = criarCidade("São Paulo", estadoSP);
		Cidade cidadeSalva = cidades.save(cidade);

		// When
		Cidade cidadeEncontrada = cidades.findById(cidadeSalva.getCodigo()).orElse(null);

		// Then
		assertThat(cidadeEncontrada).isNotNull();
		assertThat(cidadeEncontrada.getCodigo()).isEqualTo(cidadeSalva.getCodigo());
		assertThat(cidadeEncontrada.getNome()).isEqualTo("São Paulo");
	}

	@Test
	@DisplayName("Deve listar todas as cidades")
	void deveListarTodasCidades() {
		// Given
		cidades.save(criarCidade("São Paulo", estadoSP));
		cidades.save(criarCidade("Campinas", estadoSP));
		cidades.save(criarCidade("Rio de Janeiro", estadoRJ));

		// When
		Iterable<Cidade> todasCidades = cidades.findAll();

		// Then
		assertThat(todasCidades).hasSize(3);
	}

	@Test
	@DisplayName("Deve buscar cidades por código do estado")
	void deveBuscarCidadesPorEstado() {
		// Given
		cidades.save(criarCidade("São Paulo", estadoSP));
		cidades.save(criarCidade("Campinas", estadoSP));
		cidades.save(criarCidade("Santos", estadoSP));
		cidades.save(criarCidade("Rio de Janeiro", estadoRJ));

		// When
		List<Cidade> cidadesSP = cidades.findByEstadoCodigo(estadoSP.getCodigo());
		List<Cidade> cidadesRJ = cidades.findByEstadoCodigo(estadoRJ.getCodigo());

		// Then
		assertThat(cidadesSP).hasSize(3);
		assertThat(cidadesRJ).hasSize(1);
		assertThat(cidadesSP).allMatch(c -> c.getEstado().getSigla().equals("SP"));
	}

	@Test
	@DisplayName("Deve buscar cidade por nome e estado")
	void deveBuscarCidadePorNomeEEstado() {
		// Given
		cidades.save(criarCidade("São Paulo", estadoSP));
		cidades.save(criarCidade("Rio de Janeiro", estadoRJ));

		// When
		Cidade cidadeEncontrada = cidades.findByNomeAndEstado("São Paulo", estadoSP).orElse(null);
		Cidade cidadeNaoEncontrada = cidades.findByNomeAndEstado("São Paulo", estadoRJ).orElse(null);

		// Then
		assertThat(cidadeEncontrada).isNotNull();
		assertThat(cidadeEncontrada.getNome()).isEqualTo("São Paulo");
		assertThat(cidadeEncontrada.getEstado().getSigla()).isEqualTo("SP");
		assertThat(cidadeNaoEncontrada).isNull();
	}

	@Test
	@DisplayName("Deve atualizar cidade existente")
	void deveAtualizarCidadeExistente() {
		// Given
		Cidade cidade = criarCidade("Sampa", estadoSP);
		Cidade cidadeSalva = cidades.save(cidade);

		// When
		cidadeSalva.setNome("São Paulo");
		Cidade cidadeAtualizada = cidades.save(cidadeSalva);

		// Then
		assertThat(cidadeAtualizada.getCodigo()).isEqualTo(cidadeSalva.getCodigo());
		assertThat(cidadeAtualizada.getNome()).isEqualTo("São Paulo");
	}

	@Test
	@DisplayName("Deve deletar cidade")
	void deveDeletarCidade() {
		// Given
		Cidade cidade = criarCidade("Sorocaba", estadoSP);
		Cidade cidadeSalva = cidades.save(cidade);
		Long codigo = cidadeSalva.getCodigo();

		// When
		cidades.deleteById(codigo);

		// Then
		assertThat(cidades.findById(codigo)).isEmpty();
	}

	@Test
	@DisplayName("Deve retornar lista vazia quando estado não tem cidades")
	void deveRetornarListaVaziaQuandoEstadoNaoTemCidades() {
		// Given
		cidades.save(criarCidade("São Paulo", estadoSP));

		// When
		List<Cidade> cidadesRJ = cidades.findByEstadoCodigo(estadoRJ.getCodigo());

		// Then
		assertThat(cidadesRJ).isEmpty();
	}

	@Test
	@DisplayName("Deve verificar se cidade tem estado")
	void deveVerificarSeCidadeTemEstado() {
		// Given
		Cidade cidadeComEstado = criarCidade("Campinas", estadoSP);
		Cidade cidadeSemEstado = new Cidade();
		cidadeSemEstado.setNome("Cidade Sem Estado");

		// When
		boolean temEstado = cidadeComEstado.temEstado();
		boolean naoTemEstado = cidadeSemEstado.temEstado();

		// Then
		assertThat(temEstado).isTrue();
		assertThat(naoTemEstado).isFalse();
	}

	// Helper method
	private Cidade criarCidade(String nome, Estado estado) {
		Cidade cidade = new Cidade();
		cidade.setNome(nome);
		cidade.setEstado(estado);
		return cidade;
	}
}