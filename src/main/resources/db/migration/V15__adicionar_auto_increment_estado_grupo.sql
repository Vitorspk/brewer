-- Adicionar AUTO_INCREMENT nas tabelas estado e grupo
-- Necessário para testes de integração que usam @GeneratedValue(strategy = GenerationType.IDENTITY)

ALTER TABLE estado MODIFY codigo BIGINT(20) AUTO_INCREMENT;
ALTER TABLE grupo MODIFY codigo BIGINT(20) AUTO_INCREMENT;