package com.algaworks.brewer.controller.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.algaworks.brewer.service.exception.EstiloNaoEncontradoException;
import com.algaworks.brewer.service.exception.NomeEstiloJaCadastradoException;

@ControllerAdvice
public class ControllerAdviceExceptionHandler {

	@ExceptionHandler(NomeEstiloJaCadastradoException.class)
	public ResponseEntity<String> handleNomeEstiloJaCadastradoException(NomeEstiloJaCadastradoException e) {
		return ResponseEntity.badRequest().body(e.getMessage());
	}

	@ExceptionHandler(EstiloNaoEncontradoException.class)
	public ResponseEntity<String> handleEstiloNaoEncontradoException(EstiloNaoEncontradoException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
	}

}
