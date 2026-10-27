package br.com.mercadinhocerrado.movimentacao;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import br.com.mercadinhocerrado.produto.Produto;
import jakarta.persistence.*;

@Entity
@Table(name = "movimentacao")
public class Movimentacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimentacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_produto", nullable = false, updatable = false)
    private Produto produto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private TipoMovimentacao tipo;

    @Column(nullable = false, updatable = false)
    private Integer quantidade;

    @Column(name = "data_movimentacao", nullable = false, updatable = false)
    private Instant dataMovimentacao;

    @Column(name = "id_operacao", updatable = false)
    private UUID idOperacao;

    @Column(name = "saldo_anterior", updatable = false)
    private Integer saldoAnterior;

    @Column(name = "saldo_atual", updatable = false)
    private Integer saldoAtual;

    @Column(name = "estoque_minimo_registro", updatable = false)
    private Integer estoqueMinimo;

    protected Movimentacao() { }

    public Movimentacao(Produto produto, RegistroMovimentacaoRequest dados, int saldoAnterior) {
        this.produto = produto;
        this.tipo = dados.tipo();
        this.quantidade = dados.quantidade();
        this.idOperacao = dados.idOperacao();
        this.saldoAnterior = saldoAnterior;
        this.saldoAtual = produto.getQuantidadeEstoque();
        this.estoqueMinimo = produto.getEstoqueMinimo();
        this.dataMovimentacao = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public TipoMovimentacao getTipo() { return tipo; }
    public Integer getQuantidade() { return quantidade; }
    public Instant getDataMovimentacao() { return dataMovimentacao; }
    public UUID getIdOperacao() { return idOperacao; }
    public Integer getSaldoAnterior() { return saldoAnterior; }
    public Integer getSaldoAtual() { return saldoAtual; }
    public Integer getEstoqueMinimo() { return estoqueMinimo; }
}
