package br.com.mercadinhocerrado.produto;

import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class ProdutoService {
    private final ProdutoRepository produtos;

    public ProdutoService(ProdutoRepository produtos) {
        this.produtos = produtos;
    }

    @Transactional
    public ProdutoResponse cadastrar(@Valid CadastroProdutoRequest dados) {
        var produto = new Produto(dados.nome().strip(), dados.categoria().strip(), dados.preco(),
                dados.quantidadeEstoque(), dados.estoqueMinimo());
        return ProdutoResponse.de(produtos.save(produto));
    }
}
