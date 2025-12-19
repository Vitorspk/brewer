package com.algaworks.brewer.repository;

import com.algaworks.brewer.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Vendas extends JpaRepository<Venda, Long>, VendasQueries {

}