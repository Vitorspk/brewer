package com.algaworks.brewer.mail;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.algaworks.brewer.model.Venda;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Component
public class Mailer {

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private TemplateEngine thymeleaf;

	@Value("${brewer.mail.from}")
	private String from;

	@Value("${brewer.mail.logo-url}")
	private String logo;

	@Async
	public void enviar(Venda venda) {
		Context context = new Context(new Locale("pt", "BR"));

		Map<String, Object> variaveis = new HashMap<>();
		variaveis.put("venda", venda);
		variaveis.put("logo", logo);
		context.setVariables(variaveis);

		String mensagem = thymeleaf.process("mail/ResumoVenda", context);

		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
			helper.setFrom(from);
			helper.setTo(venda.getCliente().getEmail());
			helper.setSubject(String.format("Brewer - Venda nº %d", venda.getCodigo()));
			helper.setText(mensagem, true);

			mailSender.send(mimeMessage);
		} catch (MessagingException e) {
			throw new RuntimeException("Erro ao enviar e-mail", e);
		}
	}
}