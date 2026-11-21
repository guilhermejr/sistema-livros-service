# livros-service

Microserviço da biblioteca de livros: cadastro com autores, editora, estante e capa, para a aba **Livros** do frontend.

| | |
|---|---|
| Porta | `9013` |
| Context path | `/livros-service` |
| Perfil | `ROLE_LIVROS` |

## Endpoints

| Método | Caminho | O que faz |
|---|---|---|
| `GET` | `/livros` | Todos os livros do usuário, por título. |
| `GET` | `/livros/opcoes` | Países, gêneros, formatos, autores, editoras e estantes para o formulário. |
| `GET` / `PUT` / `DELETE` | `/livros/{id}` | Um livro. |
| `GET` | `/livros/isbn/{isbn}` | Dados para cadastrar pelo ISBN (BrasilAPI, Open Library, Google Books). |
| `POST` | `/livros` | Novo livro. Editora, autores e estante vão pelo nome. |
| `POST` | `/livros/{id}/capa/buscar` | Busca a capa: ISBN no Google Books e na Open Library, depois título e autor no Google Books. |
| `PUT` | `/livros/{id}/capa` | Envia a capa (multipart, campo `arquivo`, até 5 MB). |
| `DELETE` | `/livros/{id}/capa` | Remove a capa. |
| `GET` | `/capas/{id}` | A imagem da capa, sem token. |

Uma tarefa busca, aos poucos, a capa dos livros que ainda não tiveram busca; precisa de `googleBooksApiKey` no Vault.

## Rodando

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw clean package -DskipTests
VAULT_TOKEN=<token> java -jar target/livros-service-*.jar --spring.profiles.active=dev
```

Precisa de `secret/livros-service` no Vault e de `livros-service/livros-service.yml` no repositório de configuração. Detalhes em `CLAUDE.md`.
