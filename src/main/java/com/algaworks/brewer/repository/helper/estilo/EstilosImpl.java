package com.algaworks.brewer.repository.helper.estilo;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algaworks.brewer.model.Estilo;
import com.algaworks.brewer.repository.filter.EstiloFilter;

public class EstilosImpl implements EstilosQueries {

	@PersistenceContext
	private EntityManager manager;

	@Override
	@Transactional(readOnly = true)
	public Page<Estilo> filtrar(EstiloFilter filtro, Pageable pageable) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Estilo> criteriaQuery = builder.createQuery(Estilo.class);
		Root<Estilo> root = criteriaQuery.from(Estilo.class);

		Predicate[] predicates = criarPredicates(filtro, builder, root);
		criteriaQuery.where(predicates);

		// Apply sorting from Pageable
		if (pageable.getSort().isSorted()) {
			pageable.getSort().forEach(order -> {
				if (order.isAscending()) {
					criteriaQuery.orderBy(builder.asc(root.get(order.getProperty())));
				} else {
					criteriaQuery.orderBy(builder.desc(root.get(order.getProperty())));
				}
			});
		}

		TypedQuery<Estilo> query = manager.createQuery(criteriaQuery);
		query.setFirstResult((int) pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());

		return new PageImpl<>(query.getResultList(), pageable, total(filtro));
	}

	private Long total(EstiloFilter filtro) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
		Root<Estilo> root = criteriaQuery.from(Estilo.class);

		Predicate[] predicates = criarPredicates(filtro, builder, root);
		criteriaQuery.where(predicates);
		criteriaQuery.select(builder.count(root));

		return manager.createQuery(criteriaQuery).getSingleResult();
	}

	private Predicate[] criarPredicates(EstiloFilter filtro, CriteriaBuilder builder, Root<Estilo> root) {
		List<Predicate> predicates = new ArrayList<>();

		if (filtro != null) {
			if (!StringUtils.isEmpty(filtro.getNome())) {
				predicates.add(builder.like(builder.lower(root.get("nome")),
					"%" + filtro.getNome().toLowerCase() + "%"));
			}
		}

		return predicates.toArray(new Predicate[0]);
	}

}