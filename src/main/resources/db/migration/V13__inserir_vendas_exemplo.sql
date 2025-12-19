-- Vendas de exemplo para o cliente código 1
INSERT INTO venda (codigo, data_criacao, valor_frete, valor_desconto, valor_total, status, codigo_cliente, codigo_usuario)
VALUES (1, '2024-01-15', 15.00, 5.00, 110.00, 'EMITIDA', 1, 1);

INSERT INTO item_venda (codigo, quantidade, valor_unitario, codigo_cerveja, codigo_venda)
VALUES (1, 2, 50.00, 1, 1);

INSERT INTO venda (codigo, data_criacao, valor_frete, valor_desconto, valor_total, status, codigo_cliente, codigo_usuario)
VALUES (2, '2024-02-20', 20.00, 0.00, 140.00, 'ORCAMENTO', 1, 1);

INSERT INTO item_venda (codigo, quantidade, valor_unitario, codigo_cerveja, codigo_venda)
VALUES (2, 3, 40.00, 2, 2);