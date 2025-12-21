package com.algaworks.brewer.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.test.context.TestPropertySource;

import com.algaworks.brewer.config.FlywayTestConfig;
import com.algaworks.brewer.model.Estilo;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@EnableAutoConfiguration(exclude = {
	SecurityAutoConfiguration.class,
	UserDetailsServiceAutoConfiguration.class
})
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(FlywayTestConfig.class)
@DisplayName("Testes de Integração - EstilosRepository")
class EstilosIntegrationTest extends BaseRepositoryIntegrationTest {

	@Autowired
	private Estilos estilos;

	@Test
	@DisplayName("Deve salvar um estilo com sucesso")
	void deveSalvarEstiloComSucesso() {
		// Given
		Estilo estilo = criarEstilo("IPA");

		// When
		Estilo estiloSalvo = estilos.save(estilo);

		// Then
		assertThat(estiloSalvo).isNotNull();
		assertThat(estiloSalvo.getCodigo()).isNotNull();
		assertThat(estiloSalvo.getNome()).isEqualTo("IPA");
	}

	@Test
	@DisplayName("Deve buscar estilo por código")
	void deveBuscarEstiloPorCodigo() {
		// Given
		Estilo estilo = criarEstilo("Pilsen");
		Estilo estiloSalvo = estilos.save(estilo);

		// When
		Estilo estiloEncontrado = estilos.findById(estiloSalvo.getCodigo()).orElse(null);

		// Then
		assertThat(estiloEncontrado).isNotNull();
		assertThat(estiloEncontrado.getCodigo()).isEqualTo(estiloSalvo.getCodigo());
		assertThat(estiloEncontrado.getNome()).isEqualTo("Pilsen");
	}

	@Test
	@DisplayName("Deve listar todos os estilos")
	void deveListarTodosEstilos() {
		// Given
		estilos.save(criarEstilo("IPA"));
		estilos.save(criarEstilo("Pilsen"));
		estilos.save(criarEstilo("Lager"));

		// When
		Iterable<Estilo> todosEstilos = estilos.findAll();

		// Then
		assertThat(todosEstilos).hasSize(3);
	}

	@Test
	@DisplayName("Deve buscar estilo por nome (case insensitive)")
	void deveBuscarEstiloPorNome() {
		// Given
		estilos.save(criarEstilo("India Pale Ale"));

		// When
		Estilo estiloEncontrado = estilos.findByNomeIgnoreCase("india pale ale").orElse(null);

		// Then
		assertThat(estiloEncontrado).isNotNull();
		assertThat(estiloEncontrado.getNome()).isEqualToIgnoringCase("India Pale Ale");
	}

	@Test
	@DisplayName("Não deve encontrar estilo com nome inexistente")
	void naoDeveEncontrarEstiloInexistente() {
		// Given
		estilos.save(criarEstilo("IPA"));

		// When
		Estilo estiloEncontrado = estilos.findByNomeIgnoreCase("Inexistente").orElse(null);

		// Then
		assertThat(estiloEncontrado).isNull();
	}

	@Test
	@DisplayName("Deve atualizar estilo existente")
	void deveAtualizarEstiloExistente() {
		// Given
		Estilo estilo = criarEstilo("IPA");
		Estilo estiloSalvo = estilos.save(estilo);

		// When
		estiloSalvo.setNome("American IPA");
		Estilo estiloAtualizado = estilos.save(estiloSalvo);

		// Then
		assertThat(estiloAtualizado.getCodigo()).isEqualTo(estiloSalvo.getCodigo());
		assertThat(estiloAtualizado.getNome()).isEqualTo("American IPA");
	}

	@Test
	@DisplayName("Deve deletar estilo")
	void deveDeletarEstilo() {
		// Given
		Estilo estilo = criarEstilo("Stout");
		Estilo estiloSalvo = estilos.save(estilo);
		Long codigo = estiloSalvo.getCodigo();

		// When
		estilos.deleteById(codigo);

		// Then
		assertThat(estilos.findById(codigo)).isEmpty();
	}

	@Test
	@DisplayName("Deve contar estilos corretamente")
	void deveContarEstilosCorretamente() {
		// Given
		estilos.save(criarEstilo("IPA"));
		estilos.save(criarEstilo("Pilsen"));
		estilos.save(criarEstilo("Lager"));
		estilos.save(criarEstilo("Stout"));

		// When
		long total = estilos.count();

		// Then
		assertThat(total).isEqualTo(4);
	}

	@Test
	@DisplayName("Deve verificar se estilo existe por código")
	void deveVerificarSeEstiloExiste() {
		// Given
		Estilo estilo = criarEstilo("Porter");
		Estilo estiloSalvo = estilos.save(estilo);

		// When
		boolean existe = estilos.existsById(estiloSalvo.getCodigo());
		boolean naoExiste = estilos.existsById(99999L);

		// Then
		assertThat(existe).isTrue();
		assertThat(naoExiste).isFalse();
	}

	// Helper method
	private Estilo criarEstilo(String nome) {
		Estilo estilo = new Estilo();
		estilo.setNome(nome);
		return estilo;
	}
}