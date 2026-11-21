package net.guilhermejr.sistema.livrosservice.domain.repository;

import net.guilhermejr.sistema.livrosservice.domain.entity.Editora;
import net.guilhermejr.sistema.livrosservice.domain.entity.Livro;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LivroRepository extends JpaRepository<Livro, Long> {

    /** Tudo o que a lista mostra vem numa consulta só. */
    @EntityGraph(attributePaths = {"autores", "editora", "formato", "estante"})
    List<Livro> findAllByUsuarioOrderByTituloAsc(UUID usuario);

    /** Busca por id restringindo ao dono: id de outro usuário não é encontrado. */
    @EntityGraph(attributePaths = {"autores", "editora", "formato", "estante"})
    Optional<Livro> findByIdAndUsuario(Long id, UUID usuario);

    List<Livro> findAllByUsuarioAndIsbnOrderByTituloAsc(UUID usuario, String isbn);

    long countByEditora(Editora editora);

    /** Para montar a consulta da capa fora de uma transação. */
    @EntityGraph(attributePaths = {"autores", "editora"})
    Optional<Livro> findComAutoresById(Long id);

    /** Livros que a tarefa de capas ainda não tentou, de qualquer usuário. */
    @Query("select l.id from Livro l where l.capaBuscada is null"
            + " and not exists (select 1 from Capa c where c.livroId = l.id) order by l.id")
    List<Long> idsSemCapaBuscada(Limit limite);

}
