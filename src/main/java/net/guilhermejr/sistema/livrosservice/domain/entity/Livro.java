package net.guilhermejr.sistema.livrosservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/** Um livro da biblioteca. País e gênero são do autor, não do livro. */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
@Entity
@Table(name = "livros")
public class Livro extends Auditoria implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Não é único: volumes de uma coleção podem dividir o mesmo código. */
    private String isbn;

    @Column(nullable = false)
    private String titulo;

    private String subtitulo;

    private Integer anoPublicacao;

    private Integer paginas;

    @ManyToOne(fetch = FetchType.LAZY)
    private Editora editora;

    private String colecao;

    private String tradutor;

    @ManyToOne(fetch = FetchType.LAZY)
    private Pais pais;

    @ManyToOne(fetch = FetchType.LAZY)
    private Genero genero;

    @ManyToOne(fetch = FetchType.LAZY)
    private Formato formato;

    @ManyToOne(fetch = FetchType.LAZY)
    private Estante estante;


    /** Última tentativa automática de achar a capa; null = a tarefa ainda vai tentar. */
    private LocalDateTime capaBuscada;

    @ManyToMany
    @JoinTable(
            name = "livros_autores",
            joinColumns = @JoinColumn(name = "livro_id"),
            inverseJoinColumns = @JoinColumn(name = "autor_id"))
    @OrderBy("nome")
    private Set<Autor> autores = new LinkedHashSet<>();

}
