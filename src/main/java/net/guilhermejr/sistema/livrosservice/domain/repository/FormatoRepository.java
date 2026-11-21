package net.guilhermejr.sistema.livrosservice.domain.repository;

import net.guilhermejr.sistema.livrosservice.domain.entity.Formato;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormatoRepository extends JpaRepository<Formato, Long> {
}
