package com.algaworks.brewer.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.algaworks.brewer.controller.page.PageWrapper;
import com.algaworks.brewer.mail.Mailer;
import com.algaworks.brewer.service.VendaValidator;
import com.algaworks.brewer.model.Cerveja;
import com.algaworks.brewer.model.StatusVenda;
import com.algaworks.brewer.model.Venda;
import com.algaworks.brewer.repository.Cervejas;
import com.algaworks.brewer.repository.Vendas;
import com.algaworks.brewer.repository.filter.VendaFilter;
import com.algaworks.brewer.security.UsuarioSistema;
import com.algaworks.brewer.service.CadastroVendaService;
import com.algaworks.brewer.session.TabelaItensVenda;

@Controller
@RequestMapping("/vendas")
public class VendasController {

	@Autowired
	private Cervejas cervejas;

	@Autowired
	private TabelaItensVenda tabelaItensVenda;

	@Autowired
	private CadastroVendaService cadastroVendaService;

	@Autowired
	private VendaValidator vendaValidator;

	@Autowired
	private Vendas vendas;

	@Autowired
	private Mailer mailer;

	@InitBinder("venda")
	public void inicializarValidador(WebDataBinder binder) {
		binder.setValidator(vendaValidator);
	}

	@GetMapping("/nova")
	public ModelAndView nova(Venda venda) {
		ModelAndView mv = new ModelAndView("venda/CadastroVenda");

		setUuid(venda);
		mv.addObject("itens", venda.getItens());
		mv.addObject("valorTotal", tabelaItensVenda.getValorTotal());
		mv.addObject("statusVenda", StatusVenda.values());

		return mv;
	}

	@PostMapping(value = "/nova", params = "salvar")
	public ModelAndView salvar(Venda venda, BindingResult result, RedirectAttributes attributes, @AuthenticationPrincipal UsuarioSistema usuarioSistema) {
		// ROBUSTNESS FIX: Removed @Valid - validation is done manually in validarVenda()
		validarVenda(venda, result);
		if (result.hasErrors()) {
			return nova(venda);
		}

		venda.setUsuario(usuarioSistema.getUsuario());

		cadastroVendaService.salvar(venda);
		attributes.addFlashAttribute("mensagem", "Venda salva com sucesso");
		return new ModelAndView("redirect:/vendas/nova");
	}

	@PostMapping(value = "/nova", params = "emitir")
	public ModelAndView emitir(Venda venda, BindingResult result, RedirectAttributes attributes, @AuthenticationPrincipal UsuarioSistema usuarioSistema) {
		// ROBUSTNESS FIX: Removed @Valid - validation is done manually in validarVenda()
		validarVenda(venda, result);
		if (result.hasErrors()) {
			return nova(venda);
		}

		venda.setUsuario(usuarioSistema.getUsuario());

		cadastroVendaService.emitir(venda);
		attributes.addFlashAttribute("mensagem", "Venda emitida com sucesso");
		return new ModelAndView("redirect:/vendas/nova");
	}

	@PostMapping("/cancelar/{codigo}")
	public @ResponseBody String cancelar(@PathVariable("codigo") Venda venda) {
		try {
			cadastroVendaService.cancelar(venda);
		} catch (AccessDeniedException e) {
			return e.getMessage();
		}

		return "";
	}

	@GetMapping
	public ModelAndView pesquisar(VendaFilter vendaFilter, BindingResult result,
			@PageableDefault(size = 10) Pageable pageable, HttpServletRequest httpServletRequest) {
		ModelAndView mv = new ModelAndView("venda/PesquisaVendas");
		mv.addObject("todosStatus", StatusVenda.values());

		PageWrapper<Venda> paginaWrapper = new PageWrapper<>(vendas.filtrar(vendaFilter, pageable), httpServletRequest);
		mv.addObject("pagina", paginaWrapper);
		return mv;
	}

	@GetMapping("/{codigo}")
	public ModelAndView editar(@PathVariable Long codigo) {
		Venda venda = vendas.buscarComItens(codigo)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venda não encontrada"));

		setUuid(venda);
		for (com.algaworks.brewer.model.ItemVenda item : venda.getItens()) {
			tabelaItensVenda.adicionarItem(item.getCerveja(), item.getQuantidade());
		}

		ModelAndView mv = nova(venda);
		mv.addObject(venda);
		return mv;
	}

	@PostMapping(value = "/nova", params = "enviarEmail")
	public ModelAndView enviarEmail(Venda venda, BindingResult result, RedirectAttributes attributes, @AuthenticationPrincipal UsuarioSistema usuarioSistema) {
		// ROBUSTNESS FIX: Removed @Valid - validation is done manually in validarVenda()
		validarVenda(venda, result);
		if (result.hasErrors()) {
			return nova(venda);
		}

		venda.setUsuario(usuarioSistema.getUsuario());
		venda = cadastroVendaService.salvar(venda);

		enviarEmailSeClientePossuir(venda, attributes);

		return new ModelAndView("redirect:/vendas/nova");
	}

	private void enviarEmailSeClientePossuir(Venda venda, RedirectAttributes attributes) {
		if (venda.getCliente() != null && venda.getCliente().getEmail() != null && !venda.getCliente().getEmail().isBlank()) {
			mailer.enviar(venda);
			attributes.addFlashAttribute("mensagem", "Venda salva com sucesso! O e-mail será enviado em breve.");
		} else {
			attributes.addFlashAttribute("mensagem", "Venda salva com sucesso! Não foi possível enviar e-mail: cliente sem e-mail cadastrado.");
		}
	}

	private void validarVenda(Venda venda, BindingResult result) {
		venda.adicionarItens(tabelaItensVenda.getItens());
		venda.calcularValorTotal();

		vendaValidator.validate(venda, result);
	}

	private void setUuid(Venda venda) {
		if (tabelaItensVenda.getUuid() == null) {
			tabelaItensVenda.setUuid(venda.getUuid() != null ? venda.getUuid() : UUID.randomUUID().toString());
		}
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
