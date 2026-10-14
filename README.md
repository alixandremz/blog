# blog

API REST de **blog** feita para **estudar Spring Boot e Spring Web**.
Foco deste projeto: **relacionamentos com coleções** (`@OneToMany` e `@ManyToMany`) e,
principalmente, entender e **evitar o problema N+1**.

Tem três entidades: **Post**, **Tag** e **Comentario**. Um post tem várias tags (e uma tag
aparece em vários posts) e vários comentários.

---

## O que estou estudando aqui

| Área | Conceitos praticados |
|------|----------------------|
| `@OneToMany` | post → comentários, com `cascade = ALL` e `orphanRemoval = true` |
| `@ManyToMany` | post ↔ tag, com tabela de junção `post_tag` |
| N+1 | por que acontece, como enxergar no log SQL e como resolver com `@BatchSize` |
| Carregamento | `LAZY` (padrão das coleções) e por que `open-in-view=false` deixa isso explícito |
| Spring Web (REST) | `@RestController`, `@GetMapping`/`@PostMapping`/`@DeleteMapping`, `@PathVariable`, `@RequestBody`, `@RequestParam`, `ResponseEntity`, códigos HTTP (200, 201, 204, 400, 404, 409) |
| Spring Data JPA | `JpaRepository`, query derivada `findByTagsNomeIgnoreCase`, `existsBy`/`findBy` |
| Paginação | `Pageable`, `PageRequest`, `Sort`, `Page`, invólucro `PaginaResponse` |
| Bean Validation | `@NotBlank`, `@Size`, `@Valid`, `@Min`/`@Max`, inclusive validação de **elementos de coleção** |
| Tratamento de erros | `@RestControllerAdvice`, `@ExceptionHandler`, `409` para violação de banco |
| DTOs | `record` para versão "resumo" (lista) e "detalhe", separadas da entidade |

---

## Stack

- Java 21
- Spring Boot 4.1
- Spring Web MVC, Spring Data JPA, Bean Validation
- H2 em memória (com SQL logado)
- Maven (via Maven Wrapper `./mvnw`, não precisa ter Maven instalado)

---

## Modelo de dados

```
Post ──(um-para-muitos)──► Comentario        (tabela comentarios, coluna post_id)
  └──(muitos-para-muitos)──► Tag             (tabela de junção post_tag)
```

- **Post**: `id`, `titulo`, `conteudo`, `autor`, `criadoEm`, `atualizadoEm`, `tags`, `comentarios`.
- **Tag**: `id`, `nome` (único). Criada automaticamente ao criar um post com uma tag nova.
- **Comentario**: `id`, `post`, `autor`, `texto`, `criadoEm`.

---

## Endpoints

| Método | Caminho | Descrição | Sucesso | Erros |
|--------|---------|-----------|---------|-------|
| GET | `/posts` | lista paginada (resumo); filtro opcional por `tag` | `200` | `400` |
| GET | `/posts/{id}` | post completo, com tags e comentários | `200` | `404` |
| POST | `/posts` | cria um post (tags novas são criadas) | `201` | `400`, `409` |
| POST | `/posts/{id}/comentarios` | adiciona um comentário | `201` | `400`, `404` |
| DELETE | `/posts/{id}/comentarios/{comentarioId}` | remove um comentário | `204` | `404` |
| GET | `/tags` | lista todas as tags | `200` | |

### Criar post

```bash
curl -X POST http://localhost:8080/posts \
  -H 'Content-Type: application/json' \
  -d '{"titulo":"Começando com Spring Boot","conteudo":"texto do post","autor":"Ana","tags":["spring","java"]}'
```

Tags que ainda não existem são criadas; as que já existem são reaproveitadas.

### Listar com paginação e filtro por tag

| Parâmetro | Padrão | Regra |
|-----------|--------|-------|
| `tag` | (vazio) | filtra posts que tenham essa tag (sem diferenciar maiúsculas) |
| `pagina` | `0` | `>= 0` |
| `tamanho` | `10` | entre `1` e `100` |

```bash
curl "http://localhost:8080/posts?tag=java&pagina=0&tamanho=10"
```

```json
{
  "conteudo": [
    { "id": 1, "titulo": "Começando com Spring Boot", "autor": "Ana",
      "criadoEm": "2026-10-14T18:00:00Z", "tags": ["java", "spring"], "totalComentarios": 2 }
  ],
  "pagina": 0, "tamanho": 10, "total": 1, "totalPaginas": 1
}
```

### Ver um post

```bash
curl http://localhost:8080/posts/1
```

```json
{
  "id": 1, "titulo": "Começando com Spring Boot", "conteudo": "texto do post", "autor": "Ana",
  "tags": ["java", "spring"],
  "comentarios": [ { "id": 1, "autor": "João", "texto": "bom post", "criadoEm": "..." } ]
}
```

### Comentar e remover comentário

```bash
curl -X POST http://localhost:8080/posts/1/comentarios \
  -H 'Content-Type: application/json' \
  -d '{"autor":"João","texto":"bom post"}'

curl -i -X DELETE http://localhost:8080/posts/1/comentarios/1
# HTTP/1.1 204 No Content
```

---

## O problema N+1 (o coração deste projeto)

O **N+1** acontece quando você carrega uma lista de N entidades e, para cada uma, o
Hibernate dispara **mais uma consulta** para buscar uma coleção. No fim, em vez de 1
consulta, você fez `1 + N`.

Este projeto deixa o SQL ligado (`spring.jpa.show-sql=true`) justamente para você ver isso.
Exemplo do que **seria** um N+1 na listagem:

```sql
select * from posts limit 10;                    -- 1 consulta
select * from post_tag where post_id = 1;        -- +1 por post
select * from post_tag where post_id = 2;        -- +1 por post
select * from post_tag where post_id = 3;        -- ...
select * from comentarios where post_id = 1;     -- +1 por post
select * from comentarios where post_id = 2;     -- ...
```

### Como foi resolvido aqui: `@BatchSize`

As coleções `tags` e `comentarios` do `Post` estão anotadas com
`@BatchSize(size = 30)`. Em vez de buscar a coleção **post a post**, o Hibernate busca de
**lote em lote**: ele junta os ids dos posts da página e faz **uma** consulta `where
post_id in (...)` para cada 30 posts.

```java
@ManyToMany
@BatchSize(size = 30)
private Set<Tag> tags = new LinkedHashSet<>();

@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
@BatchSize(size = 30)
private List<Comentario> comentarios = new ArrayList<>();
```

Resultado: uma página de 10 posts deixa de ser `1 + 20` consultas e passa a ser algo como
**3 consultas** (a página + um lote de tags + um lote de comentários).

### Outras formas de resolver (e por que não usei aqui)

- **`JOIN FETCH` / `@EntityGraph`**: traz tudo numa consulta só. Funciona muito bem para
  **um** post (`findById`), mas combinado com **paginação** o Hibernate avisa
  (`HHH90003004`) que paginou em memória. Além disso, tentar buscar **duas coleções** de uma
  vez com `join fetch` estoura `MultipleBagFetchException`. Por isso o `@BatchSize` é uma
  escolha mais simples e segura para a listagem paginada.
- **Projeção/DTO direto na query**: buscar só o que a tela precisa em uma consulta. Ótimo em
  produção, mas menos didático no começo.

> Dica de estudo: apague o `@BatchSize`, rode os testes com `show-sql` ligado e conte as
> consultas. Depois devolva a anotação e conte de novo.

---

## Decisões e detalhes técnicos

- **Tags reaproveitadas.** Ao criar um post, cada tag é buscada por `findByNomeIgnoreCase`;
  se não existe, é criada. A coluna `nome` é `unique`, e uma violação vira `409`.
- **`cascade` + `orphanRemoval`.** O post é dono da coleção de comentários. Remover um
  comentário de `post.getComentarios()` faz o Hibernate **apagar a linha** do banco
  (orphanRemoval); apagar o post apaga os comentários junto (cascade).
- **`open-in-view=false`.** O carregamento preguiçoso acontece só dentro da transação do
  service. Isso evita o "funciona na minha máquina" de acessar lazy fora da transação e
  deixa o N+1 visível no lugar certo.
- **Dois DTOs de post.** `PostResumo` (lista, sem conteúdo) e `PostDetalhe` (tudo). Assim a
  listagem não paga o custo de trazer o corpo dos posts.
- **Validação de elementos de coleção.** As tags usam `Set<@NotBlank @Size(max=30) String>`,
  ou seja, cada tag é validada individualmente além do limite de 10 tags.

---

## Como rodar

Usa H2 em memória; os dados somem ao parar a aplicação.

```bash
cd blog
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. O SQL aparece no console. Console do H2 em
`http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:blog`, usuário `sa`, sem senha).

Também há um `Dockerfile` (multi-stage, usuário sem privilégios):

```bash
docker build -t blog .
docker run -p 8080:8080 blog
```

---

## Testes

```bash
./mvnw test
```

Cobrem:

- criação de post com tags novas (e reaproveitamento de tag existente);
- busca de post inexistente (`404`);
- listagem paginada com tags e **contagem de comentários**;
- filtro por tag (sem diferenciar maiúsculas);
- adicionar comentário e remover comentário (`204` + lista vazia depois);
- comentar post inexistente (`404`);
- título em branco → `400`.

---

## Próximos passos (ideias para continuar estudando)

- Medir o N+1 com estatísticas do Hibernate e um teste que conta as queries.
- Buscar um post com `@EntityGraph` (join fetch) e comparar com o `@BatchSize`.
- Ordenação configurável e busca por texto no título/conteúdo.
- Trocar H2 por **PostgreSQL** + `docker-compose` e usar **Testcontainers** nos testes.
- Migrations com **Flyway**.
- **Spring Security**: só o autor pode editar/apagar o próprio post.
