package com.algaworks.brewer.repository.helper.cliente;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algaworks.brewer.model.Cidade;
import com.algaworks.brewer.model.Cliente;
import com.algaworks.brewer.model.Endereco;
import com.algaworks.brewer.model.Estado;
import com.algaworks.brewer.repository.filter.ClienteFilter;

public class ClientesImpl implements ClientesQueries {

	@PersistenceContext
	private EntityManager manager;

	@Override
	@Transactional(readOnly = true)
	public Page<Cliente> filtrar(ClienteFilter filtro, Pageable pageable) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Cliente> criteriaQuery = builder.createQuery(Cliente.class);
		Root<Cliente> root = criteriaQuery.from(Cliente.class);

		// Left outer joins (similar to JoinType.LEFT_OUTER_JOIN)
		Join<Cliente, Endereco> enderecoJoin = root.join("endereco", JoinType.LEFT);
		Join<Endereco, Cidade> cidadeJoin = enderecoJoin.join("cidade", JoinType.LEFT);
		Join<Cidade, Estado> estadoJoin = cidadeJoin.join("estado", JoinType.LEFT);

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

		TypedQuery<Cliente> query = manager.createQuery(criteriaQuery);
		query.setFirstResult((int) pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());

		return new PageImpl<>(query.getResultList(), pageable, total(filtro));
	}

	private Long total(ClienteFilter filtro) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
		Root<Cliente> root = criteriaQuery.from(Cliente.class);

		Predicate[] predicates = criarPredicates(filtro, builder, root);
		criteriaQuery.where(predicates);
		criteriaQuery.select(builder.count(root));

		return manager.createQuery(criteriaQuery).getSingleResult();
	}

	private Predicate[] criarPredicates(ClienteFilter filtro, CriteriaBuilder builder, Root<Cliente> root) {
		List<Predicate> predicates = new ArrayList<>();

		if (filtro != null) {
			if (!StringUtils.isEmpty(filtro.getNome())) {
				predicates.add(builder.like(builder.lower(root.get("nome")),
					"%" + filtro.getNome().toLowerCase() + "%"));
			}

			if (!StringUtils.isEmpty(filtro.getCpfOuCnpj())) {
				predicates.add(builder.equal(root.get("cpfOuCnpj"), filtro.getCpfOuCnpjSemFormatacao()));
			}
		}

		return predicates.toArray(new Predicate[0]);
	}

}