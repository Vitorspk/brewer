package com.algaworks.brewer.repository.helper.usuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algaworks.brewer.model.Grupo;
import com.algaworks.brewer.model.Usuario;
import com.algaworks.brewer.model.UsuarioGrupo;
import com.algaworks.brewer.repository.filter.UsuarioFilter;

public class UsuariosImpl implements UsuariosQueries {

	@PersistenceContext
	private EntityManager manager;

	@Override
	public Optional<Usuario> porEmailEAtivo(String email) {
		return manager
				.createQuery("from Usuario where lower(email) = lower(:email) and ativo = true", Usuario.class)
				.setParameter("email", email).getResultList().stream().findFirst();
	}

	@Override
	public List<String> permissoes(Usuario usuario) {
		return manager.createQuery(
				"select distinct p.nome from Usuario u inner join u.grupos g inner join g.permissoes p where u = :usuario", String.class)
				.setParameter("usuario", usuario)
				.getResultList();
	}

	@Transactional(readOnly = true)
	@Override
	public Page<Usuario> filtrar(UsuarioFilter filtro, Pageable pageable) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Usuario> criteriaQuery = builder.createQuery(Usuario.class);
		Root<Usuario> root = criteriaQuery.from(Usuario.class);

		Predicate[] predicates = criarPredicates(filtro, builder, root, criteriaQuery);
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

		TypedQuery<Usuario> query = manager.createQuery(criteriaQuery);
		query.setFirstResult((int) pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());

		List<Usuario> filtrados = query.getResultList();
		filtrados.forEach(u -> Hibernate.initialize(u.getGrupos()));
		return new PageImpl<>(filtrados, pageable, total(filtro));
	}

	private Long total(UsuarioFilter filtro) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
		Root<Usuario> root = criteriaQuery.from(Usuario.class);

		Predicate[] predicates = criarPredicates(filtro, builder, root, criteriaQuery);
		criteriaQuery.where(predicates);
		criteriaQuery.select(builder.count(root));

		return manager.createQuery(criteriaQuery).getSingleResult();
	}

	private Predicate[] criarPredicates(UsuarioFilter filtro, CriteriaBuilder builder,
			Root<Usuario> root, CriteriaQuery<?> criteriaQuery) {
		List<Predicate> predicates = new ArrayList<>();

		if (filtro != null) {
			if (!StringUtils.isEmpty(filtro.getNome())) {
				predicates.add(builder.like(builder.lower(root.get("nome")),
					"%" + filtro.getNome().toLowerCase() + "%"));
			}

			if (!StringUtils.isEmpty(filtro.getEmail())) {
				predicates.add(builder.like(builder.lower(root.get("email")),
					filtro.getEmail().toLowerCase() + "%"));
			}

			if (filtro.getGrupos() != null && !filtro.getGrupos().isEmpty()) {
				List<Predicate> subqueryPredicates = new ArrayList<>();

				for (Grupo grupo : filtro.getGrupos()) {
					Subquery<Long> subquery = criteriaQuery.subquery(Long.class);
					Root<UsuarioGrupo> subRoot = subquery.from(UsuarioGrupo.class);

					subquery.select(subRoot.get("id").get("usuario").get("codigo"));
					subquery.where(builder.equal(
						subRoot.get("id").get("grupo").get("codigo"),
						grupo.getCodigo()
					));

					subqueryPredicates.add(builder.in(root.get("codigo")).value(subquery));
				}

				predicates.add(builder.and(subqueryPredicates.toArray(new Predicate[0])));
			}
		}

		return predicates.toArray(new Predicate[0]);
	}

}