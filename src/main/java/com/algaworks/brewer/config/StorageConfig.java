package com.algaworks.brewer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.algaworks.brewer.storage.FotoStorage;
import com.algaworks.brewer.storage.local.FotoStorageLocal;

/**
 * Configuração de Storage para fotos de cervejas.
 */
@Configuration
public class StorageConfig {

	@Bean
	public FotoStorage fotoStorage() {
		return new FotoStorageLocal();
	}

}