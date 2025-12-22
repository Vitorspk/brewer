-- Inserir dados de exemplo necessários para as vendas de teste
-- Migration idempotente: verifica se os dados já existem antes de inserir

-- Inserir cliente de exemplo (código 1) - apenas se não existir
INSERT INTO cliente (codigo, nome, tipo_pessoa, cpf_cnpj, telefone, email, logradouro, numero, cep, codigo_cidade)
SELECT 1, 'João Silva', 'FISICA', '123.456.789-00', '(11) 98765-4321', 'joao.silva@example.com',
       'Rua das Flores', '123', '01234-567', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cliente WHERE codigo = 1);

-- Inserir cerveja de exemplo (código 1) - apenas se não existir e se estilo Pilsner existe
INSERT INTO cerveja (codigo, sku, nome, descricao, valor, teor_alcoolico, comissao, sabor, origem, quantidade_estoque, codigo_estilo)
SELECT 1, 'BRH001', 'Brahma Chopp', 'Cerveja tipo Pilsen de qualidade', 3.50, 4.5, 10.00, 'SUAVE', 'NACIONAL', 100,
       (SELECT codigo FROM estilo WHERE nome = 'Pilsner' LIMIT 1)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cerveja WHERE codigo = 1)
  AND EXISTS (SELECT 1 FROM estilo WHERE nome = 'Pilsner');

-- Inserir cerveja de exemplo (código 2) - apenas se não existir e se estilo Pilsner existe
INSERT INTO cerveja (codigo, sku, nome, descricao, valor, teor_alcoolico, comissao, sabor, origem, quantidade_estoque, codigo_estilo)
SELECT 2, 'SKL001', 'Skol Pilsen', 'Cerveja refrescante e leve', 2.90, 4.7, 8.00, 'SUAVE', 'NACIONAL', 150,
       (SELECT codigo FROM estilo WHERE nome = 'Pilsner' LIMIT 1)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cerveja WHERE codigo = 2)
  AND EXISTS (SELECT 1 FROM estilo WHERE nome = 'Pilsner');