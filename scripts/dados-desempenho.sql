-- Execute somente em um banco de teste com as migracoes aplicadas.
-- O script recusa um catalogo ou historico ja preenchido e nao altera usuarios.
BEGIN;

DO $$
DECLARE
    indice INTEGER;
    produto_id BIGINT;
    saldo INTEGER;
    novo_saldo INTEGER;
    operacao VARCHAR(10);
BEGIN
    LOCK TABLE produto, movimentacao IN EXCLUSIVE MODE;
    IF EXISTS (SELECT 1 FROM produto) OR EXISTS (SELECT 1 FROM movimentacao) THEN
        RAISE EXCEPTION 'Use um banco de teste com catalogo e historico vazios. Nenhum dado foi alterado.';
    END IF;

    INSERT INTO produto (nome, categoria, preco, quantidade_estoque, estoque_minimo)
        SELECT 'Produto desempenho ' || lpad(numero::TEXT, 3, '0'),
               'Dados ficticios', 10.00, CASE WHEN numero % 5 = 0 THEN 3 ELSE 50 END, 5
        FROM generate_series(1, 300) AS numero;

    FOR indice IN 1..1000 LOOP
        SELECT id_produto, quantidade_estoque INTO produto_id, saldo FROM produto
            ORDER BY id_produto OFFSET ((indice - 1) % 300) LIMIT 1;
        operacao := CASE WHEN indice % 2 = 0 OR saldo = 0 THEN 'ENTRADA' ELSE 'SAIDA' END;
        novo_saldo := saldo + CASE WHEN operacao = 'ENTRADA' THEN 1 ELSE -1 END;
        UPDATE produto SET quantidade_estoque = novo_saldo WHERE id_produto = produto_id;
        INSERT INTO movimentacao (id_produto, tipo, quantidade, id_operacao, saldo_anterior,
                                   saldo_atual, estoque_minimo_registro, data_movimentacao)
            VALUES (produto_id, operacao, 1, gen_random_uuid(), saldo, novo_saldo, 5,
                    CURRENT_TIMESTAMP - (1000 - indice) * INTERVAL '1 minute');
    END LOOP;
END $$;

COMMIT;
