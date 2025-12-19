package com.algaworks.brewer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import com.algaworks.brewer.dto.FotoDTO;
import com.algaworks.brewer.storage.FotoStorage;

@Service
public class FotoUploadService {

	private static final Logger logger = LoggerFactory.getLogger(FotoUploadService.class);

	@Autowired
	private FotoStorage fotoStorage;

	@Async("taskExecutor")
	public void uploadAsync(MultipartFile[] files, DeferredResult<FotoDTO> resultado) {
		try {
			String nomeFoto = fotoStorage.salvar(files);
			String contentType = files[0].getContentType();
			resultado.setResult(new FotoDTO(nomeFoto, contentType, fotoStorage.getUrl(nomeFoto)));
		} catch (Exception e) {
			logger.error("Erro ao fazer upload de foto", e);
			resultado.setErrorResult(e);
		}
	}

}
