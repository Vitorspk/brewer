package com.algaworks.brewer.dto;

import java.math.BigDecimal;

/**
 * DTO para representar o total de vendas por mês.
 * Usado nos gráficos do Dashboard.
 */
public class VendaMes {

    private String mes;
    private Integer totalVendas;

    public VendaMes() {
    }

    public VendaMes(String mes, Integer totalVendas) {
        this.mes = mes;
        this.totalVendas = totalVendas;
    }

    public String getMes() {
        return mes;
    }

    public void setMes(String mes) {
        this.mes = mes;
    }

    public Integer getTotalVendas() {
        return totalVendas;
    }

    public void setTotalVendas(Integer totalVendas) {
        this.totalVendas = totalVendas;
    }
}