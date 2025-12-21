package com.algaworks.brewer.config;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeansException;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.number.NumberStyleFormatter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import com.algaworks.brewer.controller.converter.BigDecimalConverter;
import com.algaworks.brewer.controller.converter.CidadeConverter;
import com.algaworks.brewer.controller.converter.EstadoConverter;
import com.algaworks.brewer.controller.converter.EstiloConverter;
import com.algaworks.brewer.controller.converter.GrupoConverter;
import com.algaworks.brewer.thymeleaf.BrewerDialect;
import com.github.mxab.thymeleaf.extras.dataattribute.dialect.DataAttributeDialect;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

/**
 * Configuração Web do Spring MVC.
 *
 * FASE 3: Simplificado para Spring Boot 2.7
 * - @EnableWebMvc REMOVIDO (Spring Boot auto-configura)
 * - @ComponentScan REMOVIDO (Spring Boot escaneia automaticamente)
 * - WebMvcConfigurerAdapter → WebMvcConfigurer (interface desde Spring 5)
 * - Thymeleaf parcialmente auto-configurado, mas mantemos customizações de dialects
 * - MessageSource auto-configurado via application.properties (removido)
 * - Mantém apenas customizações específicas da aplicação
 */
@Configuration
@EnableCaching
public class WebConfig implements WebMvcConfigurer, ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * Configura Thymeleaf Template Engine com dialects customizados.
	 * Spring Boot auto-configura Thymeleaf, mas precisamos adicionar dialects customizados.
	 */
	@Bean
	public SpringTemplateEngine templateEngine() {
		SpringTemplateEngine engine = new SpringTemplateEngine();
		engine.setEnableSpringELCompiler(true);
		engine.setTemplateResolver(templateResolver());

		// Dialects customizados
		engine.addDialect(new LayoutDialect());
		engine.addDialect(new BrewerDialect());
		engine.addDialect(new DataAttributeDialect());
		engine.addDialect(new SpringSecurityDialect());
		return engine;
	}

	private SpringResourceTemplateResolver templateResolver() {
		SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
		resolver.setApplicationContext(applicationContext);
		resolver.setPrefix("classpath:/templates/");
		resolver.setSuffix(".html");
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setCacheable(false); // Desabilita cache em desenvolvimento
		return resolver;
	}

	/**
	 * Configura handlers para recursos estáticos.
	 * Spring Boot já serve /static automaticamente, mas mantemos configuração
	 * para path /fotos/ que aponta para diretório externo.
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// Fotos de cervejas (diretório externo)
		registry.addResourceHandler("/fotos/**")
				.addResourceLocations("file:///tmp/brewer/fotos/");
	}

	/**
	 * Configura conversores e formatters customizados.
	 * Spring Boot auto-registra este bean se existir.
	 */
	@Bean
	public FormattingConversionService mvcConversionService() {
		DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();

		// Conversores de entidades
		conversionService.addConverter(new EstiloConverter());
		conversionService.addConverter(new CidadeConverter());
		conversionService.addConverter(new EstadoConverter());
		conversionService.addConverter(new GrupoConverter());

		// Conversor de BigDecimal que aceita formato brasileiro (vírgula como decimal)
		conversionService.addConverter(new BigDecimalConverter());

		// Formatters de números
		// NOTA: O BigDecimalConverter cuida da conversão String -> BigDecimal
		// O NumberStyleFormatter abaixo é usado apenas para exibição (BigDecimal -> String)
		// NumberStyleFormatter bigDecimalFormatter = new NumberStyleFormatter("#,##0.00");
		// conversionService.addFormatterForFieldType(BigDecimal.class, bigDecimalFormatter);

		NumberStyleFormatter integerFormatter = new NumberStyleFormatter("#,##0");
		conversionService.addFormatterForFieldType(Integer.class, integerFormatter);

		// Formatter de datas (Java 8+ Date/Time API)
		DateTimeFormatterRegistrar dateTimeFormatter = new DateTimeFormatterRegistrar();
		dateTimeFormatter.setDateFormatter(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		dateTimeFormatter.registerFormatters(conversionService);

		return conversionService;
	}

	/**
	 * Define locale fixo para pt_BR.
	 * Aplicação não suporta internacionalização dinâmica.
	 */
	@Bean
	public LocaleResolver localeResolver() {
		return new FixedLocaleResolver(new Locale("pt", "BR"));
	}

	/**
	 * Configura cache manager simples.
	 * FASE 3: Substituído Guava por ConcurrentMapCacheManager (Spring nativo).
	 * Cache para listas de enums (Estilo, Sabor, etc).
	 */
	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager();
	}

}