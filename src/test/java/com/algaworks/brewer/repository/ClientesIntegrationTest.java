package com.algaworks.brewer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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

import com.algaworks.brewer.model.Cliente;
import com.algaworks.brewer.model.Endereco;
import com.algaworks.brewer.model.TipoPessoa;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@EnableAutoConfiguration(exclude = {
	SecurityAutoConfiguration.class,
	UserDetailsServiceAutoConfiguration.class
})
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(FlywayTestConfig.class)
@DisplayName("Testes de Integração - ClientesRepository")
class ClientesIntegrationTest extends BaseRepositoryIntegrationTest {

	@Autowired
	private Clientes clientes;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("Deve salvar um cliente pessoa física com sucesso")
	void deveSalvarClientePessoaFisicaComSucesso() {
		// Given - CPF válido: 111.444.777-35
		Cliente cliente = criarClientePF("João Silva", "11144477735", "joao@email.com", "(11) 98765-4321");

		// When
		Cliente clienteSalvo = clientes.save(cliente);

		// Then
		assertThat(clienteSalvo).isNotNull();
		assertThat(clienteSalvo.getCodigo()).isNotNull();
		assertThat(clienteSalvo.getNome()).isEqualTo("João Silva");
		assertThat(clienteSalvo.getTipoPessoa()).isEqualTo(TipoPessoa.FISICA);
	}

	@Test
	@DisplayName("Deve salvar um cliente pessoa jurídica com sucesso")
	void deveSalvarClientePessoaJuridicaComSucesso() {
		// Given - CNPJ válido
		Cliente cliente = criarClientePJ("Empresa XYZ Ltda", "11222333000181", "contato@empresa.com", "(11) 3333-4444");

		// When
		Cliente clienteSalvo = clientes.save(cliente);

		// Then
		assertThat(clienteSalvo).isNotNull();
		assertThat(clienteSalvo.getCodigo()).isNotNull();
		assertThat(clienteSalvo.getNome()).isEqualTo("Empresa XYZ Ltda");
		assertThat(clienteSalvo.getTipoPessoa()).isEqualTo(TipoPessoa.JURIDICA);
	}

	@Test
	@DisplayName("Deve buscar cliente por código")
	void deveBuscarClientePorCodigo() {
		// Given - CPF válido: 222.555.888-46
		Cliente cliente = criarClientePF("Maria Santos", "22255588846", "maria@email.com", "(11) 91111-2222");
		Cliente clienteSalvo = clientes.save(cliente);

		// When
		Cliente clienteEncontrado = clientes.findById(clienteSalvo.getCodigo()).orElse(null);

		// Then
		assertThat(clienteEncontrado).isNotNull();
		assertThat(clienteEncontrado.getCodigo()).isEqualTo(clienteSalvo.getCodigo());
		assertThat(clienteEncontrado.getNome()).isEqualTo("Maria Santos");
	}

	@Test
	@DisplayName("Deve buscar cliente por CPF")
	void deveBuscarClientePorCpf() {
		// Given - CPF válido: 419.223.844-61
		Cliente cliente = criarClientePF("Pedro Alves", "41922384461", "pedro@email.com", "(11) 92222-3333");
		clientes.save(cliente);

		// When
		Cliente clienteEncontrado = clientes.findByCpfOuCnpj("41922384461").orElse(null);

		// Then
		assertThat(clienteEncontrado).isNotNull();
		assertThat(clienteEncontrado.getCpfOuCnpjSemFormatacao()).isEqualTo("41922384461");
		assertThat(clienteEncontrado.getNome()).isEqualTo("Pedro Alves");
	}

	@Test
	@DisplayName("Deve buscar clientes por nome que começa com")
	void deveBuscarClientesPorNomeIniciandoCom() {
		// Given - CPFs válidos
		clientes.save(criarClientePF("Ana Maria Costa", "53879433011", "ana@email.com", "(11) 95555-6666"));
		clientes.save(criarClientePF("Ana Paula Silva", "19232664704", "anapaula@email.com", "(11) 96666-7777"));
		clientes.save(criarClientePF("Carlos Roberto", "26560137120", "carlos@email.com", "(11) 97777-8888"));

		// When
		List<Cliente> clientesEncontrados = clientes.findByNomeStartingWithIgnoreCase("Ana");

		// Then
		assertThat(clientesEncontrados).hasSize(2);
		assertThat(clientesEncontrados)
				.extracting(Cliente::getNome)
				.allMatch(nome -> nome.toLowerCase().startsWith("ana"));
	}

	@Test
	@DisplayName("Deve listar todos os clientes")
	void deveListarTodosClientes() {
		// Given - CPFs/CNPJ válidos
		clientes.save(criarClientePF("Cliente 1", "97882456926", "cliente1@email.com", "(11) 91111-1111"));
		clientes.save(criarClientePF("Cliente 2", "88874501293", "cliente2@email.com", "(11) 92222-2222"));
		clientes.save(criarClientePJ("Empresa 1", "11222333000181", "empresa1@email.com", "(11) 93333-3333"));

		// When
		Iterable<Cliente> todosClientes = clientes.findAll();

		// Then
		assertThat(todosClientes).hasSize(3);
	}

	@Test
	@DisplayName("Deve salvar cliente com endereço completo")
	void deveSalvarClienteComEnderecoCompleto() {
		// Given - CPF válido: 529.555.748-05
		Cliente cliente = criarClientePF("Fernanda Lima", "52955574805", "fernanda@email.com", "(11) 98888-9999");
		Endereco endereco = new Endereco();
		endereco.setLogradouro("Rua das Flores");
		endereco.setNumero("123");
		endereco.setComplemento("Apto 45");
		endereco.setCep("01234-567");
		cliente.setEndereco(endereco);

		// When
		Cliente clienteSalvo = clientes.save(cliente);
		entityManager.flush();
		entityManager.clear();
		Cliente clienteRecuperado = clientes.findById(clienteSalvo.getCodigo()).orElse(null);

		// Then
		assertThat(clienteRecuperado).isNotNull();
		assertThat(clienteRecuperado.getEndereco()).isNotNull();
		assertThat(clienteRecuperado.getEndereco().getLogradouro()).isEqualTo("Rua das Flores");
		assertThat(clienteRecuperado.getEndereco().getNumero()).isEqualTo("123");
	}

	@Test
	@DisplayName("Deve atualizar cliente existente")
	void deveAtualizarClienteExistente() {
		// Given - CPF válido: 746.295.234-30
		Cliente cliente = criarClientePF("Paulo Santos", "74629523430", "paulo@email.com", "(11) 90000-1111");
		Cliente clienteSalvo = clientes.save(cliente);

		// When
		clienteSalvo.setNome("Paulo Santos Silva");
		clienteSalvo.setTelefone("(11) 90000-2222");
		Cliente clienteAtualizado = clientes.save(clienteSalvo);

		// Then
		assertThat(clienteAtualizado.getCodigo()).isEqualTo(clienteSalvo.getCodigo());
		assertThat(clienteAtualizado.getNome()).isEqualTo("Paulo Santos Silva");
		assertThat(clienteAtualizado.getTelefone()).isEqualTo("(11) 90000-2222");
	}

	@Test
	@DisplayName("Deve deletar cliente")
	void deveDeletarCliente() {
		// Given - CPF válido: 689.054.509-54
		Cliente cliente = criarClientePF("Temporário", "68905450954", "temp@email.com", "(11) 99999-9999");
		Cliente clienteSalvo = clientes.save(cliente);
		Long codigo = clienteSalvo.getCodigo();

		// When
		clientes.deleteById(codigo);

		// Then
		assertThat(clientes.findById(codigo)).isEmpty();
	}

	// Helper methods
	private Cliente criarClientePF(String nome, String cpf, String email, String telefone) {
		Cliente cliente = new Cliente();
		cliente.setNome(nome);
		cliente.setTipoPessoa(TipoPessoa.FISICA);
		cliente.setCpfOuCnpj(cpf);
		cliente.setEmail(email);
		cliente.setTelefone(telefone);
		return cliente;
	}

	private Cliente criarClientePJ(String nome, String cnpj, String email, String telefone) {
		Cliente cliente = new Cliente();
		cliente.setNome(nome);
		cliente.setTipoPessoa(TipoPessoa.JURIDICA);
		cliente.setCpfOuCnpj(cnpj);
		cliente.setEmail(email);
		cliente.setTelefone(telefone);
		return cliente;
	}
}