package br.com.mercadinhocerrado.movimentacao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movimentacoes")
public class MovimentacaoController {
    private final MovimentacaoService movimentacoes;

    public MovimentacaoController(MovimentacaoService movimentacoes) {
        this.movimentacoes = movimentacoes;
    }

    @PostMapping
    public MovimentacaoResponse registrar(@Valid @RequestBody RegistroMovimentacaoRequest dados) {
        return movimentacoes.registrar(dados);
    }

    @GetMapping
    public PaginaMovimentacoesResponse listar(
            @RequestParam(required = false) @Positive Long produtoId,
            @RequestParam(required = false) TipoMovimentacao tipo,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return movimentacoes.listar(produtoId, tipo, pagina, tamanho);
    }
}
