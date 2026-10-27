package br.com.mercadinhocerrado.movimentacao;

import java.util.UUID;
import jakarta.validation.constraints.*;

public record RegistroMovimentacaoRequest(
        @NotNull(message = "Informe o identificador da operação para evitar duplicações.") UUID idOperacao,
        @NotNull(message = "Selecione um produto.") @Positive(message = "Selecione um produto válido.") Long produtoId,
        @NotNull(message = "Selecione Entrada ou Saída.") TipoMovimentacao tipo,
        @NotNull(message = "Informe a quantidade.")
        @Positive(message = "Informe uma quantidade maior que zero.") Integer quantidade) {
}
