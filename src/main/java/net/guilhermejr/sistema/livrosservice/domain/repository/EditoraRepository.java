package net.guilhermejr.sistema.livrosservice.domain.repository;

import net.guilhermejr.sistema.livrosservice.domain.entity.Editora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EditoraRepository extends JpaRepository<Editora, Long> {

    List<Editora> findAllByUsuarioOrderByNomeAsc(UUID usuario);

    Optional<Editora> findFirstByUsuarioAndNomeIgnoreCase(UUID usuario, String nome);

    @Modifying(flushAutomatically = true)
    @Query(value = "delete from editoras e where e.usuario = :usuario"
            + " and not exists (select 1 from livros l where l.editora_id = e.id)", nativeQuery = true)
    int apagarSemLivro(@Param("usuario") UUID usuario);

}
