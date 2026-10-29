package br.com.mercadinhocerrado.produto;

import java.math.BigDecimal;
import br.com.mercadinhocerrado.movimentacao.MovimentacaoInvalidaException;
import br.com.mercadinhocerrado.movimentacao.TipoMovimentacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

// Testes unitários: exercitam a regra de negócio sem Spring, banco ou rede.
class ProdutoTest {
    private Produto produto(int saldo) {
        return new Produto("Arroz", "Alimentos", new BigDecimal("24.90"), saldo, 5);
    }

    @Test
    void saidaDeDuasUnidadesReduzSeisParaQuatro() {
        var produto = produto(6);
        produto.movimentar(TipoMovimentacao.SAIDA, 2);
        assertEquals(4, produto.getQuantidadeEstoque());
    }

    @Test
    void entradaPodeAtingirLimiteInteiroSemOverflow() {
        var produto = produto(Integer.MAX_VALUE - 1);
        produto.movimentar(TipoMovimentacao.ENTRADA, 1);
        assertEquals(Integer.MAX_VALUE, produto.getQuantidadeEstoque());
    }

    @Test
    void entradaAcimaDoLimitePreservaSaldoAnterior() {
        var produto = produto(Integer.MAX_VALUE);
        assertThrows(MovimentacaoInvalidaException.class,
                () -> produto.movimentar(TipoMovimentacao.ENTRADA, 1));
        assertEquals(Integer.MAX_VALUE, produto.getQuantidadeEstoque());
    }

    @Test
    void saidaSemSaldoPreservaEstoque() {
        var produto = produto(1);
        assertThrows(MovimentacaoInvalidaException.class,
                () -> produto.movimentar(TipoMovimentacao.SAIDA, 2));
        assertEquals(1, produto.getQuantidadeEstoque());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void quantidadeInvalidaNaoAlteraEstoque(int quantidade) {
        var produto = produto(6);
        assertThrows(MovimentacaoInvalidaException.class,
                () -> produto.movimentar(TipoMovimentacao.ENTRADA, quantidade));
        assertEquals(6, produto.getQuantidadeEstoque());
    }

    @Test
    void tipoAusenteNaoAlteraEstoque() {
        var produto = produto(6);
        assertThrows(MovimentacaoInvalidaException.class, () -> produto.movimentar(null, 1));
        assertEquals(6, produto.getQuantidadeEstoque());
    }
}
