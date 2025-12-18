package com.algaworks.brewer.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.algaworks.brewer.storage.FotoStorage;
import com.algaworks.brewer.storage.local.FotoStorageLocal;

/**
 * Configuração de Storage para fotos de cervejas.
 *
 * Usa FotoStorageLocal como implementação padrão, mas permite
 * que outras implementações sejam providas (ex: cloud storage).
 */
@Configuration
public class StorageConfig {

	@Bean
	@ConditionalOnMissingBean
	public FotoStorage fotoStorage() {
		return new FotoStorageLocal();
	}

}