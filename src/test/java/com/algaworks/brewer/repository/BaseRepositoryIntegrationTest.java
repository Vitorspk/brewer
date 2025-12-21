package com.algaworks.brewer.repository;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * Classe base para testes de integração de repositórios.
 *
 * Fornece métodos utilitários para limpar dados de testes,
 * garantindo isolamento entre execuções de testes.
 */
@Transactional
public abstract class BaseRepositoryIntegrationTest {

    @Autowired
    protected TestEntityManager entityManager;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    /**
     * Limpa todas as tabelas de dados transacionais antes de cada teste.
     * Mantém dados de migrations essenciais (estados, cidades, usuário admin, etc.).
     */
    @BeforeEach
    void cleanupTestData() {
        EntityManager em = entityManager.getEntityManager();

        // Desabilitar verificação de foreign keys temporariamente
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

        // Limpar TODOS os dados das tabelas de teste
        // Os testes criam seus próprios dados, não dependem de migrations
        em.createNativeQuery("DELETE FROM item_venda").executeUpdate();
        em.createNativeQuery("DELETE FROM venda").executeUpdate();
        em.createNativeQuery("DELETE FROM cerveja").executeUpdate();
        em.createNativeQuery("DELETE FROM usuario_grupo").executeUpdate();
        em.createNativeQuery("DELETE FROM usuario").executeUpdate();
        em.createNativeQuery("DELETE FROM grupo_permissao").executeUpdate();
        em.createNativeQuery("DELETE FROM grupo").executeUpdate();
        em.createNativeQuery("DELETE FROM permissao").executeUpdate();
        em.createNativeQuery("DELETE FROM cliente").executeUpdate();
        em.createNativeQuery("DELETE FROM cidade").executeUpdate();
        em.createNativeQuery("DELETE FROM estado").executeUpdate();
        em.createNativeQuery("DELETE FROM estilo").executeUpdate();

        // Reabilitar verificação de foreign keys
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

        // Flush para garantir que as mudanças foram aplicadas
        em.flush();
        em.clear();
    }
}