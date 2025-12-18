package com.algaworks.brewer.controller;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import com.algaworks.brewer.dto.FotoDTO;
import com.algaworks.brewer.storage.FotoStorage;
import com.algaworks.brewer.storage.FotoStorageRunnable;

@RestController
@RequestMapping("/fotos")
public class FotosController {

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
		try {
			return fotoStorage.recuperarFotoTemporaria(nome);
		} catch (Exception e) {
			return getImagemPadrao();
		}
	}

	@GetMapping("/{nome:.*}")
	public byte[] recuperar(@PathVariable String nome) {
		try {
			return fotoStorage.recuperar(nome);
		} catch (Exception e) {
			return getImagemPadrao();
		}
	}

	private byte[] getImagemPadrao() {
		try {
			ClassPathResource resource = new ClassPathResource("static/images/logo-gray.png");
			InputStream inputStream = resource.getInputStream();
			return inputStream.readAllBytes();
		} catch (IOException e) {
			return new byte[0];
		}
	}

}
