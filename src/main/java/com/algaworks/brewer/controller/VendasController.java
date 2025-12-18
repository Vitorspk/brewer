package com.algaworks.brewer.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import com.algaworks.brewer.model.Cerveja;
import com.algaworks.brewer.repository.Cervejas;
import com.algaworks.brewer.session.TabelaItensVenda;

@Controller
@RequestMapping("/vendas")
public class VendasController {

	@Autowired
	private Cervejas cervejas;

	@Autowired
	private TabelaItensVenda tabelaItensVenda;

	@GetMapping("/nova")
	public ModelAndView nova() {
		ModelAndView mv = new ModelAndView("venda/CadastroVenda");

		if (tabelaItensVenda.getUuid() == null) {
			tabelaItensVenda.setUuid(UUID.randomUUID().toString());
		}

		mv.addObject("itens", tabelaItensVenda.getItens());
		mv.addObject("valorTotal", tabelaItensVenda.getValorTotal());
		mv.addObject("valorTotalItens", tabelaItensVenda.getValorTotal());

		return mv;
	}

	/**
	 * Adiciona um item à venda.
	 * Se o item já existir, incrementa a quantidade.
	 */
	@PostMapping("/item")
	public ModelAndView adicionarItem(Long codigoCerveja) {
		Cerveja cerveja = cervejas.findById(codigoCerveja)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cerveja não encontrada"));
		tabelaItensVenda.adicionarItem(cerveja, 1);
		return mvTabelaItensVenda();
	}

	/**
	 * Altera a quantidade de um item específico.
	 */
	@PutMapping("/item/{codigoCerveja}")
	public ModelAndView alterarQuantidadeItem(@PathVariable Long codigoCerveja,
			@RequestParam Integer quantidade) {
		Cerveja cerveja = cervejas.findById(codigoCerveja)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cerveja não encontrada"));
		tabelaItensVenda.alterarQuantidadeItens(cerveja, quantidade);
		return mvTabelaItensVenda();
	}

	/**
	 * Remove um item da venda.
	 */
	@DeleteMapping("/item/{codigoCerveja}")
	public ModelAndView excluirItem(@PathVariable Long codigoCerveja) {
		Cerveja cerveja = cervejas.findById(codigoCerveja)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cerveja não encontrada"));
		tabelaItensVenda.excluirItem(cerveja);
		return mvTabelaItensVenda();
	}

	private ModelAndView mvTabelaItensVenda() {
		ModelAndView mv = new ModelAndView("venda/TabelaItensVenda");
		mv.addObject("itens", tabelaItensVenda.getItens());
		mv.addObject("valorTotal", tabelaItensVenda.getValorTotal());
		mv.addObject("valorTotalItens", tabelaItensVenda.getValorTotal());
		return mv;
	}

}
