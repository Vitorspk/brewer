package com.algaworks.brewer.repository.helper.cerveja;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algaworks.brewer.dto.CervejaDTO;
import com.algaworks.brewer.model.Cerveja;
import com.algaworks.brewer.repository.filter.CervejaFilter;

public class CervejasImpl implements CervejasQueries {

	@PersistenceContext
	private EntityManager manager;

	@Override
	@Transactional(readOnly = true)
	public Page<Cerveja> filtrar(CervejaFilter filtro, Pageable pageable) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Cerveja> criteriaQuery = builder.createQuery(Cerveja.class);
		Root<Cerveja> root = criteriaQuery.from(Cerveja.class);

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

		TypedQuery<Cerveja> query = manager.createQuery(criteriaQuery);
		query.setFirstResult((int) pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());

		return new PageImpl<>(query.getResultList(), pageable, total(filtro));
	}

	private Long total(CervejaFilter filtro) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
		Root<Cerveja> root = criteriaQuery.from(Cerveja.class);

		Predicate[] predicates = criarPredicates(filtro, builder, root);
		criteriaQuery.where(predicates);
		criteriaQuery.select(builder.count(root));

		return manager.createQuery(criteriaQuery).getSingleResult();
	}

	private Predicate[] criarPredicates(CervejaFilter filtro, CriteriaBuilder builder, Root<Cerveja> root) {
		List<Predicate> predicates = new ArrayList<>();

		if (filtro != null) {
			if (!StringUtils.isEmpty(filtro.getSku())) {
				predicates.add(builder.equal(root.get("sku"), filtro.getSku()));
			}

			if (!StringUtils.isEmpty(filtro.getNome())) {
				predicates.add(builder.like(builder.lower(root.get("nome")),
					"%" + filtro.getNome().toLowerCase() + "%"));
			}

			if (isEstiloPresente(filtro)) {
				predicates.add(builder.equal(root.get("estilo"), filtro.getEstilo()));
			}

			if (filtro.getSabor() != null) {
				predicates.add(builder.equal(root.get("sabor"), filtro.getSabor()));
			}

			if (filtro.getOrigem() != null) {
				predicates.add(builder.equal(root.get("origem"), filtro.getOrigem()));
			}

			if (filtro.getValorDe() != null) {
				predicates.add(builder.greaterThanOrEqualTo(root.get("valor"), filtro.getValorDe()));
			}

			if (filtro.getValorAte() != null) {
				predicates.add(builder.lessThanOrEqualTo(root.get("valor"), filtro.getValorAte()));
			}
		}

		return predicates.toArray(new Predicate[0]);
	}

	private boolean isEstiloPresente(CervejaFilter filtro) {
		return filtro.getEstilo() != null && filtro.getEstilo().getCodigo() != null;
	}

	@Override
	public List<CervejaDTO> porSkuOuNome(String skuOuNome) {
		String jpql = "select new com.algaworks.brewer.dto.CervejaDTO(codigo, sku, nome, origem, valor, foto) "
				+ "from Cerveja where lower(sku) like lower(:skuOuNome) or lower(nome) like lower(:skuOuNome)";
		List<CervejaDTO> cervejasFiltradas = manager.createQuery(jpql, CervejaDTO.class)
					.setParameter("skuOuNome", skuOuNome + "%")
					.getResultList();
		return cervejasFiltradas;
	}

}