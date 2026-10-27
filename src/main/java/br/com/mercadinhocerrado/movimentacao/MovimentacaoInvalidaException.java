package br.com.mercadinhocerrado.movimentacao;

public class MovimentacaoInvalidaException extends RuntimeException {
    public MovimentacaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
