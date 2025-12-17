package com.algaworks.brewer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
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

import com.algaworks.brewer.model.Grupo;
import com.algaworks.brewer.model.Usuario;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@EnableAutoConfiguration(exclude = {
	SecurityAutoConfiguration.class,
	UserDetailsServiceAutoConfiguration.class
})
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("Testes de Integração - UsuariosRepository")
class UsuariosIntegrationTest {

	@Autowired
	private Usuarios usuarios;

	@Autowired
	private TestEntityManager entityManager;

	private Grupo grupoAdmin;
	private Grupo grupoVendedor;

	@BeforeEach
	void setUp() {
		// Criar grupos de teste
		grupoAdmin = new Grupo();
		grupoAdmin.setNome("Administrador");
		grupoAdmin = entityManager.persistAndFlush(grupoAdmin);

		grupoVendedor = new Grupo();
		grupoVendedor.setNome("Vendedor");
		grupoVendedor = entityManager.persistAndFlush(grupoVendedor);
	}

	@Test
	@DisplayName("Deve salvar um usuário com sucesso")
	void deveSalvarUsuarioComSucesso() {
		// Given
		Usuario usuario = criarUsuario("João Silva", "joao@email.com", "senha123", true,
				LocalDate.of(1990, 5, 15), Arrays.asList(grupoVendedor));

		// When
		Usuario usuarioSalvo = usuarios.save(usuario);

		// Then
		assertThat(usuarioSalvo).isNotNull();
		assertThat(usuarioSalvo.getCodigo()).isNotNull();
		assertThat(usuarioSalvo.getNome()).isEqualTo("João Silva");
		assertThat(usuarioSalvo.getEmail()).isEqualTo("joao@email.com");
		assertThat(usuarioSalvo.getAtivo()).isTrue();
		assertThat(usuarioSalvo.getGrupos()).hasSize(1);
	}

	@Test
	@DisplayName("Deve buscar usuário por código")
	void deveBuscarUsuarioPorCodigo() {
		// Given
		Usuario usuario = criarUsuario("Maria Santos", "maria@email.com", "senha456", true,
				LocalDate.of(1985, 3, 20), Arrays.asList(grupoAdmin));
		Usuario usuarioSalvo = usuarios.save(usuario);

		// When
		Usuario usuarioEncontrado = usuarios.findById(usuarioSalvo.getCodigo()).orElse(null);

		// Then
		assertThat(usuarioEncontrado).isNotNull();
		assertThat(usuarioEncontrado.getCodigo()).isEqualTo(usuarioSalvo.getCodigo());
		assertThat(usuarioEncontrado.getNome()).isEqualTo("Maria Santos");
	}

	@Test
	@DisplayName("Deve buscar usuário por email")
	void deveBuscarUsuarioPorEmail() {
		// Given
		Usuario usuario = criarUsuario("Pedro Alves", "pedro@email.com", "senha789", true,
				LocalDate.of(1988, 8, 10), Arrays.asList(grupoVendedor));
		usuarios.save(usuario);

		// When
		Usuario usuarioEncontrado = usuarios.findByEmail("pedro@email.com").orElse(null);

		// Then
		assertThat(usuarioEncontrado).isNotNull();
		assertThat(usuarioEncontrado.getEmail()).isEqualTo("pedro@email.com");
		assertThat(usuarioEncontrado.getNome()).isEqualTo("Pedro Alves");
	}

	@Test
	@DisplayName("Não deve encontrar usuário com email inexistente")
	void naoDeveEncontrarUsuarioComEmailInexistente() {
		// Given
		Usuario usuario = criarUsuario("Ana Costa", "ana@email.com", "senha321", true,
				LocalDate.of(1992, 12, 5), Arrays.asList(grupoVendedor));
		usuarios.save(usuario);

		// When
		Usuario usuarioEncontrado = usuarios.findByEmail("inexistente@email.com").orElse(null);

		// Then
		assertThat(usuarioEncontrado).isNull();
	}

	@Test
	@DisplayName("Deve buscar usuários por array de códigos")
	void deveBuscarUsuariosPorCodigosIn() {
		// Given
		Usuario usuario1 = criarUsuario("Carlos Lima", "carlos@email.com", "senha111", true,
				LocalDate.of(1987, 6, 25), Arrays.asList(grupoAdmin));
		Usuario usuario2 = criarUsuario("Fernanda Rocha", "fernanda@email.com", "senha222", true,
				LocalDate.of(1991, 9, 18), Arrays.asList(grupoVendedor));
		Usuario usuario3 = criarUsuario("Ricardo Martins", "ricardo@email.com", "senha333", true,
				LocalDate.of(1989, 4, 12), Arrays.asList(grupoVendedor));

		Usuario u1Salvo = usuarios.save(usuario1);
		Usuario u2Salvo = usuarios.save(usuario2);
		usuarios.save(usuario3);

		// When
		Long[] codigos = { u1Salvo.getCodigo(), u2Salvo.getCodigo() };
		List<Usuario> usuariosEncontrados = usuarios.findByCodigoIn(codigos);

		// Then
		assertThat(usuariosEncontrados).hasSize(2);
		assertThat(usuariosEncontrados)
				.extracting(Usuario::getNome)
				.containsExactlyInAnyOrder("Carlos Lima", "Fernanda Rocha");
	}

	@Test
	@DisplayName("Deve listar todos os usuários")
	void deveListarTodosUsuarios() {
		// Given
		usuarios.save(criarUsuario("User1", "user1@email.com", "senha1", true,
				LocalDate.of(1990, 1, 1), Arrays.asList(grupoVendedor)));
		usuarios.save(criarUsuario("User2", "user2@email.com", "senha2", true,
				LocalDate.of(1990, 2, 2), Arrays.asList(grupoAdmin)));
		usuarios.save(criarUsuario("User3", "user3@email.com", "senha3", false,
				LocalDate.of(1990, 3, 3), Arrays.asList(grupoVendedor)));

		// When
		Iterable<Usuario> todosUsuarios = usuarios.findAll();

		// Then
		assertThat(todosUsuarios).hasSize(3);
	}

	@Test
	@DisplayName("Deve salvar usuário com múltiplos grupos")
	void deveSalvarUsuarioComMultiplosGrupos() {
		// Given
		Usuario usuario = criarUsuario("Super Admin", "superadmin@email.com", "senha999", true,
				LocalDate.of(1985, 1, 1), Arrays.asList(grupoAdmin, grupoVendedor));

		// When
		Usuario usuarioSalvo = usuarios.save(usuario);

		// Then
		assertThat(usuarioSalvo.getGrupos()).hasSize(2);
		assertThat(usuarioSalvo.getGrupos())
				.extracting(Grupo::getNome)
				.containsExactlyInAnyOrder("Administrador", "Vendedor");
	}

	@Test
	@DisplayName("Deve atualizar usuário existente")
	void deveAtualizarUsuarioExistente() {
		// Given
		Usuario usuario = criarUsuario("Paulo Henrique", "paulo@email.com", "senha555", true,
				LocalDate.of(1993, 7, 22), Arrays.asList(grupoVendedor));
		Usuario usuarioSalvo = usuarios.save(usuario);
		entityManager.flush();
		entityManager.clear();

		// When - Buscar o usuário salvo para evitar problemas com lista imutável
		Usuario usuarioParaAtualizar = usuarios.findById(usuarioSalvo.getCodigo()).orElseThrow();
		usuarioParaAtualizar.setNome("Paulo Henrique Silva");
		usuarioParaAtualizar.setAtivo(false);
		Usuario usuarioAtualizado = usuarios.save(usuarioParaAtualizar);

		// Then
		assertThat(usuarioAtualizado.getCodigo()).isEqualTo(usuarioSalvo.getCodigo());
		assertThat(usuarioAtualizado.getNome()).isEqualTo("Paulo Henrique Silva");
		assertThat(usuarioAtualizado.getAtivo()).isFalse();
	}

	@Test
	@DisplayName("Deve deletar usuário")
	void deveDeletarUsuario() {
		// Given
		Usuario usuario = criarUsuario("Temporário", "temp@email.com", "senha000", true,
				LocalDate.of(1995, 11, 30), Arrays.asList(grupoVendedor));
		Usuario usuarioSalvo = usuarios.save(usuario);
		Long codigo = usuarioSalvo.getCodigo();

		// When
		usuarios.deleteById(codigo);

		// Then
		assertThat(usuarios.findById(codigo)).isEmpty();
	}

	@Test
	@DisplayName("Deve verificar se usuário é novo")
	void deveVerificarSeUsuarioEhNovo() {
		// Given
		Usuario usuarioNovo = criarUsuario("Novo User", "novo@email.com", "senha", true,
				LocalDate.now(), Arrays.asList(grupoVendedor));

		// When
		boolean ehNovoAntesDeSalvar = usuarioNovo.isNovo();
		Usuario usuarioSalvo = usuarios.save(usuarioNovo);
		boolean naoEhNovoDepoisDeSalvar = usuarioSalvo.isNovo();

		// Then
		assertThat(ehNovoAntesDeSalvar).isTrue();
		assertThat(naoEhNovoDepoisDeSalvar).isFalse();
	}

	// Helper method
	private Usuario criarUsuario(String nome, String email, String senha, Boolean ativo,
			LocalDate dataNascimento, List<Grupo> grupos) {
		Usuario usuario = new Usuario();
		usuario.setNome(nome);
		usuario.setEmail(email);
		usuario.setSenha(senha);
		usuario.setConfirmacaoSenha(senha);
		usuario.setAtivo(ativo);
		usuario.setDataNascimento(dataNascimento);
		usuario.setGrupos(grupos);
		return usuario;
	}
}