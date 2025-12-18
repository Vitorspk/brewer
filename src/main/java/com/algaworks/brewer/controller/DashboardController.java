package com.algaworks.brewer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.algaworks.brewer.repository.Cervejas;
import com.algaworks.brewer.repository.Clientes;
import com.algaworks.brewer.repository.Estilos;

@Controller
public class DashboardController {

	@Autowired
	private Cervejas cervejas;

	@Autowired
	private Clientes clientes;

	@Autowired
	private Estilos estilos;

	@GetMapping("/dashboard")
	public ModelAndView dashboard() {
		ModelAndView mv = new ModelAndView("Dashboard");

		// Estatísticas básicas
		mv.addObject("totalCervejas", cervejas.count());
		mv.addObject("totalClientes", clientes.count());
		mv.addObject("totalEstilos", estilos.count());

		return mv;
	}

}