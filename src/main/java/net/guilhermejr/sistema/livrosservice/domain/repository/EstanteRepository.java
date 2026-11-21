package net.guilhermejr.sistema.livrosservice.domain.repository;

import net.guilhermejr.sistema.livrosservice.domain.entity.Estante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstanteRepository extends JpaRepository<Estante, Long> {

    List<Estante> findAllByUsuarioOrderByNomeAsc(UUID usuario);

    Optional<Estante> findFirstByUsuarioAndNomeIgnoreCase(UUID usuario, String nome);

}
