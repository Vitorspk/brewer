package com.algaworks.brewer.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "venda")
@DynamicUpdate
public class Venda implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;

	@Column(name = "data_criacao")
	private LocalDate dataCriacao;

	@Column(name = "valor_frete")
	private BigDecimal valorFrete;

	@Column(name = "valor_desconto")
	private BigDecimal valorDesconto;

	@Column(name = "valor_total")
	private BigDecimal valorTotal;

	private String observacao;

	@Column(name = "data_hora_entrega")
	private LocalDateTime dataHoraEntrega;

	@NotNull(message = "O status é obrigatório")
	@Enumerated(EnumType.STRING)
	private StatusVenda status = StatusVenda.ORCAMENTO;

	@NotNull(message = "O cliente é obrigatório")
	@ManyToOne
	@JoinColumn(name = "codigo_cliente")
	private Cliente cliente;

	@ManyToOne
	@JoinColumn(name = "codigo_usuario")
	private Usuario usuario;

	@OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ItemVenda> itens = new ArrayList<>();

	@Transient
	private String uuid;

	@Transient
	private LocalDate dataEntrega;

	@Transient
	private LocalTime horarioEntrega;

	@PrePersist
	@PreUpdate
	private void prePersistPreUpdate() {
		this.valorTotal = calcularValorTotal();
	}

	public Long getCodigo() {
		return codigo;
	}

	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}

	public LocalDate getDataCriacao() {
		return dataCriacao;
	}

	public void setDataCriacao(LocalDate dataCriacao) {
		this.dataCriacao = dataCriacao;
	}

	public BigDecimal getValorFrete() {
		return valorFrete;
	}

	public void setValorFrete(BigDecimal valorFrete) {
		this.valorFrete = valorFrete;
	}

	public BigDecimal getValorDesconto() {
		return valorDesconto;
	}

	public void setValorDesconto(BigDecimal valorDesconto) {
		this.valorDesconto = valorDesconto;
	}

	public BigDecimal getValorTotal() {
		return valorTotal;
	}

	public void setValorTotal(BigDecimal valorTotal) {
		this.valorTotal = valorTotal;
	}

	public String getObservacao() {
		return observacao;
	}

	public void setObservacao(String observacao) {
		this.observacao = observacao;
	}

	public LocalDateTime getDataHoraEntrega() {
		return dataHoraEntrega;
	}

	public void setDataHoraEntrega(LocalDateTime dataHoraEntrega) {
		this.dataHoraEntrega = dataHoraEntrega;
	}

	public StatusVenda getStatus() {
		return status;
	}

	public void setStatus(StatusVenda status) {
		this.status = status;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public List<ItemVenda> getItens() {
		return itens;
	}

	public void setItens(List<ItemVenda> itens) {
		this.itens = itens;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public LocalDate getDataEntrega() {
		return dataEntrega;
	}

	public void setDataEntrega(LocalDate dataEntrega) {
		this.dataEntrega = dataEntrega;
	}

	public LocalTime getHorarioEntrega() {
		return horarioEntrega;
	}

	public void setHorarioEntrega(LocalTime horarioEntrega) {
		this.horarioEntrega = horarioEntrega;
	}

	public BigDecimal getValorTotalItens() {
		return getItens().stream()
				.map(ItemVenda::getValorTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public void adicionarItens(List<ItemVenda> itens) {
		this.itens.clear();
		this.itens.addAll(itens);
		this.itens.forEach(i -> i.setVenda(this));
	}

	public BigDecimal calcularValorTotal() {
		BigDecimal valorTotalItens = getValorTotalItens();
		BigDecimal valorFrete = Optional.ofNullable(getValorFrete()).orElse(BigDecimal.ZERO);
		BigDecimal valorDesconto = Optional.ofNullable(getValorDesconto()).orElse(BigDecimal.ZERO);
		return valorTotalItens.add(valorFrete).subtract(valorDesconto);
	}

	public boolean isNova() {
		return codigo == null;
	}

	public boolean isSalvarPermitido() {
		return !isNova() && (status.equals(StatusVenda.ORCAMENTO) || status.equals(StatusVenda.EMITIDA));
	}

	public boolean isCancelada() {
		return status.equals(StatusVenda.CANCELADA);
	}

	public boolean isEmitida() {
		return status.equals(StatusVenda.EMITIDA);
	}

	public boolean isOrcamento() {
		return StatusVenda.ORCAMENTO.equals(status);
	}

	/**
	 * Calcula o número de dias desde a criação da venda até hoje.
	 *
	 * TESTABILITY FIX: Phase 12 - Medium Priority Issue #2
	 * Usa getCurrentDate() ao invés de LocalDate.now() hardcoded,
	 * permitindo testes determinísticos ao sobrescrever o método.
	 *
	 * @return número de dias desde a criação, ou 0 se dataCriacao for null
	 */
	public Long diasCriacao() {
		if (dataCriacao == null) {
			return 0L;
		}
		LocalDate hoje = getCurrentDate();
		return ChronoUnit.DAYS.between(dataCriacao, hoje);
	}

	/**
	 * Retorna a data atual.
	 *
	 * Protected para permitir override nos testes com data fixa.
	 * Em produção, usa LocalDate.now().
	 *
	 * @return data atual
	 */
	protected LocalDate getCurrentDate() {
		return LocalDate.now();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((codigo == null) ? 0 : codigo.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Venda other = (Venda) obj;
		if (codigo == null) {
			if (other.codigo != null)
				return false;
		} else if (!codigo.equals(other.codigo))
			return false;
		return true;
	}

}