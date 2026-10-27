ALTER TABLE movimentacao
    ADD COLUMN id_operacao UUID,
    ADD COLUMN saldo_anterior INTEGER,
    ADD COLUMN saldo_atual INTEGER,
    ADD COLUMN estoque_minimo_registro INTEGER,
    ADD CONSTRAINT uk_movimentacao_operacao UNIQUE (id_produto, id_operacao),
    ADD CONSTRAINT ck_movimentacao_resultado CHECK (
        (id_operacao IS NULL AND saldo_anterior IS NULL AND saldo_atual IS NULL AND estoque_minimo_registro IS NULL)
        OR
        (id_operacao IS NOT NULL AND saldo_anterior IS NOT NULL AND saldo_atual IS NOT NULL
         AND estoque_minimo_registro IS NOT NULL
         AND saldo_anterior >= 0 AND saldo_atual >= 0 AND estoque_minimo_registro >= 0
         AND ((tipo = 'ENTRADA' AND saldo_atual::BIGINT = saldo_anterior::BIGINT + quantidade)
              OR (tipo = 'SAIDA' AND saldo_atual::BIGINT = saldo_anterior::BIGINT - quantidade)))
    );

-- Registros anteriores permanecem sem os dados de resultado que não foram coletados.
