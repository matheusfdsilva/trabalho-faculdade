package br.com.mercadinhocerrado.produto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    Page<Produto> findByNomeContainingIgnoreCase(String nome, Pageable paginacao);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id")
    Optional<Produto> buscarParaAlteracao(@Param("id") Long id);
}
