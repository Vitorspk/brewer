package com.algaworks.brewer.security;

/**
 * Constantes para nomes de permissões do sistema.
 * Centraliza as strings de permissões para evitar erros de digitação
 * e facilitar manutenção.
 */
public final class Permissoes {

	private Permissoes() {
		// Classe utilitária, não deve ser instanciada
	}

	public static final String EMITIR_VENDA = "ROLE_EMITIR_VENDA";
	public static final String CANCELAR_VENDA = "ROLE_CANCELAR_VENDA";
}
