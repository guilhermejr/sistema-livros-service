-- Listas fixas, iguais para todos os usuários. País e gênero são do autor.
create table paises (
    id bigserial not null,
    nome varchar(50) not null,
    primary key (id)
);

create table generos (
    id bigserial not null,
    nome varchar(30) not null,
    primary key (id)
);

create table formatos (
    id bigserial not null,
    nome varchar(30) not null,
    primary key (id)
);

-- Cadastros do usuário. Nascem ao salvar um livro com um nome novo, e autores e
-- editoras sem nenhum livro são apagados junto com o último que os usava.
create table autores (
    id bigserial not null,
    nome varchar(200) not null,
    usuario uuid not null,
    primary key (id)
);

create unique index uk_autores_usuario_nome on autores (usuario, nome);

create table editoras (
    id bigserial not null,
    nome varchar(100) not null,
    usuario uuid not null,
    primary key (id)
);

create unique index uk_editoras_usuario_nome on editoras (usuario, nome);

create table estantes (
    id bigserial not null,
    nome varchar(100) not null,
    usuario uuid not null,
    primary key (id)
);

create unique index uk_estantes_usuario_nome on estantes (usuario, nome);

-- O ISBN não é único: volumes de uma mesma coleção podem dividir o mesmo código.
create table livros (
    id bigserial not null,
    isbn varchar(20),
    titulo varchar(200) not null,
    subtitulo varchar(300),
    ano_publicacao integer,
    paginas integer,
    editora_id bigint,
    colecao varchar(100),
    tradutor varchar(200),
    pais_id bigint,
    genero_id bigint,
    formato_id bigint,
    estante_id bigint,
    data_leitura date,
    -- Última tentativa automática de achar a capa; null = ainda não tentou.
    capa_buscada timestamp without time zone,
    criado timestamp without time zone,
    atualizado timestamp without time zone,
    usuario uuid not null,
    primary key (id),
    constraint fk_livros_editora foreign key (editora_id) references editoras (id),
    constraint fk_livros_pais foreign key (pais_id) references paises (id),
    constraint fk_livros_genero foreign key (genero_id) references generos (id),
    constraint fk_livros_formato foreign key (formato_id) references formatos (id),
    constraint fk_livros_estante foreign key (estante_id) references estantes (id)
);

create index ix_livros_usuario on livros (usuario);

create table livros_autores (
    livro_id bigint not null,
    autor_id bigint not null,
    primary key (livro_id, autor_id),
    constraint fk_livros_autores_livro foreign key (livro_id) references livros (id) on delete cascade,
    constraint fk_livros_autores_autor foreign key (autor_id) references autores (id)
);

create index ix_livros_autores_autor on livros_autores (autor_id);

-- A imagem fica no banco. O id muda a cada capa nova, então o endereço público
-- /capas/{id} pode ser guardado em cache para sempre.
create table capas (
    id uuid not null,
    livro_id bigint not null,
    conteudo bytea not null,
    tipo varchar(50) not null,
    origem varchar(20) not null,
    criado timestamp without time zone,
    primary key (id),
    constraint fk_capas_livro foreign key (livro_id) references livros (id) on delete cascade,
    constraint ck_capas_origem check (origem in ('GOOGLE_BOOKS', 'OPEN_LIBRARY', 'MANUAL'))
);

create unique index uk_capas_livro on capas (livro_id);
