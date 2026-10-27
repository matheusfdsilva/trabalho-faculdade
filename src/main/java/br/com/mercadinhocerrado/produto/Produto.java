package br.com.mercadinhocerrado.produto;

import java.math.BigDecimal;

import jakarta.persistence.*;
import br.com.mercadinhocerrado.movimentacao.TipoMovimentacao;
import br.com.mercadinhocerrado.movimentacao.MovimentacaoInvalidaException;

@Entity
@Table(name = "produto")
public class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produto")
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "quantidade_estoque", nullable = false)
    private Integer quantidadeEstoque;

    @Column(name = "estoque_minimo", nullable = false)
    private Integer estoqueMinimo;

    protected Produto() {
    }

    public Produto(String nome, String categoria, BigDecimal preco,
                   Integer quantidadeEstoque, Integer estoqueMinimo) {
        this.nome = nome;
        this.categoria = categoria;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
        this.estoqueMinimo = estoqueMinimo;
    }

    public void editar(String nome, String categoria, BigDecimal preco, Integer estoqueMinimo) {
        this.nome = nome;
        this.categoria = categoria;
        this.preco = preco;
        this.estoqueMinimo = estoqueMinimo;
    }

    public void movimentar(TipoMovimentacao tipo, int quantidade) {
        if (tipo == null || quantidade <= 0) {
            throw new MovimentacaoInvalidaException("Selecione o tipo e informe uma quantidade maior que zero.");
        }
        long novoSaldo = (long) quantidadeEstoque + (tipo == TipoMovimentacao.ENTRADA ? quantidade : -quantidade);
        if (novoSaldo < 0) {
            throw new MovimentacaoInvalidaException("Quantidade superior ao estoque disponível.");
        }
        if (novoSaldo > Integer.MAX_VALUE) {
            throw new MovimentacaoInvalidaException("A entrada excede o limite de unidades permitido por produto.");
        }
        this.quantidadeEstoque = (int) novoSaldo;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCategoria() { return categoria; }
    public BigDecimal getPreco() { return preco; }
    public Integer getQuantidadeEstoque() { return quantidadeEstoque; }
    public Integer getEstoqueMinimo() { return estoqueMinimo; }
}
