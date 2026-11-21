-- Capa achada no Google Books pelo título e autor, quando o ISBN não acha nada.
-- Pode ser de outra edição do mesmo livro, por isso tem origem própria.
alter table capas drop constraint ck_capas_origem;
alter table capas add constraint ck_capas_origem
    check (origem in ('GOOGLE_BOOKS', 'GOOGLE_BOOKS_TITULO', 'OPEN_LIBRARY', 'MANUAL'));

-- Os livros que já passaram pela busca sem achar capa ganham uma nova tentativa,
-- agora também pelo título.
update livros set capa_buscada = null
where capa_buscada is not null and not exists (select 1 from capas c where c.livro_id = livros.id);
