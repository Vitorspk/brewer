-- Inserir permissão CANCELAR_VENDA (código 3, após ROLE_CADASTRAR_CIDADE e ROLE_CADASTRAR_USUARIO)
INSERT INTO permissao (codigo, nome) VALUES (3, 'ROLE_CANCELAR_VENDA');

-- Associar permissão ao grupo Administrador (código 1)
INSERT INTO grupo_permissao (codigo_grupo, codigo_permissao) VALUES (1, 3);
