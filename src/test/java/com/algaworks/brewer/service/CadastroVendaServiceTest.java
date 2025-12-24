package com.algaworks.brewer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.algaworks.brewer.model.Grupo;
import com.algaworks.brewer.model.Permissao;
import com.algaworks.brewer.model.StatusVenda;
import com.algaworks.brewer.model.Usuario;
import com.algaworks.brewer.model.Venda;
import com.algaworks.brewer.repository.Vendas;
import com.algaworks.brewer.security.Permissoes;
import com.algaworks.brewer.service.exception.ImpossivelEmitirVendaException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - CadastroVendaService")
class CadastroVendaServiceTest {

	@Mock
	private Vendas vendas;

	@InjectMocks
	private CadastroVendaService service;

	private Usuario usuarioProprietario;
	private Usuario usuarioComPermissao;
	private Usuario usuarioSemPermissao;
	private Venda venda;

	@BeforeEach
	void setUp() {
		// Criar usuário proprietário da venda
		usuarioProprietario = new Usuario();
		usuarioProprietario.setCodigo(1L);
		usuarioProprietario.setNome("Proprietário");
		usuarioProprietario.setGrupos(new ArrayList<>());

		// Criar usuário com permissão EMITIR_VENDA
		usuarioComPermissao = new Usuario();
		usuarioComPermissao.setCodigo(2L);
		usuarioComPermissao.setNome("Usuario Com Permissão");

		Grupo grupoAdmin = new Grupo();
		grupoAdmin.setCodigo(1L);
		grupoAdmin.setNome("Administrador");

		Permissao permissaoEmitir = new Permissao();
		permissaoEmitir.setCodigo(1L);
		permissaoEmitir.setNome(Permissoes.EMITIR_VENDA);

		List<Permissao> permissoes = new ArrayList<>();
		permissoes.add(permissaoEmitir);
		grupoAdmin.setPermissoes(permissoes);

		List<Grupo> grupos = new ArrayList<>();
		grupos.add(grupoAdmin);
		usuarioComPermissao.setGrupos(grupos);

		// Criar usuário sem permissão
		usuarioSemPermissao = new Usuario();
		usuarioSemPermissao.setCodigo(3L);
		usuarioSemPermissao.setNome("Usuario Sem Permissão");
		usuarioSemPermissao.setGrupos(new ArrayList<>());

		// Criar venda
		venda = new Venda();
		venda.setCodigo(1L);
		venda.setStatus(StatusVenda.ORCAMENTO);
		venda.setUsuario(usuarioProprietario);
		venda.setValorTotal(BigDecimal.valueOf(100.00));
	}

	@Test
	@DisplayName("Deve emitir venda quando usuário é proprietário")
	void deveEmitirVendaQuandoUsuarioEhProprietario() {
		// Given
		when(vendas.findById(1L)).thenReturn(Optional.of(venda));
		when(vendas.save(any(Venda.class))).thenReturn(venda);

		// When
		service.emitir(venda, usuarioProprietario);

		// Then
		assertThat(venda.getStatus()).isEqualTo(StatusVenda.EMITIDA);
		verify(vendas).save(venda);
	}

	@Test
	@DisplayName("Deve emitir venda quando usuário tem permissão EMITIR_VENDA")
	void deveEmitirVendaQuandoUsuarioTemPermissao() {
		// Given
		when(vendas.findById(1L)).thenReturn(Optional.of(venda));
		when(vendas.save(any(Venda.class))).thenReturn(venda);

		// When
		service.emitir(venda, usuarioComPermissao);

		// Then
		assertThat(venda.getStatus()).isEqualTo(StatusVenda.EMITIDA);
		verify(vendas).save(venda);
	}

	@Test
	@DisplayName("Não deve emitir venda quando usuário não tem permissão")
	void naoDeveEmitirVendaQuandoUsuarioNaoTemPermissao() {
		// Given
		when(vendas.findById(1L)).thenReturn(Optional.of(venda));

		// When & Then
		assertThatThrownBy(() -> service.emitir(venda, usuarioSemPermissao))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage("Você não tem permissão para emitir esta venda");

		verify(vendas, never()).save(any(Venda.class));
	}

	@Test
	@DisplayName("Não deve emitir venda cancelada")
	void naoDeveEmitirVendaCancelada() {
		// Given
		venda.setStatus(StatusVenda.CANCELADA);
		when(vendas.findById(1L)).thenReturn(Optional.of(venda));

		// When & Then
		assertThatThrownBy(() -> service.emitir(venda, usuarioProprietario))
			.isInstanceOf(ImpossivelEmitirVendaException.class)
			.hasMessage("Não é possível emitir uma venda cancelada");

		verify(vendas, never()).save(any(Venda.class));
	}

	@Test
	@DisplayName("Deve lançar exceção quando venda não encontrada ao emitir")
	void deveLancarExcecaoQuandoVendaNaoEncontradaAoEmitir() {
		// Given
		when(vendas.findById(1L)).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> service.emitir(venda, usuarioProprietario))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Venda não encontrada");

		verify(vendas, never()).save(any(Venda.class));
	}

	@Test
	@DisplayName("Deve cancelar venda quando usuário é proprietário")
	void deveCancelarVendaQuandoUsuarioEhProprietario() {
		// Given
		venda.setStatus(StatusVenda.EMITIDA);
		when(vendas.findById(1L)).thenReturn(Optional.of(venda));
		when(vendas.save(any(Venda.class))).thenReturn(venda);

		// When
		service.cancelar(venda, usuarioProprietario);

		// Then
		assertThat(venda.getStatus()).isEqualTo(StatusVenda.CANCELADA);
		verify(vendas).save(venda);
	}

	@Test
	@DisplayName("Deve cancelar venda quando usuário tem permissão CANCELAR_VENDA")
	void deveCancelarVendaQuandoUsuarioTemPermissao() {
		// Given
		venda.setStatus(StatusVenda.EMITIDA);

		// Criar usuário com permissão CANCELAR_VENDA
		Usuario usuarioComPermissaoCancelar = new Usuario();
		usuarioComPermissaoCancelar.setCodigo(4L);

		Grupo grupo = new Grupo();
		Permissao permissao = new Permissao();
		permissao.setNome(Permissoes.CANCELAR_VENDA);

		List<Permissao> permissoes = new ArrayList<>();
		permissoes.add(permissao);
		grupo.setPermissoes(permissoes);

		List<Grupo> grupos = new ArrayList<>();
		grupos.add(grupo);
		usuarioComPermissaoCancelar.setGrupos(grupos);

		when(vendas.findById(1L)).thenReturn(Optional.of(venda));
		when(vendas.save(any(Venda.class))).thenReturn(venda);

		// When
		service.cancelar(venda, usuarioComPermissaoCancelar);

		// Then
		assertThat(venda.getStatus()).isEqualTo(StatusVenda.CANCELADA);
		verify(vendas).save(venda);
	}

	@Test
	@DisplayName("Não deve cancelar venda quando usuário não tem permissão")
	void naoDeveCancelarVendaQuandoUsuarioNaoTemPermissao() {
		// Given
		venda.setStatus(StatusVenda.EMITIDA);
		when(vendas.findById(1L)).thenReturn(Optional.of(venda));

		// When & Then
		assertThatThrownBy(() -> service.cancelar(venda, usuarioSemPermissao))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage("Você não tem permissão para cancelar esta venda");

		verify(vendas, never()).save(any(Venda.class));
	}

	@Test
	@DisplayName("Não deve cancelar venda que não está emitida")
	void naoDeveCancelarVendaQueNaoEstaEmitida() {
		// Given
		venda.setStatus(StatusVenda.ORCAMENTO);
		when(vendas.findById(1L)).thenReturn(Optional.of(venda));

		// When
		service.cancelar(venda, usuarioProprietario);

		// Then
		assertThat(venda.getStatus()).isEqualTo(StatusVenda.ORCAMENTO);
		verify(vendas, never()).save(any(Venda.class));
	}

	@Test
	@DisplayName("Deve lançar exceção quando venda não encontrada ao cancelar")
	void deveLancarExcecaoQuandoVendaNaoEncontradaAoCancelar() {
		// Given
		when(vendas.findById(1L)).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> service.cancelar(venda, usuarioProprietario))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Venda não encontrada");

		verify(vendas, never()).save(any(Venda.class));
	}
}
