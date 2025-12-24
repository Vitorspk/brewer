package com.algaworks.brewer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.algaworks.brewer.model.Cliente;
import com.algaworks.brewer.model.TipoPessoa;
import com.algaworks.brewer.repository.Clientes;
import com.algaworks.brewer.service.exception.CpfCnpjClienteJaCadastradoException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes - CadastroClienteService")
class CadastroClienteServiceTest {

	@Mock
	private Clientes clientes;

	@InjectMocks
	private CadastroClienteService service;

	private Cliente clienteNovo;
	private Cliente clienteExistente;

	@BeforeEach
	void setUp() {
		// Cliente novo (sem código)
		clienteNovo = new Cliente();
		clienteNovo.setNome("Cliente Novo");
		clienteNovo.setTipoPessoa(TipoPessoa.FISICA);
		clienteNovo.setCpfOuCnpj("11144477735");
		clienteNovo.setEmail("novo@teste.com");

		// Cliente existente no banco (com código)
		clienteExistente = new Cliente();
		clienteExistente.setCodigo(1L);
		clienteExistente.setNome("Cliente Existente");
		clienteExistente.setTipoPessoa(TipoPessoa.FISICA);
		clienteExistente.setCpfOuCnpj("11144477735");
		clienteExistente.setEmail("existente@teste.com");
	}

	@Test
	@DisplayName("Deve salvar novo cliente com CPF único")
	void deveSalvarNovoClienteComCpfUnico() {
		// Given
		when(clientes.findByCpfOuCnpj("11144477735")).thenReturn(Optional.empty());

		// When
		service.salvar(clienteNovo);

		// Then
		verify(clientes).save(clienteNovo);
	}

	@Test
	@DisplayName("Deve lançar exceção ao tentar cadastrar CPF duplicado")
	void deveLancarExcecaoAoTentarCadastrarCpfDuplicado() {
		// Given - CPF já existe no banco
		when(clientes.findByCpfOuCnpj("11144477735")).thenReturn(Optional.of(clienteExistente));

		// When & Then
		assertThatThrownBy(() -> service.salvar(clienteNovo))
			.isInstanceOf(CpfCnpjClienteJaCadastradoException.class)
			.hasMessage("CPF/CNPJ já cadastrado");

		verify(clientes, never()).save(any());
	}

	@Test
	@DisplayName("Deve permitir editar cliente mantendo o mesmo CPF")
	void devePermitirEditarClienteMantendoMesmoCpf() {
		// Given - Editando cliente existente sem mudar CPF
		Cliente clienteEditado = new Cliente();
		clienteEditado.setCodigo(1L); // Mesmo código
		clienteEditado.setNome("Nome Atualizado");
		clienteEditado.setTipoPessoa(TipoPessoa.FISICA);
		clienteEditado.setCpfOuCnpj("11144477735"); // Mesmo CPF
		clienteEditado.setEmail("atualizado@teste.com");

		when(clientes.findByCpfOuCnpj("11144477735")).thenReturn(Optional.of(clienteExistente));

		// When
		service.salvar(clienteEditado);

		// Then - Deve salvar normalmente
		verify(clientes).save(clienteEditado);
	}

	@Test
	@DisplayName("Deve lançar exceção ao tentar editar cliente com CPF de outro cliente")
	void deveLancarExcecaoAoTentarEditarClienteComCpfDeOutroCliente() {
		// Given - Tentando editar cliente 2 com CPF do cliente 1
		Cliente outroClienteExistente = new Cliente();
		outroClienteExistente.setCodigo(2L); // Cliente diferente
		outroClienteExistente.setNome("Outro Cliente");
		outroClienteExistente.setTipoPessoa(TipoPessoa.FISICA);
		outroClienteExistente.setCpfOuCnpj("22233344456");
		outroClienteExistente.setEmail("outro@teste.com");

		// Tentar mudar CPF para um que já existe
		Cliente clienteComCpfDuplicado = new Cliente();
		clienteComCpfDuplicado.setCodigo(2L);
		clienteComCpfDuplicado.setNome("Outro Cliente");
		clienteComCpfDuplicado.setTipoPessoa(TipoPessoa.FISICA);
		clienteComCpfDuplicado.setCpfOuCnpj("11144477735"); // CPF do cliente 1
		clienteComCpfDuplicado.setEmail("outro@teste.com");

		when(clientes.findByCpfOuCnpj("11144477735")).thenReturn(Optional.of(clienteExistente));

		// When & Then
		assertThatThrownBy(() -> service.salvar(clienteComCpfDuplicado))
			.isInstanceOf(CpfCnpjClienteJaCadastradoException.class)
			.hasMessage("CPF/CNPJ já cadastrado");

		verify(clientes, never()).save(any());
	}

	@Test
	@DisplayName("Deve permitir salvar cliente CNPJ único")
	void devePermitirSalvarClienteCnpjUnico() {
		// Given
		Cliente clientePJ = new Cliente();
		clientePJ.setNome("Empresa Teste");
		clientePJ.setTipoPessoa(TipoPessoa.JURIDICA);
		clientePJ.setCpfOuCnpj("47960950000121");
		clientePJ.setEmail("empresa@teste.com");

		when(clientes.findByCpfOuCnpj("47960950000121")).thenReturn(Optional.empty());

		// When
		service.salvar(clientePJ);

		// Then
		verify(clientes).save(clientePJ);
	}

	@Test
	@DisplayName("Deve lançar exceção ao tentar cadastrar CNPJ duplicado")
	void deveLancarExcecaoAoTentarCadastrarCnpjDuplicado() {
		// Given - CNPJ já existe
		Cliente empresaExistente = new Cliente();
		empresaExistente.setCodigo(10L);
		empresaExistente.setNome("Empresa Existente");
		empresaExistente.setTipoPessoa(TipoPessoa.JURIDICA);
		empresaExistente.setCpfOuCnpj("47960950000121");
		empresaExistente.setEmail("empresa1@teste.com");

		Cliente novaEmpresa = new Cliente();
		novaEmpresa.setNome("Nova Empresa");
		novaEmpresa.setTipoPessoa(TipoPessoa.JURIDICA);
		novaEmpresa.setCpfOuCnpj("47960950000121"); // Mesmo CNPJ
		novaEmpresa.setEmail("empresa2@teste.com");

		when(clientes.findByCpfOuCnpj("47960950000121")).thenReturn(Optional.of(empresaExistente));

		// When & Then
		assertThatThrownBy(() -> service.salvar(novaEmpresa))
			.isInstanceOf(CpfCnpjClienteJaCadastradoException.class)
			.hasMessage("CPF/CNPJ já cadastrado");

		verify(clientes, never()).save(any());
	}
}