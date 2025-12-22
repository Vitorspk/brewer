-- Inserir permissão CANCELAR_VENDA (código 3, após ROLE_CADASTRAR_CIDADE e ROLE_CADASTRAR_USUARIO)
INSERT INTO permissao (codigo, nome) VALUES (3, 'ROLE_CANCELAR_VENDA');

-- CODE QUALITY FIX: Phase 12 - Medium Priority Issue #8
-- Associar permissão ao grupo Administrador usando subquery ao invés de ID hardcoded
-- Antes: VALUES (1, 3) assumia que Administrador sempre tinha código 1
-- Depois: Busca o código dinamicamente pelo nome
INSERT INTO grupo_permissao (codigo_grupo, codigo_permissao)
VALUES (
    (SELECT codigo FROM grupo WHERE nome = 'Administrador'),
    (SELECT codigo FROM permissao WHERE nome = 'ROLE_CANCELAR_VENDA')
);
