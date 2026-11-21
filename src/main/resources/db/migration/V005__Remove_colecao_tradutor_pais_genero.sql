-- Coleção, tradutor e país/gênero do autor deixaram de existir no módulo.
alter table livros
    drop column colecao,
    drop column tradutor,
    drop column pais_id,
    drop column genero_id;

drop table paises;
drop table generos;
