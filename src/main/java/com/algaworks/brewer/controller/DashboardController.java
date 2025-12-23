package com.algaworks.brewer.controller;

import com.algaworks.brewer.dto.VendaMes;
import com.algaworks.brewer.dto.VendaOrigem;
import com.algaworks.brewer.repository.Vendas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.algaworks.brewer.repository.Cervejas;
import com.algaworks.brewer.repository.Clientes;
import com.algaworks.brewer.repository.Estilos;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class DashboardController {

	@Autowired
	private Cervejas cervejas;

	@Autowired
	private Clientes clientes;

	@Autowired
	private Estilos estilos;

	@Autowired
	private Vendas vendas;

	@GetMapping("/dashboard")
	public ModelAndView dashboard() {
		ModelAndView mv = new ModelAndView("Dashboard");

		// Estatísticas básicas
		mv.addObject("totalCervejas", cervejas.count());
		mv.addObject("totalClientes", clientes.count());
		mv.addObject("totalEstilos", estilos.count());

		// Estatísticas de vendas
		BigDecimal valorTotalAno = vendas.valorTotalNoAno();
		BigDecimal valorTotalMes = vendas.valorTotalNoMes();
		BigDecimal ticketMedio = vendas.valorTicketMedioNoAno();

		mv.addObject("valorTotalAno", valorTotalAno != null ? valorTotalAno : BigDecimal.ZERO);
		mv.addObject("valorTotalMes", valorTotalMes != null ? valorTotalMes : BigDecimal.ZERO);
		mv.addObject("ticketMedio", ticketMedio != null ? ticketMedio : BigDecimal.ZERO);

		return mv;
	}

	/**
	 * Endpoint JSON para dados do gráfico de vendas por mês (últimos 6 meses).
	 */
	@GetMapping("/dashboard/vendas/totalPorMes")
	@ResponseBody
	public List<VendaMes> listarTotalVendasPorMes() {
		return vendas.totalPorMes();
	}

	/**
	 * Endpoint JSON para dados do gráfico de vendas por origem das cervejas.
	 */
	@GetMapping("/dashboard/vendas/totalPorOrigem")
	@ResponseBody
	public List<VendaOrigem> listarTotalVendasPorOrigem() {
		return vendas.totalPorOrigem();
	}

}