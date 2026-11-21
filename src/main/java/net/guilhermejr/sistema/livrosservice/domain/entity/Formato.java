package net.guilhermejr.sistema.livrosservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/** Físico, Ebook, Audiobook, HQ, Doado. Lista fixa. */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Entity
@Table(name = "formatos")
public class Formato implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nome;

}
