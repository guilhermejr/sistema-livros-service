package net.guilhermejr.sistema.livrosservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import net.guilhermejr.sistema.livrosservice.domain.enums.OrigemCapa;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * A imagem da capa de um livro, no máximo uma por livro. Trocar a capa grava uma
 * linha nova com outro id, por isso o endereço público pode ficar em cache para sempre.
 * O livro é só o id: a listagem nunca deve carregar o conteúdo junto.
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Entity
@Table(name = "capas")
public class Capa implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Long livroId;

    @Column(nullable = false)
    private byte[] conteudo;

    @Column(nullable = false)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemCapa origem;

    @Column(updatable = false)
    private LocalDateTime criado;

    public Capa(Long livroId, byte[] conteudo, String tipo, OrigemCapa origem) {
        this.id = UUID.randomUUID();
        this.livroId = livroId;
        this.conteudo = conteudo;
        this.tipo = tipo;
        this.origem = origem;
        this.criado = LocalDateTime.now(ZoneOffset.UTC);
    }

}
