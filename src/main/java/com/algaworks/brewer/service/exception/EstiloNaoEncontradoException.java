package com.algaworks.brewer.service.exception;

public class EstiloNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EstiloNaoEncontradoException(String mensagem) {
		super(mensagem);
	}

	public EstiloNaoEncontradoException(Long codigo) {
		this(String.format("Estilo com código %d não encontrado", codigo));
	}

}
