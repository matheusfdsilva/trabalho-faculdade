package br.com.mercadinhocerrado.movimentacao;

import java.time.Instant;
import java.util.UUID;

public record HistoricoMovimentacaoResponse(Long id, UUID idOperacao, Long produtoId, String produtoNome,
        TipoMovimentacao tipo, Integer quantidade, Instant dataMovimentacao, Integer saldoAnterior,
        Integer saldoAtual, Integer estoqueMinimo, Boolean reposicaoNecessaria) {
    public static HistoricoMovimentacaoResponse de(Movimentacao movimento) {
        var resultado = MovimentacaoResponse.de(movimento);
        return new HistoricoMovimentacaoResponse(resultado.id(), resultado.idOperacao(), resultado.produtoId(),
                movimento.getProduto().getNome(), resultado.tipo(), resultado.quantidade(), resultado.dataMovimentacao(),
                resultado.saldoAnterior(), resultado.saldoAtual(), resultado.estoqueMinimo(), resultado.reposicaoNecessaria());
    }
}
