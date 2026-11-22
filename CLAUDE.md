# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The book library: books with authors, publisher, shelf and cover, for the frontend's "Livros" tab.

| | |
|---|---|
| Port | `9013` |
| Context path | `/livros-service` |
| Role required | `ROLE_LIVROS` (except `GET /capas/{id}`, public) |

Part of a personal microservices system; sibling repos live at `../sistema-*`. The API gateway fronts it at `https://sistema-backend.guilhermejr.net/livros-service`.

## API

- `GET /livros` — every book of the user, by title, in one call (~1,300 books; the frontend searches and paginates).
- `GET /livros/opcoes` — what the form offers: the fixed `formatos` and the user's `autores`, `editoras`, `estantes`.
- `GET|PUT|DELETE /livros/{id}`, `POST /livros`.
- `GET /livros/isbn/{isbn}` — data to prefill a new book (`BuscadorDadosIsbn`): BrasilAPI, Open Library and Google Books (with the key) in parallel; each field comes from the first source that has it, in that order. Measured on real ISBNs: BrasilAPI (the CBL base) knows almost every Brazilian book but usually lacks authors, year and pages, and its authors may include translator and illustrators; Open Library often fills the authors. All-caps titles are softened (`Textos.semCaixaAlta`), "Não definido" publisher is dropped. Publisher and authors are matched to the user's own names (`chaveEditora` ignores "Editora", "Ltda", "Brasil", spaces…; on a tie the publisher with more books wins). `fontes` empty = no source knows the ISBN; `jaCadastrados` lists the user's books with that ISBN. A source that fails just drops out — nothing to retry, the user completes by hand.
- `POST /livros/{id}/capa/buscar` — looks the cover up now (replaces even a manual one). 400 with a message when nothing is found.
- `PUT /livros/{id}/capa` (multipart, field `arquivo`, up to 5 MB) — manual cover; converted to a JPEG at most 400 px wide (`ImagemCapa`).
- `DELETE /livros/{id}/capa` — also marks the search as done, so the scheduled task doesn't bring it back.
- `GET /capas/{id}` — the image, **without a token**: an `<img>` can't send `Authorization`. `SegurancaCapasConfig` adds a chain for `/capas/**` between the two that come from `seguranca-jwt`. A new cover gets a new UUID, so the response is cached for a year (`immutable`).

A book of another user answers 404.

## The data model

- Publisher, authors and shelf travel **by name**: a name the user doesn't have (case-insensitive) becomes a new row. Authors and publishers left without any book are deleted after each update/delete (`apagarSemLivro`); shelves stay, even empty.
- `formatos` is a fixed list, shared by every user. "Doado" is one of the formats, on purpose.
- Collection, translator and the author's country and gender came from the old system and were dropped by `V005__Remove_colecao_tradutor_pais_genero.sql` (columns and the `paises`/`generos` tables). Don't bring them back.
- ISBN is **not unique**: volumes of a collection share one (the old system told them apart with `-1`, `-2` suffixes in its key). Some old ISBNs are internal codes (`62`, 8 digits); only 10/13-digit ones are searched by ISBN (those books still get the title search).

`V002__Dados.sql` is the migration of the old MySQL tables (`bk_*`), all assigned to Guilherme's user id; its header lists every cleanup done. The module keeps no reading dates: the old `bk_Leitura` (reading history) was not migrated, and the book's `data_leitura`, which V002 still imports, is dropped by `V004__Remove_data_leitura.sql`.

## Covers

`BuscadorCapas` tries, in order:

1. the ISBN on **Google Books** (needs `googleBooksApiKey`);
2. the ISBN on **Open Library** (`default=false`, so a miss is a 404 instead of a blank image);
3. **title and first author on Google Books** (`intitle:`/`inauthor:`), also for books without a usable ISBN. Saved as `GOOGLE_BOOKS_TITULO`: it may be another edition, and the frontend says so.

Measured on 40 real ISBNs: Google knew 1 by ISBN, Open Library 13; title search found 21 of the remaining 27, of which 4 were **other books** ("Eu Morreria Por Ti" → "24 contos de F. Scott Fitzgerald"). Hence `mesmoTitulo`: the found title must equal the book's title up to its first " - " or ":" (`tituloBase`), ignoring accents and case, optionally followed by more words. That rejected all 4 and kept the 17; among matches, the same publisher wins (`mesmaEditora`). `BuscadorCapasTest` holds those real cases.

`TarefaCapas` runs every 5 minutes on 10 books whose `capa_buscada` is null (migrated, new, ISBN or title changed) and records the attempt either way. Two rules keep it honest:

- **Off in the `dev` profile** (`livros.capas.tarefa-ativa: false` in `application.yml`): the local database is a copy of the same books, and searching on both sides would spend the same key's daily quota twice. Locally a cover only comes from the "Buscar capa" button.
- **Without the Google key it does nothing.** Searching Open Library alone would mark books as tried that the title search would find later.
- A temporary failure (5xx, network, Google's 429 when the free 1,000/day quota runs out) throws `BuscaIndisponivelException`: the batch stops **without** marking the book and resumes next round. Only a real "not found" is recorded.
- **A failure that never goes away doesn't block the queue.** Open Library answers 503 forever for some covers (a broken file on its side; seen for ISBN 9788533613379 in Nov 2026, which stalled the task for a day, since the queue goes by id). When Open Library fails, `buscar` still tries the title search and rethrows only if that finds nothing. When the same book fails `FALHAS_SEGUIDAS` (3) rounds in a row, the task records it as tried without a cover and moves on. Quota (429) never counts. The counter lives in memory and resets on restart.

Changing the ISBN drops an automatic cover and resets `capa_buscada`; changing the title drops only a `GOOGLE_BOOKS_TITULO` one. A manual cover always stays. `V003` reset `capa_buscada` of books without a cover, so the title search reaches the ones already tried by ISBN.

The Books API has no per-call charge: past the free 1,000 requests/day it answers 429. A book without a cover costs up to two Google requests, so the first pass over the library takes about three days.

`CapaService.gravarBusca` opens its own transaction (`TransactionTemplate`): `buscar` calls it from inside the class, where `@Transactional` wouldn't apply, and the HTTP calls stay outside the transaction.

## Database migrations

PostgreSQL with Flyway, migrations in `src/main/resources/db/migration`.

`spring-boot-flyway` is declared in `pom.xml` and **must stay there**: in Spring Boot 4 the Flyway auto-configuration lives in that module; with only `flyway-core` the service starts and silently applies nothing.

## Security comes from a library

JWT validation, the filter and `@EnableMethodSecurity` arrive from `net.guilhermejr.sistema:seguranca-jwt` through auto-configuration; `@PreAuthorize` only works because of it.

## Configuration comes from outside

`application.yml` only bootstraps `spring.config.import`:

- **Vault** — `secret/application` (shared) and `secret/livros-service` (`eurekaHostname`, `livrosDBHost`, `livrosDBBase`, `livrosDBUser`, `livrosDBPass`, and optionally `googleBooksApiKey`)
- **Config Server** — `livros-service/livros-service.yml` in the config repo: port, context path, datasource, `livros.capas.google-books-chave`

`VAULT_TOKEN` is required and has no default.

## Building and running

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw clean package -DskipTests
VAULT_TOKEN=<token> java -jar target/livros-service-*.jar --spring.profiles.active=dev
```

The config server reads the config repo from GitHub. To run before `livros-service.yml` is pushed, add `--spring.config.additional-location=file:../repositorio-cofiguracao-sistema-prd/livros-service/livros-service.yml`.

`./mvnw test` runs the unit tests (no Spring context, no infrastructure).

## Deploying

`git push origin main` **is** the deploy, through the VPS `post-receive` hook, like the sibling services. The `Dockerfile` only copies a prebuilt jar and has a `HEALTHCHECK` on `/actuator/health`.
