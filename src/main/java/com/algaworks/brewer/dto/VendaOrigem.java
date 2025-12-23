package com.algaworks.brewer.dto;

/**
 * DTO para representar o total de vendas por origem das cervejas.
 * Usado nos gráficos do Dashboard.
 */
public class VendaOrigem {

    private String origem;
    private Integer totalVendas;

    public VendaOrigem() {
    }

    public VendaOrigem(String origem, Integer totalVendas) {
        this.origem = origem;
        this.totalVendas = totalVendas;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public Integer getTotalVendas() {
        return totalVendas;
    }

    public void setTotalVendas(Integer totalVendas) {
        this.totalVendas = totalVendas;
    }
}