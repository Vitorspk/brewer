-- Inserir permissão CANCELAR_VENDA
INSERT INTO permissao (nome) VALUES ('ROLE_CANCELAR_VENDA');

-- Associar permissão ao grupo Administrador (assumindo que grupo Administrador tem código 1)
INSERT INTO grupo_permissao (codigo_grupo, codigo_permissao)
VALUES (1, (SELECT codigo FROM permissao WHERE nome = 'ROLE_CANCELAR_VENDA'));
