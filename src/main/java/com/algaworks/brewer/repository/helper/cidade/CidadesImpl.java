package com.algaworks.brewer.repository.helper.cidade;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algaworks.brewer.model.Cidade;
import com.algaworks.brewer.model.Estado;
import com.algaworks.brewer.repository.filter.CidadeFilter;

public class CidadesImpl implements CidadesQueries {

	@PersistenceContext
	private EntityManager manager;

	@Override
	@Transactional(readOnly = true)
	public Page<Cidade> filtrar(CidadeFilter filtro, Pageable pageable) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Cidade> criteriaQuery = builder.createQuery(Cidade.class);
		Root<Cidade> root = criteriaQuery.from(Cidade.class);

		// Create fetch join for estado to avoid LazyInitializationException
		root.fetch("estado");

		Predicate[] predicates = criarPredicates(filtro, builder, root);
		criteriaQuery.where(predicates);
		criteriaQuery.distinct(true);

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

		TypedQuery<Cidade> query = manager.createQuery(criteriaQuery);
		query.setFirstResult((int) pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());

		return new PageImpl<>(query.getResultList(), pageable, total(filtro));
	}

	private Long total(CidadeFilter filtro) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
		Root<Cidade> root = criteriaQuery.from(Cidade.class);

		Predicate[] predicates = criarPredicates(filtro, builder, root);
		criteriaQuery.where(predicates);
		criteriaQuery.select(builder.count(root));

		return manager.createQuery(criteriaQuery).getSingleResult();
	}

	private Predicate[] criarPredicates(CidadeFilter filtro, CriteriaBuilder builder, Root<Cidade> root) {
		List<Predicate> predicates = new ArrayList<>();

		if (filtro != null) {
			if (filtro.getEstado() != null) {
				predicates.add(builder.equal(root.get("estado"), filtro.getEstado()));
			}

			if (!StringUtils.isEmpty(filtro.getNome())) {
				predicates.add(builder.like(builder.lower(root.get("nome")),
					"%" + filtro.getNome().toLowerCase() + "%"));
			}
		}

		return predicates.toArray(new Predicate[0]);
	}

}