-- Inserir dados de exemplo necessários para as vendas de teste

-- Inserir cliente de exemplo (código 1)
INSERT INTO cliente (codigo, nome, tipo_pessoa, cpf_cnpj, telefone, email, logradouro, numero, cep, codigo_cidade)
VALUES (1, 'João Silva', 'FISICA', '123.456.789-00', '(11) 98765-4321', 'joao.silva@example.com',
        'Rua das Flores', '123', '01234-567', 1);

-- Inserir cervejas de exemplo (códigos 1 e 2) usando estilo Pilsner que já existe
INSERT INTO cerveja (codigo, sku, nome, descricao, valor, teor_alcoolico, comissao, sabor, origem, quantidade_estoque, codigo_estilo)
VALUES (1, 'BRH001', 'Brahma Chopp', 'Cerveja tipo Pilsen de qualidade', 3.50, 4.5, 10.00, 'SUAVE', 'NACIONAL', 100,
        (SELECT codigo FROM estilo WHERE nome = 'Pilsner' LIMIT 1));

INSERT INTO cerveja (codigo, sku, nome, descricao, valor, teor_alcoolico, comissao, sabor, origem, quantidade_estoque, codigo_estilo)
VALUES (2, 'SKL001', 'Skol Pilsen', 'Cerveja refrescante e leve', 2.90, 4.7, 8.00, 'SUAVE', 'NACIONAL', 150,
        (SELECT codigo FROM estilo WHERE nome = 'Pilsner' LIMIT 1));