package br.com.mercadinhocerrado.produto;

import java.math.BigDecimal;

public record ProdutoResponse(Long id, String nome, String categoria, BigDecimal preco,
                              Integer quantidadeEstoque, Integer estoqueMinimo) {
    public static ProdutoResponse de(Produto produto) {
        return new ProdutoResponse(produto.getId(), produto.getNome(), produto.getCategoria(),
                produto.getPreco(), produto.getQuantidadeEstoque(), produto.getEstoqueMinimo());
    }
}
