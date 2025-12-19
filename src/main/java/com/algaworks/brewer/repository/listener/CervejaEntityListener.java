package com.algaworks.brewer.repository.listener;

import jakarta.persistence.PostLoad;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.algaworks.brewer.model.Cerveja;
import com.algaworks.brewer.storage.FotoStorage;

@Component
public class CervejaEntityListener implements ApplicationContextAware {

	private static ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		applicationContext = context;
	}

	@PostLoad
	public void postLoad(final Cerveja cerveja) {
		if (applicationContext != null) {
			FotoStorage fotoStorage = applicationContext.getBean(FotoStorage.class);

			cerveja.setUrlFoto(fotoStorage.getUrl(cerveja.getFotoOuMock()));
			cerveja.setUrlThumbnailFoto(fotoStorage.getUrl(FotoStorage.THUMBNAIL_PREFIX + cerveja.getFotoOuMock()));
		}
	}

}
