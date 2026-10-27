package br.com.mercadinhocerrado.movimentacao;

import jakarta.validation.Valid;
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
}
