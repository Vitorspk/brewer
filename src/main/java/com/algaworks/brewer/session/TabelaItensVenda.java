package com.algaworks.brewer.session;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.algaworks.brewer.model.Cerveja;
import com.algaworks.brewer.model.ItemVenda;

@SessionScope
@Component
public class TabelaItensVenda {

	private String uuid;
	private final List<ItemVenda> itens = Collections.synchronizedList(new ArrayList<>());

	public BigDecimal getValorTotal() {
		return itens.stream()
				.map(ItemVenda::getValorTotal)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);
	}

	/**
	 * Adiciona um item à venda.
	 * Se a cerveja já existir na lista, incrementa a quantidade.
	 *
	 * @throws IllegalArgumentException se cerveja ou quantidade forem nulos
	 */
	public synchronized void adicionarItem(Cerveja cerveja, Integer quantidade) {
		Objects.requireNonNull(cerveja, "Cerveja não pode ser nula");
		Objects.requireNonNull(quantidade, "Quantidade não pode ser nula");

		if (quantidade <= 0) {
			throw new IllegalArgumentException("Quantidade deve ser maior que zero");
		}

		Optional<ItemVenda> itemVendaOptional = buscarItemPorCerveja(cerveja);

		if (itemVendaOptional.isPresent()) {
			// Item já existe - atualiza quantidade
			ItemVenda itemVenda = itemVendaOptional.get();
			itemVenda.setQuantidade(itemVenda.getQuantidade() + quantidade);
		} else {
			// Item novo - adiciona à lista
			ItemVenda itemVenda = new ItemVenda();
			itemVenda.setCerveja(cerveja);
			itemVenda.setQuantidade(quantidade);
			itemVenda.setValorUnitario(cerveja.getValor());
			itens.add(itemVenda);
		}
	}

	/**
	 * Altera a quantidade de um item específico.
	 *
	 * @throws IllegalArgumentException se cerveja ou quantidade forem nulos
	 */
	public synchronized void alterarQuantidadeItens(Cerveja cerveja, Integer quantidade) {
		Objects.requireNonNull(cerveja, "Cerveja não pode ser nula");
		Objects.requireNonNull(quantidade, "Quantidade não pode ser nula");

		if (quantidade <= 0) {
			throw new IllegalArgumentException("Quantidade deve ser maior que zero");
		}

		Optional<ItemVenda> itemVendaOptional = buscarItemPorCerveja(cerveja);
		itemVendaOptional.ifPresent(item -> item.setQuantidade(quantidade));
	}

	/**
	 * Remove um item da venda baseado no código da cerveja.
	 *
	 * @throws IllegalArgumentException se cerveja for nula
	 */
	public synchronized void excluirItem(Cerveja cerveja) {
		Objects.requireNonNull(cerveja, "Cerveja não pode ser nula");
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
