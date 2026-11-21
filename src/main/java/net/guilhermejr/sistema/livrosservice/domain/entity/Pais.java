package net.guilhermejr.sistema.livrosservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/** País do autor. Lista fixa, igual para todos os usuários. */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Entity
@Table(name = "paises")
public class Pais implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nome;

}
