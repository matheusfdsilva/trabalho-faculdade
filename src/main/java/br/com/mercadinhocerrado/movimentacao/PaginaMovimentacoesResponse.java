package br.com.mercadinhocerrado.movimentacao;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaMovimentacoesResponse(List<HistoricoMovimentacaoResponse> itens, int pagina, int tamanho,
                                         long totalItens, int totalPaginas) {
    public static PaginaMovimentacoesResponse de(Page<Movimentacao> movimentos) {
        return new PaginaMovimentacoesResponse(movimentos.map(HistoricoMovimentacaoResponse::de).getContent(),
                movimentos.getNumber(), movimentos.getSize(), movimentos.getTotalElements(), movimentos.getTotalPages());
    }
}
