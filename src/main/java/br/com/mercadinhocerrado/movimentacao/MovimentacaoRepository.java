package br.com.mercadinhocerrado.movimentacao;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {
    Optional<Movimentacao> findByProdutoIdAndIdOperacao(Long produtoId, UUID idOperacao);

    @EntityGraph(attributePaths = "produto")
    @Query("""
            select m from Movimentacao m
            where (:produtoId is null or m.produto.id = :produtoId)
            and (:tipo is null or m.tipo = :tipo)
            """)
    Page<Movimentacao> buscarHistorico(@Param("produtoId") Long produtoId,
                                     @Param("tipo") TipoMovimentacao tipo, Pageable paginacao);
}
