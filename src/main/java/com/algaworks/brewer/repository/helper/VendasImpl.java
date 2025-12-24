package com.algaworks.brewer.repository.helper;

import com.algaworks.brewer.dto.VendaMes;
import com.algaworks.brewer.dto.VendaOrigem;
import com.algaworks.brewer.model.Origem;
import com.algaworks.brewer.model.Venda;
import com.algaworks.brewer.model.StatusVenda;
import com.algaworks.brewer.repository.VendasQueries;
import com.algaworks.brewer.repository.filter.VendaFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class VendasImpl implements VendasQueries {

    @PersistenceContext
    private EntityManager manager;

    @Override
    public Page<Venda> filtrar(VendaFilter filtro, Pageable pageable) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Venda> criteria = builder.createQuery(Venda.class);
        Root<Venda> root = criteria.from(Venda.class);

        // Fetch joins para evitar N+1
        root.fetch("cliente", JoinType.LEFT);
        root.fetch("usuario", JoinType.LEFT);

        List<Predicate> predicates = criarPredicates(filtro, builder, root);
        criteria.where(predicates.toArray(new Predicate[0]));

        // Adicionar ordenação
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            for (Sort.Order order : pageable.getSort()) {
                if (order.isAscending()) {
                    orders.add(builder.asc(root.get(order.getProperty())));
                } else {
                    orders.add(builder.desc(root.get(order.getProperty())));
                }
            }
            criteria.orderBy(orders);
        }

        TypedQuery<Venda> query = manager.createQuery(criteria);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Venda> vendas = query.getResultList();
        long total = total(filtro);

        return new PageImpl<>(vendas, pageable, total);
    }

    @Override
    public Optional<Venda> buscarComItens(Long codigo) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Venda> criteria = builder.createQuery(Venda.class);
        Root<Venda> root = criteria.from(Venda.class);

        // ROBUSTNESS FIX: cliente is @NotNull, use INNER JOIN (default)
        root.fetch("cliente");
        root.fetch("usuario", JoinType.LEFT);
        root.fetch("itens", JoinType.LEFT).fetch("cerveja");

        criteria.where(builder.equal(root.get("codigo"), codigo));

        // ROBUSTNESS FIX: Use getResultList() to avoid connection closed error with streams
        List<Venda> result = manager.createQuery(criteria).getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public BigDecimal valorTotalNoAno() {
        LocalDateTime dataInicio = Year.now().atDay(1).atStartOfDay();
        return valorTotalNoPeriodo(dataInicio);
    }

    @Override
    public BigDecimal valorTotalNoMes() {
        LocalDateTime dataInicio = YearMonth.now().atDay(1).atStartOfDay();
        return valorTotalNoPeriodo(dataInicio);
    }

    @Override
    public BigDecimal valorTicketMedioNoAno() {
        LocalDateTime dataInicio = Year.now().atDay(1).atStartOfDay();

        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Double> criteria = builder.createQuery(Double.class);
        Root<Venda> root = criteria.from(Venda.class);

        criteria.select(builder.avg(root.get("valorTotal")));
        criteria.where(
            builder.greaterThanOrEqualTo(root.get("dataCriacao"), dataInicio.toLocalDate()),
            builder.equal(root.get("status"), StatusVenda.EMITIDA)
        );

        Double valorMedio = manager.createQuery(criteria).getSingleResult();
        return valorMedio != null ? BigDecimal.valueOf(valorMedio) : BigDecimal.ZERO;
    }

    private BigDecimal valorTotalNoPeriodo(LocalDateTime dataInicio) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> criteria = builder.createQuery(BigDecimal.class);
        Root<Venda> root = criteria.from(Venda.class);

        criteria.select(builder.sum(root.get("valorTotal")));
        criteria.where(
            builder.greaterThanOrEqualTo(root.get("dataCriacao"), dataInicio.toLocalDate()),
            builder.equal(root.get("status"), StatusVenda.EMITIDA)
        );

        BigDecimal valorTotal = manager.createQuery(criteria).getSingleResult();
        return valorTotal != null ? valorTotal : BigDecimal.ZERO;
    }

    private Long total(VendaFilter filtro) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<Venda> root = criteria.from(Venda.class);

        List<Predicate> predicates = criarPredicates(filtro, builder, root);
        criteria.where(predicates.toArray(new Predicate[0]));
        criteria.select(builder.count(root));

        return manager.createQuery(criteria).getSingleResult();
    }

    private List<Predicate> criarPredicates(VendaFilter filtro, CriteriaBuilder builder, Root<Venda> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (filtro.getCodigo() != null) {
            predicates.add(builder.equal(root.get("codigo"), filtro.getCodigo()));
        }

        if (filtro.getStatus() != null) {
            predicates.add(builder.equal(root.get("status"), filtro.getStatus()));
        }

        if (filtro.getDesde() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("dataCriacao"), filtro.getDesde()));
        }

        if (filtro.getAte() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("dataCriacao"), filtro.getAte()));
        }

        if (filtro.getDataCriacaoInicio() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("dataCriacao"), filtro.getDataCriacaoInicio()));
        }

        if (filtro.getDataCriacaoFim() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("dataCriacao"), filtro.getDataCriacaoFim()));
        }

        if (filtro.getValorMinimo() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("valorTotal"), filtro.getValorMinimo()));
        }

        if (filtro.getValorMaximo() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("valorTotal"), filtro.getValorMaximo()));
        }

        if (StringUtils.hasText(filtro.getNomeCliente())) {
            predicates.add(builder.like(
                builder.lower(root.get("cliente").get("nome")),
                "%" + filtro.getNomeCliente().toLowerCase() + "%"
            ));
        }

        if (StringUtils.hasText(filtro.getCpfOuCnpjCliente())) {
            predicates.add(builder.equal(
                root.get("cliente").get("cpfOuCnpj"),
                filtro.getCpfOuCnpjCliente()
            ));
        }

        return predicates;
    }

    @Override
    public List<VendaMes> totalPorMes() {
        List<VendaMes> vendasMes = new ArrayList<>();

        LocalDate hoje = LocalDate.now();

        // Obter vendas dos últimos 6 meses
        for (int i = 5; i >= 0; i--) {
            YearMonth yearMonth = YearMonth.from(hoje.minusMonths(i));
            LocalDate inicio = yearMonth.atDay(1);
            LocalDate fim = yearMonth.atEndOfMonth();

            Integer total = countVendasNoPeriodo(inicio, fim);

            String nomeMes = yearMonth.getMonth()
                .getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"));

            vendasMes.add(new VendaMes(nomeMes, total));
        }

        return vendasMes;
    }

    @Override
    public List<VendaOrigem> totalPorOrigem() {
        // Query nativa para buscar total de vendas por origem da cerveja
        String jpql = "SELECT c.origem, COUNT(v) " +
                     "FROM Venda v " +
                     "JOIN v.itens i " +
                     "JOIN i.cerveja c " +
                     "WHERE v.status = :status " +
                     "GROUP BY c.origem";

        List<Object[]> resultado = manager.createQuery(jpql, Object[].class)
            .setParameter("status", StatusVenda.EMITIDA)
            .getResultList();

        List<VendaOrigem> vendasOrigem = new ArrayList<>();
        for (Object[] row : resultado) {
            Origem origem = (Origem) row[0];
            Long total = (Long) row[1];
            vendasOrigem.add(new VendaOrigem(origem.getDescricao(), total.intValue()));
        }

        return vendasOrigem;
    }

    private Integer countVendasNoPeriodo(LocalDate inicio, LocalDate fim) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<Venda> root = criteria.from(Venda.class);

        criteria.select(builder.countDistinct(root));
        criteria.where(
            builder.between(root.get("dataCriacao"), inicio, fim),
            builder.equal(root.get("status"), StatusVenda.EMITIDA)
        );

        Long count = manager.createQuery(criteria).getSingleResult();
        return count != null ? count.intValue() : 0;
    }
}
