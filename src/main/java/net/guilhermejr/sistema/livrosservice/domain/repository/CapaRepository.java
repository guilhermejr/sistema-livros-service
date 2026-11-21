package net.guilhermejr.sistema.livrosservice.domain.repository;

import net.guilhermejr.sistema.livrosservice.domain.entity.Capa;
import net.guilhermejr.sistema.livrosservice.domain.enums.OrigemCapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapaRepository extends JpaRepository<Capa, UUID> {

    /** Id do livro, id e origem da capa, sem o conteúdo, para montar a lista. */
    interface CapaDoLivro {
        Long getLivroId();
        UUID getId();
        OrigemCapa getOrigem();
    }

    @Query("select c.livroId as livroId, c.id as id, c.origem as origem from Capa c where c.livroId in"
            + " (select l.id from Livro l where l.usuario = :usuario)")
    List<CapaDoLivro> idsDoUsuario(@Param("usuario") UUID usuario);

    @Query("select c.livroId as livroId, c.id as id, c.origem as origem from Capa c where c.livroId = :livroId")
    Optional<CapaDoLivro> idDoLivro(@Param("livroId") Long livroId);

    Optional<Capa> findByLivroId(Long livroId);

    @Modifying(flushAutomatically = true)
    @Query("delete from Capa c where c.livroId = :livroId")
    void apagarDoLivro(@Param("livroId") Long livroId);

}
