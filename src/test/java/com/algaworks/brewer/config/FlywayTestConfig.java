package com.algaworks.brewer.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Configuração do Flyway para testes de integração.
 *
 * Esta configuração garante que:
 * 1. O Flyway execute SEMPRE antes dos testes (@DataJpaTest tenta desabilitar)
 * 2. O banco seja limpo antes de cada suite de testes
 * 3. Todas as migrations sejam aplicadas
 *
 * IMPORTANTE: Apenas para testes! Nunca usar em produção.
 */
@TestConfiguration
@Profile("test")
public class FlywayTestConfig {

    /**
     * Strategy customizada que força execução do Flyway em testes.
     *
     * O @DataJpaTest tenta desabilitar o Flyway por padrão para usar
     * Hibernate DDL. Esta strategy sobrescreve esse comportamento.
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Limpa o banco de dados (apenas em testes!)
            flyway.clean();

            // Executa todas as migrations
            flyway.migrate();
        };
    }
}