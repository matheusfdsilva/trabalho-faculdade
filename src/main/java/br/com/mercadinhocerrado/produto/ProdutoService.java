package br.com.mercadinhocerrado.produto;

import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;

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

    @Transactional(readOnly = true)
    public PaginaProdutosResponse listar(String nome, int pagina, int tamanho) {
        var paginacao = PageRequest.of(pagina, tamanho, Sort.by("nome").ascending().and(Sort.by("id")));
        return PaginaProdutosResponse.de(produtos.findByNomeContainingIgnoreCase(nome.strip(), paginacao));
    }

    @Transactional(readOnly = true)
    public ProdutoResponse consultar(Long id) {
        return ProdutoResponse.de(produtos.findById(id).orElseThrow(ProdutoNaoEncontradoException::new));
    }

    @Transactional
    public ProdutoResponse editar(Long id, @Valid EdicaoProdutoRequest dados) {
        var produto = produtos.buscarParaAlteracao(id).orElseThrow(ProdutoNaoEncontradoException::new);
        produto.editar(dados.nome().strip(), dados.categoria().strip(), dados.preco(), dados.estoqueMinimo());
        return ProdutoResponse.de(produto);
    }

    @Transactional
    public void excluir(Long id) {
        var produto = produtos.buscarParaAlteracao(id).orElseThrow(ProdutoNaoEncontradoException::new);
        try {
            produtos.delete(produto);
            produtos.flush();
        } catch (DataIntegrityViolationException erro) {
            // A chave estrangeira também protege contra uma movimentação concorrente.
            throw new ProdutoComHistoricoException(erro);
        }
    }
}
