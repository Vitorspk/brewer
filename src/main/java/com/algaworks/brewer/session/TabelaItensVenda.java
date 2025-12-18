package com.algaworks.brewer.session;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.algaworks.brewer.model.Cerveja;
import com.algaworks.brewer.model.ItemVenda;

@SessionScope
@Component
public class TabelaItensVenda {

	private String uuid;
	private List<ItemVenda> itens = new ArrayList<>();

	public BigDecimal getValorTotal() {
		return itens.stream()
				.map(ItemVenda::getValorTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
	}

	/**
	 * Adiciona um item à venda.
	 * Se a cerveja já existir na lista, incrementa a quantidade.
	 */
	public void adicionarItem(Cerveja cerveja, Integer quantidade) {
		Optional<ItemVenda> itemVendaOptional = buscarItemPorCerveja(cerveja);

		ItemVenda itemVenda = null;
		if (itemVendaOptional.isPresent()) {
			// Item já existe - atualiza quantidade
			itemVenda = itemVendaOptional.get();
			itemVenda.setQuantidade(itemVenda.getQuantidade() + quantidade);
		} else {
			// Item novo - adiciona à lista
			itemVenda = new ItemVenda();
			itemVenda.setCerveja(cerveja);
			itemVenda.setQuantidade(quantidade);
			itemVenda.setValorUnitario(cerveja.getValor());
			itens.add(itemVenda);
		}
	}

	/**
	 * Altera a quantidade de um item específico.
	 */
	public void alterarQuantidadeItens(Cerveja cerveja, Integer quantidade) {
		Optional<ItemVenda> itemVendaOptional = buscarItemPorCerveja(cerveja);
		itemVendaOptional.ifPresent(item -> item.setQuantidade(quantidade));
	}

	/**
	 * Remove um item da venda baseado no código da cerveja.
	 */
	public void excluirItem(Cerveja cerveja) {
		itens.removeIf(item -> item.getCerveja().equals(cerveja));
	}

	public int total() {
		return itens.size();
	}

	public List<ItemVenda> getItens() {
		return itens;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	private Optional<ItemVenda> buscarItemPorCerveja(Cerveja cerveja) {
		return itens.stream()
				.filter(item -> item.getCerveja().equals(cerveja))
				.findAny();
	}
}
