package br.com.mercadinhocerrado.movimentacao;

import br.com.mercadinhocerrado.produto.ProdutoRepository;
import br.com.mercadinhocerrado.produto.ProdutoNaoEncontradoException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
@Validated
public class MovimentacaoService {
    private final ProdutoRepository produtos;
    private final MovimentacaoRepository movimentacoes;

    public MovimentacaoService(ProdutoRepository produtos, MovimentacaoRepository movimentacoes) {
        this.produtos = produtos;
        this.movimentacoes = movimentacoes;
    }

    @Transactional(readOnly = true)
    public PaginaMovimentacoesResponse listar(Long produtoId, TipoMovimentacao tipo, int pagina, int tamanho) {
        if (produtoId != null && !produtos.existsById(produtoId)) {
            throw new ProdutoNaoEncontradoException();
        }
        var paginacao = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "dataMovimentacao", "id"));
        return PaginaMovimentacoesResponse.de(movimentacoes.buscarHistorico(produtoId, tipo, paginacao));
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
