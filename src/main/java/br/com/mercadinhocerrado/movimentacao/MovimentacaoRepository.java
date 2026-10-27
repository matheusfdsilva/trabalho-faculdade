package br.com.mercadinhocerrado.movimentacao;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {
    Optional<Movimentacao> findByProdutoIdAndIdOperacao(Long produtoId, UUID idOperacao);
}
