package br.com.mercadinhocerrado.movimentacao;

import br.com.mercadinhocerrado.produto.ProdutoRepository;
import br.com.mercadinhocerrado.produto.ProdutoNaoEncontradoException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class MovimentacaoService {
    private final ProdutoRepository produtos;
    private final MovimentacaoRepository movimentacoes;

    public MovimentacaoService(ProdutoRepository produtos, MovimentacaoRepository movimentacoes) {
        this.produtos = produtos;
        this.movimentacoes = movimentacoes;
    }

    @Transactional
    public MovimentacaoResponse registrar(@Valid RegistroMovimentacaoRequest dados) {
        // Serializa alterações do mesmo produto, inclusive reenvios concorrentes.
        var produto = produtos.buscarParaAlteracao(dados.produtoId())
                .orElseThrow(ProdutoNaoEncontradoException::new);
        var existente = movimentacoes.findByProdutoIdAndIdOperacao(dados.produtoId(), dados.idOperacao());
        if (existente.isPresent()) {
            var movimento = existente.get();
            if (movimento.getTipo() != dados.tipo() || !movimento.getQuantidade().equals(dados.quantidade())) {
                throw new MovimentacaoInvalidaException("Este identificador de operação já foi usado com outros dados. Inicie uma nova operação.");
            }
            return MovimentacaoResponse.de(movimento);
        }

        int saldoAnterior = produto.getQuantidadeEstoque();
        produto.movimentar(dados.tipo(), dados.quantidade());
        var movimento = movimentacoes.saveAndFlush(new Movimentacao(produto, dados, saldoAnterior));
        return MovimentacaoResponse.de(movimento);
    }
}
