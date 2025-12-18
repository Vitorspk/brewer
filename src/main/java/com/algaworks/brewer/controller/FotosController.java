package com.algaworks.brewer.controller;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
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
import com.algaworks.brewer.storage.FotoStorage;
import com.algaworks.brewer.storage.FotoStorageRunnable;

@RestController
@RequestMapping("/fotos")
public class FotosController {

	private static final Logger logger = LoggerFactory.getLogger(FotosController.class);
	private static final String DEFAULT_IMAGE_PATH = "static/images/logo-gray.png";

	@Autowired
	private FotoStorage fotoStorage;

	@PostMapping
	public DeferredResult<FotoDTO> upload(@RequestParam("files[]") MultipartFile[] files) {
		DeferredResult<FotoDTO> resultado = new DeferredResult<>();

		Thread thread = new Thread(new FotoStorageRunnable(files, resultado, fotoStorage));
		thread.start();

		return resultado;
	}

	@GetMapping("/temp/{nome:.*}")
	public byte[] recuperarFotoTemporaria(@PathVariable String nome) {
		validateFileName(nome);
		try {
			return fotoStorage.recuperarFotoTemporaria(nome);
		} catch (Exception e) {
			logger.warn("Failed to load temporary photo '{}': {}", nome, e.getMessage());
			return getImagemPadrao();
		}
	}

	@GetMapping("/{nome:.*}")
	public byte[] recuperar(@PathVariable String nome) {
		validateFileName(nome);
		try {
			return fotoStorage.recuperar(nome);
		} catch (Exception e) {
			logger.warn("Failed to load photo '{}': {}", nome, e.getMessage());
			return getImagemPadrao();
		}
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

	private byte[] getImagemPadrao() {
		try {
			ClassPathResource resource = new ClassPathResource(DEFAULT_IMAGE_PATH);
			try (InputStream inputStream = resource.getInputStream()) {
				return inputStream.readAllBytes();
			}
		} catch (IOException e) {
			logger.error("Failed to load default image from '{}': {}", DEFAULT_IMAGE_PATH, e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to load image");
		}
	}

}
