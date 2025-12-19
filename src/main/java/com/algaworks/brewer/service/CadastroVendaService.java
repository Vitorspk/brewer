package com.algaworks.brewer.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.algaworks.brewer.model.StatusVenda;
import com.algaworks.brewer.model.Venda;
import com.algaworks.brewer.repository.Vendas;
import com.algaworks.brewer.service.exception.ImpossivelEmitirVendaException;

@Service
public class CadastroVendaService {

	@Autowired
	private Vendas vendas;

	@Transactional
	public Venda salvar(Venda venda) {
		if (venda.isNova()) {
			venda.setDataCriacao(LocalDate.now());
		}

		if (venda.getDataEntrega() != null && venda.getHorarioEntrega() != null) {
			venda.setDataHoraEntrega(LocalDateTime.of(venda.getDataEntrega(), venda.getHorarioEntrega()));
		}

		return vendas.saveAndFlush(venda);
	}

	@Transactional
	public void emitir(Venda venda) {
		venda = vendas.findById(venda.getCodigo())
				.orElseThrow(() -> new IllegalArgumentException("Venda não encontrada"));

		if (venda.isCancelada()) {
			throw new ImpossivelEmitirVendaException("Não é possível emitir uma venda cancelada");
		}

		venda.setStatus(StatusVenda.EMITIDA);
		vendas.save(venda);

		// TODO: Publicar evento VendaEvent para controle de estoque
	}

	@PreAuthorize("#venda.usuario == principal.usuario or hasRole('CANCELAR_VENDA')")
	@Transactional
	public void cancelar(Venda venda) {
		venda = vendas.findById(venda.getCodigo())
				.orElseThrow(() -> new IllegalArgumentException("Venda não encontrada"));

		if (venda.isEmitida()) {
			venda.setStatus(StatusVenda.CANCELADA);
			vendas.save(venda);
			// TODO: Publicar evento para liberar estoque
		}
	}

}