package net.guilhermejr.sistema.livrosservice.domain.repository;

import net.guilhermejr.sistema.livrosservice.domain.entity.Autor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutorRepository extends JpaRepository<Autor, Long> {

    List<Autor> findAllByUsuarioOrderByNomeAsc(UUID usuario);

    Optional<Autor> findFirstByUsuarioAndNomeIgnoreCase(UUID usuario, String nome);

    @Modifying(flushAutomatically = true)
    @Query(value = "delete from autores a where a.usuario = :usuario"
            + " and not exists (select 1 from livros_autores la where la.autor_id = a.id)", nativeQuery = true)
    int apagarSemLivro(@Param("usuario") UUID usuario);

}
