package br.com.mercadinhocerrado.produto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaProdutosResponse(List<ProdutoResponse> itens, int pagina, int tamanho,
                                     long totalItens, int totalPaginas) {
    public static PaginaProdutosResponse de(Page<Produto> produtos) {
        return new PaginaProdutosResponse(produtos.map(ProdutoResponse::de).getContent(),
                produtos.getNumber(), produtos.getSize(), produtos.getTotalElements(), produtos.getTotalPages());
    }
}
