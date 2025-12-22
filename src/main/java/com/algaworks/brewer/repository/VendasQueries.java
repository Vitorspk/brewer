package com.algaworks.brewer.repository;

import com.algaworks.brewer.model.Venda;
import com.algaworks.brewer.repository.filter.VendaFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;

public interface VendasQueries {

    Page<Venda> filtrar(VendaFilter filtro, Pageable pageable);

    Optional<Venda> buscarComItens(Long codigo);

    BigDecimal valorTotalNoAno();

    BigDecimal valorTotalNoMes();

    BigDecimal valorTicketMedioNoAno();
}
