package com.algaworks.brewer.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.algaworks.brewer.repository.listener.CervejaEntityListener;
import com.algaworks.brewer.storage.FotoStorage;

/**
 * Configuração de teste para prover beans mockados.
 *
 * Esta classe fornece implementações mockadas de beans que são necessários
 * durante a execução de testes, mas que dependem de recursos externos
 * (como storage de arquivos) que não estão disponíveis no ambiente de teste.
 */
@TestConfiguration
public class TestConfig {

	/**
	 * Fornece um mock do FotoStorage para testes.
	 *
	 * O bean FotoStorage é usado pela entidade Cerveja através do CervejaEntityListener
	 * para enriquecer URLs de fotos. Durante os testes, não queremos depender de
	 * storage real (local ou S3), então fornecemos um mock.
	 *
	 * @Primary garante que este bean tem precedência sobre implementações reais
	 * quando múltiplos candidatos estão disponíveis.
	 */
	@Bean
	@Primary
	public FotoStorage fotoStorage() {
		FotoStorage mock = mock(FotoStorage.class);
		// Configurar o mock para retornar URLs de teste quando chamado
		when(mock.getUrl(anyString())).thenAnswer(invocation -> {
			String nomeArquivo = invocation.getArgument(0);
			return "http://localhost/fotos/" + nomeArquivo;
		});
		return mock;
	}

	/**
	 * Fornece o CervejaEntityListener para testes.
	 *
	 * O listener precisa ser um bean gerenciado pelo Spring para ter acesso
	 * ao ApplicationContext e assim poder buscar o bean FotoStorage.
	 * Em @DataJpaTest, componentes não são automaticamente escaneados,
	 * então precisamos declará-lo explicitamente aqui.
	 */
	@Bean
	public CervejaEntityListener cervejaEntityListener() {
		return new CervejaEntityListener();
	}
}
