-- Adicionar AUTO_INCREMENT nas tabelas estado e grupo
-- Necessário para testes de integração que usam @GeneratedValue(strategy = GenerationType.IDENTITY)

-- Para estado: precisa remover a FK de cidade temporariamente
ALTER TABLE cidade DROP FOREIGN KEY cidade_ibfk_1;
ALTER TABLE estado MODIFY codigo BIGINT(20) AUTO_INCREMENT;
ALTER TABLE cidade ADD CONSTRAINT cidade_ibfk_1 FOREIGN KEY (codigo_estado) REFERENCES estado(codigo);

-- Para grupo: precisa remover FKs de usuario_grupo e grupo_permissao temporariamente
ALTER TABLE usuario_grupo DROP FOREIGN KEY usuario_grupo_ibfk_2;
ALTER TABLE grupo_permissao DROP FOREIGN KEY grupo_permissao_ibfk_1;
ALTER TABLE grupo MODIFY codigo BIGINT(20) AUTO_INCREMENT;
ALTER TABLE usuario_grupo ADD CONSTRAINT usuario_grupo_ibfk_2 FOREIGN KEY (codigo_grupo) REFERENCES grupo(codigo);
ALTER TABLE grupo_permissao ADD CONSTRAINT grupo_permissao_ibfk_1 FOREIGN KEY (codigo_grupo) REFERENCES grupo(codigo);