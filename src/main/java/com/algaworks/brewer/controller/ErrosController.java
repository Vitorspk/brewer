package com.algaworks.brewer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ErrosController implements ErrorController {

	private static final Logger logger = LoggerFactory.getLogger(ErrosController.class);

	@RequestMapping("/error")
	public String handleError(HttpServletRequest request) {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

		if (status != null) {
			try {
				int statusCode = Integer.parseInt(status.toString());

				if (statusCode == HttpStatus.NOT_FOUND.value()) {
					return "404";
				} else if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
					return "redirect:/login";
				} else if (statusCode == HttpStatus.FORBIDDEN.value()) {
					return "403";
				} else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
					return "500";
				}
			} catch (NumberFormatException e) {
				logger.warn("Invalid HTTP status code format: {}", status);
			}
		}

		// Fallback for unhandled error codes
		return "500";
	}

	@GetMapping("/404")
	public String paginaNaoEncontrada() {
		return "404";
	}

	@RequestMapping("/500")
	public String erroServidor() {
		return "500";
	}

}
