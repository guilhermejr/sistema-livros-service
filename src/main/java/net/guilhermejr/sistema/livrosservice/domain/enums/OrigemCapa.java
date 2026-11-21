package net.guilhermejr.sistema.livrosservice.domain.enums;

/** De onde veio a capa. Só as automáticas são trocadas quando o ISBN muda. */
public enum OrigemCapa {
    GOOGLE_BOOKS,
    /** Achada pelo título e autor: pode ser de outra edição do mesmo livro. */
    GOOGLE_BOOKS_TITULO,
    OPEN_LIBRARY,
    MANUAL
}
