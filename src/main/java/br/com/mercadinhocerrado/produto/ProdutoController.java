package br.com.mercadinhocerrado.produto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {
    private final ProdutoService produtos;

    public ProdutoController(ProdutoService produtos) {
        this.produtos = produtos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoResponse cadastrar(@Valid @RequestBody CadastroProdutoRequest dados) {
        return produtos.cadastrar(dados);
    }

    @GetMapping
    public PaginaProdutosResponse listar(
            @RequestParam(defaultValue = "") @Size(max = 150) String nome,
            @RequestParam(defaultValue = "false") boolean reposicao,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return produtos.listar(nome, reposicao, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public ProdutoResponse consultar(@PathVariable @Positive Long id) {
        return produtos.consultar(id);
    }

    @PutMapping("/{id}")
    public ProdutoResponse editar(@PathVariable @Positive Long id,
                                 @Valid @RequestBody EdicaoProdutoRequest dados) {
        return produtos.editar(id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable @Positive Long id) {
        produtos.excluir(id);
    }
}
