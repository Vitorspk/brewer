package com.algaworks.brewer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.algaworks.brewer.dto.FotoDTO;
import com.algaworks.brewer.service.FotoUploadService;
import com.algaworks.brewer.storage.FotoStorage;

@RestController
@RequestMapping("/fotos")
public class FotosController {

	private static final Logger logger = LoggerFactory.getLogger(FotosController.class);

	@Autowired
	private FotoStorage fotoStorage;

	@Autowired
	private FotoUploadService fotoUploadService;
	
	@PostMapping
	public DeferredResult<FotoDTO> upload(@RequestParam("files[]") MultipartFile[] files) {
		DeferredResult<FotoDTO> resultado = new DeferredResult<>();
		fotoUploadService.uploadAsync(files, resultado);
		return resultado;
	}
	
	@GetMapping("/{nome:.*}")
	public byte[] recuperar(@PathVariable String nome) {
		validateFileName(nome);
		return fotoStorage.recuperar(nome);
	}

	private void validateFileName(String nome) {
		if (nome == null || nome.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name cannot be empty");
		}
		if (nome.contains("..") || nome.contains("/") || nome.contains("\\")) {
			logger.warn("Attempted path traversal attack detected: {}", nome);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
		}
	}
	
}
