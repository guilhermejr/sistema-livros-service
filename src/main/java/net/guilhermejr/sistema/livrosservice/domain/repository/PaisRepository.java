package net.guilhermejr.sistema.livrosservice.domain.repository;

import net.guilhermejr.sistema.livrosservice.domain.entity.Pais;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaisRepository extends JpaRepository<Pais, Long> {
}
