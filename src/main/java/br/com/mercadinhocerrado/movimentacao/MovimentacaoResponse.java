package br.com.mercadinhocerrado.movimentacao;

import java.time.Instant;
import java.util.UUID;

public record MovimentacaoResponse(Long id, UUID idOperacao, Long produtoId, TipoMovimentacao tipo,
                                  Integer quantidade, Instant dataMovimentacao, Integer saldoAnterior,
                                  Integer saldoAtual, Integer estoqueMinimo, boolean reposicaoNecessaria) {
    public static MovimentacaoResponse de(Movimentacao movimento) {
        return new MovimentacaoResponse(movimento.getId(), movimento.getIdOperacao(),
                movimento.getProduto().getId(), movimento.getTipo(), movimento.getQuantidade(),
                movimento.getDataMovimentacao(), movimento.getSaldoAnterior(), movimento.getSaldoAtual(),
                movimento.getEstoqueMinimo(), movimento.getSaldoAtual() <= movimento.getEstoqueMinimo());
    }
}
