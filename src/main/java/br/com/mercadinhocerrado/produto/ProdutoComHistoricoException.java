package br.com.mercadinhocerrado.produto;

public class ProdutoComHistoricoException extends RuntimeException {
    public ProdutoComHistoricoException(Throwable causa) {
        super("Não é possível excluir um produto com histórico de movimentações.", causa);
    }
}
