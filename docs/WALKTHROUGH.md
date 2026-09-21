# Done n Dusted — Implementation Walkthrough

This document explains **every** decision taken while building the LD1 REST API, in the order
the work happened. One section per branch. Read it top to bottom and you will be able to defend
any line of this codebase.

Scope reminder: LD1 only asks for **the REST API layer** — 15 methods over 3 domain objects in a
`Guild → Quest → LogEntry` hierarchy, an OpenAPI spec, a working PostgreSQL database with
meaningful seed data, and a demo. Authentication (JWT), roles, friendships, the character sheet
and the React front-end are described in the LD0 report but are **deliberately not implemented
yet** — they belong to LD2/LD3. Everything below is designed so those can be added without
rewriting what exists.

---

## Branch 1 — `feat/project-setup`

### 1.1 What the environment forced

Before writing any code I checked what the machine actually has:

| Tool | Result |
| --- | --- |
| Java | 21.0.12 (Temurin, LTS) — matches the LD0 report |
| Maven | **not installed** |
| Docker | **not installed** |
| PostgreSQL | **not installed**, nothing listening on 5432 |
| Network | reachable (Maven Central answers 200) |

Two of these are problems, because the lab requires *"paruošti programavimo aplinką leidžiančią
atsiskaitymo metu patogiai paleisti ir pademonstruoti programą"* — the grader must be able to
start the thing without a 40-minute setup.

### 1.2 Decision: Maven Wrapper instead of installing Maven

I generated `mvnw` / `mvnw.cmd` with `mvn wrapper:wrapper -Dtype=only-script`.

* `only-script` means **no `maven-wrapper.jar` is committed** — the script downloads the Maven
  distribution itself the first time it runs. Fewer binaries in git, nothing to review.
* Anyone who clones the repo runs `./mvnw` and gets exactly Maven 3.9.11. No "works on my
  machine".
* `.mvn/wrapper/maven-wrapper.properties` pins the distribution URL, so the build is reproducible.

### 1.3 Decision: how PostgreSQL gets provided

The LD0 report commits to PostgreSQL, so switching to H2 "because it is easier" would be a
regression against my own specification. But there is no Docker and no local server here.

The solution is **two profiles**:

* **`dev` (default)** — `EmbeddedPostgresConfig` starts a *real* PostgreSQL 18 server as a child
  process using `io.zonky.test:embedded-postgres`. The binaries are downloaded once from Maven
  Central and cached. It listens on **port 55432**, so you can also connect to it with DBeaver /
  psql during the defence and show the tables. This is genuine PostgreSQL — not an emulation,
  not H2-in-postgres-mode — so every Postgres-specific thing (sequences, `TIMESTAMPTZ`, Flyway's
  postgres dialect) behaves exactly as it will in production.
* **`prod`** — `application-prod.yml` takes a normal JDBC URL from environment variables
  (`DB_URL`, `DB_USER`, `DB_PASSWORD`), defaulting to `localhost:5432/donendusted`. This is the
  profile Render will use, and the one that works with the provided `docker-compose.yml`.

Why `spring.profiles.default: dev` rather than `spring.profiles.active: dev`? `default` only
applies when *no* profile was chosen. Setting `active` in `application.yml` would silently
override `--spring.profiles.active=prod` in some launch setups. `default` is the safe form:
zero-config start for the demo, but explicit profiles still win.

`docker-compose.yml` is kept even though Docker is not installed here — it documents the real
deployment shape, and it is one command for anyone who does have Docker.

### 1.4 Decision: Flyway, and `ddl-auto: validate`

`spring.jpa.hibernate.ddl-auto` is set to **`validate`**, not `update`.

* `update` lets Hibernate silently invent the schema. You never know what actually got created,
  it cannot express constraints the lab needs (`ON DELETE CASCADE`, check constraints, unique
  indexes), and it cannot seed data.
* With Flyway the schema is an explicit, reviewable SQL file under version control, and
  `validate` makes the application **refuse to start** if the entities and the tables ever drift
  apart. That is exactly the failure you want — loud, at boot, not silently at runtime.
* Seed data ("prasmingi duomenys" for the defence) is just another migration, so a fresh
  `dev` run always comes up with a populated database.

### 1.5 Other settings worth knowing

* `spring.jpa.open-in-view: false` — the default (`true`) keeps a database session open for the
  whole HTTP request so that lazy associations can be loaded during JSON serialisation. That
  hides N+1 queries and leaks persistence concerns into the web layer. With it off, anything not
  loaded inside the service throws, which forces the mapping to DTOs to be honest.
* `jackson.default-property-inclusion: non_null` — `null` fields are dropped from responses, so
  payloads stay clean.
* `hibernate.jdbc.time_zone: UTC` — all timestamps are stored and compared in UTC; no surprises
  when the server and the grader are in different zones.
* `app.pagination.*` — configured, not hard-coded, because the list endpoints must be paginated
  and a magic `20` scattered across three controllers is how you end up with three different
  page sizes.

### 1.6 Layout

```
src/main/java/lt/ktu/donendusted/
  DoneNDustedApplication.java   application entry point
  config/                       infrastructure wiring (embedded DB, OpenAPI)
src/main/resources/
  application.yml               shared configuration
  application-prod.yml          real-PostgreSQL profile
  db/migration/                 Flyway schema + seed data
```

Package root is `lt.ktu.donendusted`; the group id is `lt.ktu`. The following branches add
`domain/`, `repository/`, `service/`, `web/` and `web/dto/` under the same root.

---

## Branch 2 — `feat/domain-model`

### 2.1 The three domain objects

The lab requires three objects in a **parent → child → grandchild** hierarchy. From the LD0
report that is:

```
Guild (1:N) → Quest (1:N) → LogEntry
```

* **Guild** — a group/team. Top of the chain, has no parent.
* **Quest** — a concrete goal inside one guild. Declares which of the six DnD stats it grows,
  its type and its difficulty.
* **LogEntry** — one claim of progress against one quest, with optional evidence, reviewed by
  the DM.

### 2.2 What was deliberately left out

`User`, `GuildMembership` and `Friendship` from the LD0 report are **not** implemented in LD1,
and `LogEntry` has **no author column**. This is a scope decision, not an oversight:

* LD1 has no authentication, so there is no "current user" to attach an entry to.
* Adding a `player_name VARCHAR` placeholder now would be a throwaway column that LD2 would have
  to drop and replace with a real foreign key — a migration that exists only to undo a shortcut.
* Every rule in the report that does not need identity *is* implemented (XP derivation, frozen
  XP, PENDING earns nothing, parent-chain access). Only the rules that are literally
  "who is allowed to do this" are deferred, because they are authorisation rules and LD2 is the
  authorisation lab.

The same applies to `SIDE_QUEST` being DM-only, "an entry cannot be approved by its own author"
and the one-DM-per-guild rule: all three are checks on *who* is calling, so they arrive together
with JWT in LD2.

### 2.3 Enums, and why `Difficulty` carries behaviour

`Stat`, `QuestType`, `Difficulty`, `LogStatus` are Java enums stored as `VARCHAR` with
`@Enumerated(EnumType.STRING)`.

* **String, not ordinal.** `EnumType.ORDINAL` stores `0,1,2…`; inserting a new constant in the
  middle later silently reinterprets every existing row. Strings are self-describing in the
  database and readable during the defence.
* Each enum column also has a **`CHECK` constraint** in SQL listing the allowed values. That
  means the rule survives even if someone writes to the database with psql instead of the API.

`Difficulty` is not a bare list — it holds the multiplier:

```java
TRIVIAL(1), EASY(2), MEDIUM(3), HARD(5), LEGENDARY(8);
public static final int BASE_XP = 10;
public int xpValue() { return BASE_XP * multiplier; }
```

The report says *"Sudėtingumas yra bazinio XP daugiklis"* and *"XP niekada nepriimamas iš kliento
— serveris jį išveda iš užduoties sudėtingumo."* Putting the multiplier on the enum means there
is exactly **one** place in the codebase that knows what a quest is worth, and it is impossible
for a controller to invent a different number.

### 2.4 Rules pushed down into the database

`V1__schema.sql` does more than declare columns:

| Constraint | Requirement it enforces |
| --- | --- |
| `uq_guild_name` | Guild names are unique — makes 409 on duplicate meaningful |
| `uq_quest_guild_title` | Two quests in the same guild cannot share a title (different guilds can) |
| `ON DELETE CASCADE` on both FKs | Deleting a parent removes its children; no orphan rows |
| `ck_quest_target_count` | `target_count` is either absent or positive |
| `ck_log_pending_is_empty` | A `PENDING` entry has 0 XP, 0 inspiration and no review timestamp |
| `ck_log_reviewed_at` | Anything not `PENDING` **must** carry a review timestamp |
| `ck_log_xp` / `ck_log_inspiration` | Never negative |

`ck_log_pending_is_empty` is the database-level version of *"įrašas atsiranda būsenoje PENDING ir
neduoda jokių taškų"*. The service layer enforces it too, but belt and braces: a rule that only
lives in Java is a rule that a future bug can bypass.

### 2.5 Entity design choices

* **`@ManyToOne(fetch = LAZY)`** on both child entities. The JPA default for `@ManyToOne` is
  `EAGER`, which means loading one `LogEntry` silently joins `Quest` and then `Guild`. Lazy plus
  `open-in-view: false` makes every fetch deliberate.
* **No `@OneToMany` collections.** `Guild` does not hold a `List<Quest>`. Children are always
  reached through their repository with a page and a filter, so a mapped collection would only
  invite someone to load 5000 log entries into memory. The cascade that matters (delete) is
  handled by the database.
* **`protected` no-arg constructors.** JPA requires one; making it `protected` stops application
  code from creating half-built entities while Hibernate can still use it.
* **Setters only where mutation is legal.** `id`, `createdAt`, the parent reference and
  `status` / `xpAwarded` have **no** setters. A log entry's outcome can only change through
  `approve()`, `reject()` or `resetToPending()`, which keep status, XP, inspiration and
  `reviewedAt` consistent as a unit. This is what stops the `ck_log_*` constraints from ever
  being violated: the illegal combinations are unreachable from Java.
* **`@CreationTimestamp`** rather than a database `DEFAULT now()` alone — the value is then
  available on the object straight after `save()`, without a re-read. The SQL default stays as a
  safety net for manual inserts.
* **`Instant`, not `LocalDateTime`.** Columns are `TIMESTAMPTZ` and `hibernate.jdbc.time_zone` is
  UTC, so an instant means the same moment everywhere.

### 2.6 Repositories

Three Spring Data interfaces, no implementations.

The important detail is that the child repositories **never look up by id alone**:

```java
Optional<Quest> findByIdAndGuildId(Long id, Long guildId);
Optional<LogEntry> findByIdAndQuestIdAndQuestGuildId(Long id, Long questId, Long guildId);
```

The report requires *"Vaikinis objektas pasiekiamas tik per tėvinį … neteisinga grandinė - 404"*.
If the repository only offered `findById`, every controller would have to remember to compare the
parent id afterwards, and one forgotten check leaks another guild's data. By making the full
chain part of the query, a mismatched path simply returns an empty `Optional`, which becomes a
404. The rule is enforced by the *only* method that exists.

Filtering uses one JPQL query per resource with `(:param is null or field = :param)`. Compared to
JPA `Specification`s this is a few lines instead of a criteria-builder class per filter, and the
set of filters here is small and fixed. If the filter list ever grows, that is the moment to
switch — not before.

### 2.7 Seed data

`V2__seed_data.sql` inserts 3 guilds, 9 quests and 12 log entries. It is written so that the
defence has something real to show:

* All four quest types and several difficulties appear, including a `GRIND` quest with a
  `target_count` and an inactive quest (so the `active` filter has something to filter).
* Log entries cover **all three statuses**, with and without `evidence_url`, with and without a
  reviewer note, and several with `inspiration_awarded = 1`.
* Approved XP generally matches `BASE_XP × multiplier` for that quest's difficulty — with one
  deliberate exception, because the DM is allowed to override the score at approval time.
* Rows are linked with `(select id from guild where name = …)` instead of hard-coded ids, so the
  script does not break if sequences start elsewhere.

### 2.8 The bug this branch hit

The first boot failed with `Schema validation: missing table [guild]` — Flyway never ran. Cause:
Spring Boot 4 split auto-configuration out of `spring-boot-autoconfigure` into per-technology
modules, so a bare `flyway-core` dependency gives you the library but **not** the
auto-configuration. Fix: depend on `spring-boot-starter-flyway`. Worth remembering, because the
same trap applies to any Boot-3 tutorial followed while on Boot 4.

---

## Branch 3 — `feat/api-foundation`

Nothing in this branch is a REST endpoint. It is the three things every endpoint needs, built
once so the three controller branches after it stay short: a uniform error body, a uniform paged
list body, and the OpenAPI document metadata.

### 3.1 `ApiError` — one error shape for everything

The report requires *"Klaidos grąžinamos vienoda struktūra (ApiError) su prasmingu HTTP kodu."*

```json
{
  "timestamp": "2026-09-21T09:15:30.123Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "GRIND tipo uzduotis privalo tureti targetCount",
  "path": "/api/guilds/2/quests",
  "details": [ { "field": "title", "message": "must not be blank" } ]
}
```

* `details` is **only** present when the error is about specific fields — `jackson.default-property-inclusion: non_null` drops it otherwise, so a 404 body is not padded with `"details": null`.
* `error` is always the standard HTTP reason phrase, taken from `HttpStatus`, so it can never
  drift from `status`.
* `path` comes from the actual request URI, which makes a failing Postman run self-explanatory.

Spring has a built-in RFC 7807 `ProblemDetail`, and in a greenfield project it would be a
reasonable default. It is **not** used here because the LD0 specification names `ApiError` and
lists its role explicitly. Following my own written spec beats following a convention the spec
did not choose.

### 3.2 One handler, no error handling in controllers

`ApiExceptionHandler` is a single `@RestControllerAdvice`. No controller builds an error
response; controllers only ever return the happy path. That is what makes "uniform structure"
actually true rather than aspirational.

Three custom exceptions, and the reason each HTTP code was picked:

| Exception | Code | When |
| --- | --- | --- |
| `ResourceNotFoundException` | **404** | id does not exist, **or** the parent chain in the URL does not match |
| `ConflictException` | **409** | request is valid but fights the current state (duplicate guild name) |
| `BusinessRuleException` | **422** | JSON parsed, types correct, but the *content* breaks a domain rule |

And the framework exceptions that are translated:

| Exception | Code | Why |
| --- | --- | --- |
| `MethodArgumentNotValidException` | 400 | `@Valid` failed on the body — the payload does not match the declared schema |
| `HandlerMethodValidationException`, `ConstraintViolationException` | 400 | same, for query/path parameters |
| `HttpMessageNotReadableException` | 400 | malformed JSON, unknown enum value, unparseable timestamp |
| `MethodArgumentTypeMismatchException` | 400 | `/api/guilds/abc` — `abc` is not a `Long` |
| `NoResourceFoundException` | 404 | a URL the API does not define at all |
| `HttpRequestMethodNotSupportedException` | 405 | right URL, wrong verb |
| `DataIntegrityViolationException` | 409 | a database constraint caught what a check missed |
| `Exception` | 500 | last resort — logged with a stack trace, but the client gets a generic message |

**The 400-vs-422 line, because the lab asks for both.** The rule used consistently across this
codebase:

> **400** = "I could not understand your request." Broken syntax, wrong type, a field that is
> missing or violates its own declared constraint (`@NotBlank`, `@Size`, `@Positive`).
> **422** = "I understood you perfectly, and the answer is no." Everything parsed and every field
> is individually legal, but the combination breaks a rule of the domain — a `GRIND` quest with
> no `targetCount`, a log entry dated in the future, approving an entry with negative XP.

Put differently: a 400 can be fixed by looking only at the request; a 422 needs knowledge of the
business rules. This is the distinction demonstrated during the defence.

The `DataIntegrityViolationException` handler deserves a note: every uniqueness rule is *also*
checked in the service before insert, which is what produces the nice 409 message. The handler is
there for the race where two requests pass that check simultaneously and the database unique index
is what actually stops the second one. Without it, that race returns 500. With it, both callers
get the same 409.

### 3.3 `PagedResponse` — lists with navigation links

The report requires *"Sąrašai puslapiuojami, filtruojami ir grąžina self/first/prev/next/last
nuorodas, išsaugančias filtrus."*

```json
{
  "content": [ ... ],
  "page":  { "number": 1, "size": 20, "totalElements": 42, "totalPages": 3 },
  "links": {
    "self":  "http://localhost:8080/api/guilds?name=iron&page=1&size=20",
    "first": "http://localhost:8080/api/guilds?name=iron&page=0&size=20",
    "prev":  "http://localhost:8080/api/guilds?name=iron&page=0&size=20",
    "next":  "http://localhost:8080/api/guilds?name=iron&page=2&size=20",
    "last":  "http://localhost:8080/api/guilds?name=iron&page=2&size=20"
  }
}
```

Two decisions:

1. **Links are built from the current request URI**
   (`ServletUriComponentsBuilder.fromCurrentRequest()`), then only `page` and `size` are
   replaced. Every other query parameter — `name`, `stat`, `status`, `sort` — survives untouched.
   That is precisely the "išsaugančias filtrus" requirement, and it keeps working automatically
   when a new filter is added later, because the link builder never enumerates the filters.
2. **`prev` and `next` are `null` when they do not exist**, and are therefore omitted from the
   JSON. A client can test "is there a next page?" by asking whether the link is there, instead
   of comparing page numbers. `first`, `self` and `last` are always present — on an empty result
   all three point at page 0.

`PagedResponse.from(page, mapper)` takes the entity `Page` and a mapping function, so the
conversion to DTO and the link building happen in one call. Controllers stay one line.

Raw Spring Data `Page` is deliberately **not** returned. Its JSON serialisation is unstable
across versions (Boot even warns about it), it exposes `pageable`, `sort` and `numberOfElements`
internals the API never promised, and it carries no links.

### 3.4 Pagination input

Controllers take Spring's `Pageable` argument and the defaults live in configuration:

```yaml
spring.data.web.pageable:
  default-page-size: 20
  max-page-size: 100
  one-indexed-parameters: false
```

* `max-page-size: 100` means `?size=100000` silently becomes 100 instead of trying to serialise
  the whole table. A denial-of-service in one line of YAML.
* `one-indexed-parameters: false` keeps `page=0` as the first page, matching what the links emit.
* `sort` support (`?sort=name,asc`) comes free with the resolver.

### 3.5 OpenAPI document metadata

`OpenApiConfig` only sets the document-level `info` and `servers`. Every operation, parameter,
status code and schema is annotated at the place it is defined, in the following branches — a
hand-maintained central description would be out of date within a week.

`springdoc-openapi` generates `/v3/api-docs` (JSON) and serves Swagger UI at `/swagger-ui.html`.
The spec is generated from the code, so it cannot describe an endpoint that does not exist.

### 3.6 The bug this branch hit

`HandlerMethodValidationException.getAllValidationResults()` does not exist in Spring Framework 7
— it is now `getParameterValidationResults()`. Another Boot 4 / Spring 7 rename to watch for.

---

## Branch 4 — `feat/guild-api`

The first five of the fifteen methods. This branch also fixes the layering for the two branches
that follow, so it is longer than they will be.

### 4.1 The five endpoints

| # | Method | Path | Success |
| --- | --- | --- | --- |
| 1 | `GET` | `/api/guilds` | 200 — paged, filtered list |
| 2 | `GET` | `/api/guilds/{guildId}` | 200 |
| 3 | `POST` | `/api/guilds` | **201** + `Location` header |
| 4 | `PUT` | `/api/guilds/{guildId}` | 200 — updated resource in the body |
| 5 | `DELETE` | `/api/guilds/{guildId}` | **204** — no body |

Why those particular codes:

* **201 with `Location`.** The lab explicitly asks that creation returns 201. The `Location`
  header is built from the actual request URI plus the new id, so the client is told where the
  thing it just made lives. The created resource is also in the body, which saves the client a
  follow-up GET.
* **200 on PUT, 204 on DELETE.** The lab says *"Kai resursas ištrinamas - 200/204 priklausomai,
  ar grąžinamas response body"*. There is nothing meaningful to return after a delete, so the
  honest answer is 204 No Content. PUT does have something to return — the new state, including
  fields the server owns like `createdAt` — so it is 200 with a body.

### 4.2 Three layers, and what each is allowed to do

```
GuildController  ── HTTP only: routing, status codes, Location header, DTO ↔ JSON
      │
GuildService     ── transactions + business rules; speaks entities
      │
GuildRepository  ── queries only
```

* The **controller** never touches the repository and never builds an error response. Every
  method body is one or two lines.
* The **service** is where `@Transactional` lives, and it is the only place that decides a rule
  has been broken. Reads are `@Transactional(readOnly = true)`, which lets the driver and
  Hibernate skip dirty-checking and flushing.
* The **repository** has no logic at all.

### 4.3 DTOs, not entities, on the wire

`GuildRequest` (in) and `GuildResponse` (out) are separate Java records. Entities are never
serialised directly. Three concrete reasons, not style:

1. **The client must not be able to set server-owned fields.** `GuildRequest` has no `id` and no
   `createdAt`, so a request that includes them cannot do anything. If the entity were bound
   directly, a `PUT` carrying `"id": 7` would be a small privilege-escalation bug.
2. **Lazy associations.** Serialising an entity with a lazy `@ManyToOne` outside a transaction
   throws. A DTO is already fully materialised.
3. **The API shape can stay stable while the schema changes.** They are allowed to diverge.

The mapping is a single static factory, `GuildResponse.from(guild)`. No MapStruct, no
ModelMapper: three entities with a handful of fields do not justify a code generator or a
reflection-based mapper, and a static method is greppable and steps through in a debugger.

Validation annotations sit on `GuildRequest` (`@NotBlank`, `@Size(min = 3, max = 60)`) and the
controller marks the body `@Valid`. That is what produces the 400 with per-field `details`.
Note that the constraint mirrors the SQL column definition (`varchar(60)`) — the same limit is
stated once for the client, once for the database.

### 4.4 Uniqueness: checked in the service, guaranteed by the database

`create` and `update` both ask `existsByNameIgnoreCase…` before saving, and throw
`ConflictException` → 409 with a readable message. The `uq_guild_name` index is what actually
guarantees it; the service check exists to produce a good error message instead of a raw
constraint violation.

`update` uses `existsByNameIgnoreCaseAndIdNot(name, id)` — without the `AndIdNot`, saving a guild
without changing its name would find *itself* and report a conflict.

### 4.5 Delete cascades through the database

`GuildService.delete` just deletes the guild. There is no `@OneToMany(cascade = REMOVE)` and no
manual "first delete the log entries, then the quests" loop. The `ON DELETE CASCADE` on the two
foreign keys does it in one statement inside the same transaction. Fewer round trips, and it is
impossible to leave orphans behind even if someone deletes a row with psql.

### 4.6 Two bugs this branch hit — both worth understanding

**(a) `function lower(bytea) does not exist`.**
The original filter query was JPQL of the form:

```sql
where (:name is null or lower(g.name) like lower(concat('%', :name, '%')))
```

This is the standard "optional filter" trick and it works on many databases. On PostgreSQL it
fails: when the parameter is `null`, the driver sends an untyped parameter, Postgres cannot infer
its type from `? IS NULL`, and it guesses `bytea`. Adding `cast(:name as String)` did not help
either.

The fix was to stop expressing the optionality in SQL and express it in Java, with a
**`Specification`**:

```java
Specification<Guild> spec = (root, query, cb) -> StringUtils.hasText(nameFilter)
        ? cb.like(cb.lower(root.get("name")), "%" + nameFilter.toLowerCase() + "%")
        : cb.conjunction();
return guilds.findAll(spec, pageable);
```

When no filter is supplied, the predicate is simply **not added to the query** — there is no null
parameter to type in the first place. `cb.conjunction()` is the always-true predicate used when
nothing is filtered. The repositories now extend `JpaSpecificationExecutor`, and the same pattern
is used for the quest and log-entry filters in the next branches, where the parameters are enums
and would hit exactly the same problem.

The lesson generalises: *"is this filter present?"* is a Java question, not a SQL question.
Answering it in SQL forces the database to reason about a value that is not really there.

**(b) `@PageableDefault` silently overrode the configured page size.**
`spring.data.web.pageable.default-page-size: 20` was set in `application.yml`, yet responses came
back with `"size": 10`. Cause: `@PageableDefault` has its own `size()` whose default is **10**,
and the annotation wins over the global property. Writing `@PageableDefault(sort = "name")` for
the sort alone quietly changed the page size too.

Fix: state the size in the annotation (`@PageableDefault(size = 20, sort = "name")`) and delete
`default-page-size` from the YAML, so there is exactly one place that defines it. `max-page-size:
100` stays in the YAML, because it is a global safety limit rather than a per-endpoint default —
and it is verified to work: `?size=99999` comes back as `"size": 100`.

A default `sort` is set on every list endpoint on purpose. Without one, PostgreSQL may return
rows in any order, which means page 2 can repeat a row from page 1. Pagination without a
deterministic sort is not pagination.

### 4.7 Verified by hand before merging

```
GET    /api/guilds                        → 200, paged, sorted by name
GET    /api/guilds?name=iron              → 200, one result, filters preserved in links
GET    /api/guilds?size=99999             → 200, size clamped to 100
GET    /api/guilds/1                      → 200
GET    /api/guilds/999                    → 404 ApiError
GET    /api/guilds/abc                    → 400 ApiError ("turi buti Long")
POST   /api/guilds                        → 201 + Location: /api/guilds/4
POST   /api/guilds (duplicate name)       → 409 ApiError
POST   /api/guilds {"name":"ab"}          → 400 ApiError + details[].field = "name"
POST   /api/guilds (truncated JSON)       → 400 ApiError
PUT    /api/guilds/4                      → 200, updated body
DELETE /api/guilds/4                      → 204, empty body
DELETE /api/guilds/4 (again)              → 404 ApiError
PATCH  /api/guilds/1                      → 405 ApiError
```

While checking this, the raw Jackson parse message turned out to be multi-line and full of
internals, so `HttpMessageNotReadableException` now reports only the first line of the cause.

---

## Branch 5 — `feat/quest-api`

Methods 6–10. The first **nested** resource, so this is where the parent-chain rule becomes real.

### 5.1 The five endpoints

All under `/api/guilds/{guildId}/quests`:

| # | Method | Path | Success |
| --- | --- | --- | --- |
| 6 | `GET` | `…/quests` | 200 — paged, four filters |
| 7 | `GET` | `…/quests/{questId}` | 200 |
| 8 | `POST` | `…/quests` | 201 + `Location` |
| 9 | `PUT` | `…/quests/{questId}` | 200 |
| 10 | `DELETE` | `…/quests/{questId}` | 204 |

There is **no** `/api/quests/{id}` shortcut. The report says a child is reachable only through its
parent, so the flat route simply does not exist — not "exists but is hidden".

### 5.2 Three different 404s, and why each is correct

| Request | Response |
| --- | --- |
| `GET /api/guilds/999/quests` | **404** "Guild 999 nerasta" |
| `GET /api/guilds/1/quests/999` | **404** "Quest 999 nerasta grupeje 1" |
| `GET /api/guilds/1/quests/4` where quest 4 belongs to guild 2 | **404** "Quest 4 nerasta grupeje 1" |

The third row is the interesting one. Quest 4 exists, and a naive `findById(4)` would happily
return it — leaking another guild's data through a URL that claims otherwise. Because the
repository only offers `findByIdAndGuildId`, the mismatch produces an empty `Optional` and
therefore a 404. The rule is enforced by the shape of the API, not by a check someone has to
remember to write.

Note that listing the quests of a guild that does not exist is **404, not an empty list**. An
empty `content` array would tell the client "this guild has no quests", which is a different and
false statement. `QuestService.list` therefore calls `guilds.get(guildId)` first.

### 5.3 Filtering with four optional parameters

`stat`, `type`, `difficulty`, `active` — any combination, all optional. The service builds one
`Specification` and adds a predicate only for the parameters that were supplied:

```java
List<Predicate> predicates = new ArrayList<>();
predicates.add(cb.equal(root.get("guild").get("id"), guildId));
if (stat != null)       predicates.add(cb.equal(root.get("stat"), stat));
if (type != null)       predicates.add(cb.equal(root.get("type"), type));
...
return cb.and(predicates.toArray(Predicate[]::new));
```

The guild predicate is not optional — it is what scopes the list to the parent. This is the same
pattern introduced in branch 4 and it is exactly what avoids the PostgreSQL untyped-null problem
described in 4.6(a); with four enum parameters, the JPQL `:p is null` approach would have failed
four times over.

An unknown enum value in a query parameter (`?type=NOPE`) becomes
`MethodArgumentTypeMismatchException` → **400**, handled centrally.

### 5.4 `xpPerCompletion` — showing that the server owns XP

`QuestResponse` carries a field that does not exist in the database:

```java
quest.getDifficulty().xpValue()   // BASE_XP (10) × multiplier
```

The report states *"XP niekada nepriimamas iš kliento… serveris jį išveda iš užduoties
sudėtingumo"*. `QuestRequest` has no XP field at all — there is nothing for a client to send. The
derived value is published in the response so a client can show "this is worth 50 XP" without
duplicating the formula. It is computed on read rather than stored, so it can never go stale.

This pairs with the frozen `xpAwarded` on `LogEntry` in the next branch: the *quest* always
reports its current worth, while an *approved entry* keeps what it was worth on the day it was
approved. Changing a quest from `EASY` to `LEGENDARY` changes future entries only.

### 5.5 Why `targetCount` produces 422 and not 400

`targetCount` is annotated `@Positive`, so `-5` is a **400** — the field is invalid on its own
terms. But these two are **422**:

* `type = GRIND` with no `targetCount` — a cumulative quest with no target to accumulate towards.
* `type = MILESTONE` with a `targetCount` — a one-off quest cannot have a progress target.

Both requests are perfectly well-formed JSON with every field individually legal. They are
rejected because of what the fields mean *together*, which is the 422 case defined in branch 3.
The check lives in `QuestService.validate` and runs on both create and update, because an update
can break the rule just as easily as a create.

A Jakarta `@AssertTrue` on the record could express this too. It is a service-level check instead
so that it produces the same 422 as every other domain rule — routing it through bean validation
would make it a 400 and blur the very line branch 3 drew.

### 5.6 `active` defaults to `true`

`QuestRequest.active` is a `Boolean` (nullable), not a `boolean`. If it were primitive, an
omitted field would silently become `false` and every quest created without mentioning `active`
would be born inactive. As a nullable wrapper, "not sent" is distinguishable from "sent as
false", and `activeOrDefault()` turns the former into `true`.

### 5.7 Verified by hand before merging

```
GET    /api/guilds/2/quests?size=3              → 200, sorted by title
GET    /api/guilds/2/quests?type=GRIND          → 200, totalElements = 1
GET    /api/guilds/2/quests?active=true&size=2  → 200, links keep active=true
GET    /api/guilds/2/quests?type=NOPE           → 400
GET    /api/guilds/999/quests                   → 404 (guild, not empty list)
GET    /api/guilds/1/quests/4                   → 404 (quest 4 belongs to guild 2)
POST   /api/guilds/1/quests                     → 201 + Location, xpPerCompletion = 30
POST   … GRIND without targetCount              → 422
POST   … MILESTONE with targetCount             → 422
POST   … missing "stat"                         → 400 + details[].field = "stat"
POST   … "stat":"LUCK"                          → 400, message lists accepted values
POST   … duplicate title in same guild          → 409
PUT    /api/guilds/1/quests/10                  → 200, xpPerCompletion recalculated to 50
DELETE /api/guilds/1/quests/10                  → 204, then 404 on repeat
```

---

## Branch 6 — `feat/logentry-api`

Methods 11–15, two levels deep: `/api/guilds/{guildId}/quests/{questId}/logs`. This is the exact
path the report names, and it is where the domain rules about XP actually live.

### 6.1 The five endpoints

| # | Method | Path | Success |
| --- | --- | --- | --- |
| 11 | `GET` | `…/logs` | 200 — paged, filter by `status` |
| 12 | `GET` | `…/logs/{logId}` | 200 |
| 13 | `POST` | `…/logs` | 201 + `Location` |
| 14 | `PUT` | `…/logs/{logId}` | 200 — content **and** DM review |
| 15 | `DELETE` | `…/logs/{logId}` | 204 |

The list is sorted by `occurredAt` **descending** by default — a log is read newest-first.

### 6.2 Two request DTOs for one resource, on purpose

This is the only resource with separate `LogEntryCreateRequest` and `LogEntryUpdateRequest`, and
the difference is the whole point:

```java
record LogEntryCreateRequest(String note, String evidenceUrl, Instant occurredAt) { }
```

There is **no `status` and no `xpAwarded` field**. The report says *"įrašas atsiranda būsenoje
PENDING ir neduoda jokių taškų"* and *"XP niekada nepriimamas iš kliento kuriant įrašą"*. Rather
than accepting those fields and then validating them away, the create DTO simply has nowhere to
put them. A client cannot send what the schema does not define — and the generated OpenAPI
document says so too.

`LogEntryUpdateRequest` does have `status`, `xpAwarded`, `inspirationAwarded` and `reviewerNote`,
because `PUT` is the DM review: *"DM peržiūri įrašą ir nustato būseną APPROVED arba REJECTED, gali
perrašyti XP įvertį ir palikti pastabą"*, plus *"Tvirtindamas DM gali skirti inspiration"*.

### 6.3 How approval works

```java
case APPROVED -> {
    int xp = request.xpAwarded() != null ? request.xpAwarded() : quest.getDifficulty().xpValue();
    int inspiration = request.inspirationAwarded() != null ? request.inspirationAwarded() : 0;
    entry.approve(xp, inspiration, request.reviewerNote(), Instant.now());
}
```

* **Omitting `xpAwarded` is the normal case** — the server derives it from the quest's difficulty
  (`BASE_XP × multiplier`). Verified: approving an entry on a `HARD` quest without sending any XP
  produced `"xpAwarded": 50`.
* **Sending `xpAwarded` is the DM override**, which the report explicitly allows *at approval
  time and only then*.
* `reviewedAt` is stamped by the server with `Instant.now()`. It is never taken from the request;
  a client cannot backdate a review.

**The XP is frozen at this moment.** `xpAwarded` is a plain column on `LogEntry`, not a
calculation over the quest. Editing the quest to `LEGENDARY` afterwards changes
`QuestResponse.xpPerCompletion` for future entries, and leaves every already-approved entry
exactly as it was — *"Patvirtintas XP užšaldomas įraše - vėliau pakeitus užduoties sudėtingumą
istorija neperrašoma."* If the value were derived on read, history would silently rewrite itself
every time a DM re-balanced a quest.

### 6.4 The status transitions, and the 422s they produce

`LogEntry` has no `setStatus`. The only ways to change state are `approve()`, `reject()` and
`resetToPending()` — each sets status, XP, inspiration and `reviewedAt` together:

| Target status | What the entity does | Rejected with 422 when |
| --- | --- | --- |
| `APPROVED` | stores XP + inspiration, stamps `reviewedAt` | — |
| `REJECTED` | forces XP and inspiration to 0, stamps `reviewedAt` | the request carries non-zero XP or inspiration |
| `PENDING` | clears XP, inspiration **and** `reviewedAt` | the request carries non-zero XP or inspiration |

The last row matters: sending an entry back for revision has to *remove* the reward, not leave a
pending entry sitting on 50 XP. `resetToPending` nulls `reviewedAt` as well, which is what keeps
the `ck_log_reviewed_at` and `ck_log_pending_is_empty` constraints satisfied. Those two database
checks and these three methods are the same rule expressed twice — deliberately.

Trying to set `PENDING` while sending `xpAwarded: 50` is a 422, not a 400: the JSON is valid and
`50` is a legal integer; the request is refused because of what those fields mean together.

### 6.5 Two more domain rules

* **`occurredAt` cannot be in the future** → 422. Checked on create *and* on update.
* **No new entries on an inactive quest** → 422. `active = false` means the quest is closed;
  existing entries stay readable and reviewable, but nothing new can be logged against it. This
  is why the seed data contains one inactive quest — the rule is demonstrable.

`occurredAt` is `@NotNull` (→ 400 if missing) and the "not in the future" check is in the service
(→ 422). Same field, two different failures, each with the code that matches the reason. That
contrast is worth showing during the defence.

`evidenceUrl` uses Hibernate Validator's `@URL`, so `"not a url"` is a **400** with
`details[0].field = "evidenceUrl"` — it is malformed on its own terms, no domain knowledge
needed.

### 6.6 A LazyInitializationException that was designed away

`LogEntryResponse` originally carried a `guildId`, mapped as:

```java
entry.getQuest().getGuild().getId()
```

`entry.getQuest()` returns a lazy proxy. Calling `getId()` on a proxy is safe — Hibernate answers
from the identifier without going to the database — but calling `getGuild()` **initialises** the
proxy, and with `open-in-view: false` the session is already closed by the time the controller
maps the response. That is a `LazyInitializationException`, i.e. a 500.

Three ways out: map inside the transaction, `join fetch` the guild, or drop the field. The field
was dropped. A log entry is only ever reachable through `/api/guilds/{guildId}/…`, so the client
already knows the guild id — it was duplicated data that cost an extra join. The cheapest fix was
the one that removed something.

(`questId` stays, because that one *is* a proxy id read and costs nothing.)

### 6.7 Verified by hand before merging

```
GET    …/quests/4/logs?size=2              → 200, newest first, all statuses present
GET    …/quests/4/logs?status=PENDING      → 200, totalElements = 1
POST   …/quests/4/logs                     → 201 + Location, status PENDING, xpAwarded 0
PUT    …/logs/13 status=APPROVED (no xp)   → 200, xpAwarded 50 (derived), reviewedAt stamped
PUT    …/logs/13 status=PENDING, xp 50     → 422
POST   …/logs occurredAt in 2030           → 422
POST   …/quests/7/logs (inactive quest)    → 422
POST   …/logs evidenceUrl="not a url"      → 400 + details[].field = "evidenceUrl"
POST   …/logs {}                           → 400 (occurredAt required)
GET    /api/guilds/1/quests/4/logs/13      → 404 (quest 4 is in guild 2)
DELETE …/logs/13                           → 204, then 404 on repeat
```

**All fifteen API methods are now implemented and working.**

---

## Branch 7 — `feat/openapi-and-postman`

No production code. This branch produces the three things the lab grades besides the endpoints
themselves: the OpenAPI specification, the demonstration, and the repository documentation.

### 7.1 OpenAPI: generated, then committed

`springdoc-openapi` builds the document by reflecting over the controllers at runtime, so
`/v3/api-docs` can never describe an endpoint that does not exist. The annotations that feed it
(`@Tag`, `@Operation`, `@ApiResponse`, `@Schema`, `@Parameter`) were written in branches 4–6, next
to the code they describe — that is the only way they stay true.

The generated document was then pretty-printed and committed as
[`docs/openapi.json`](openapi.json). Both forms matter:

* the **live** endpoint is what Swagger UI reads, and it is what you show during the defence;
* the **committed file** is a reviewable artefact — it diffs when the API changes, and the lab
  asks for a specification to be *submitted*, not merely to be reachable while the app runs.

Verification that the spec really covers everything:

```
paths: 6   operations: 15
  GET,POST        /api/guilds
  GET,PUT,DELETE  /api/guilds/{guildId}
  GET,POST        /api/guilds/{guildId}/quests
  GET,PUT,DELETE  /api/guilds/{guildId}/quests/{questId}
  GET,POST        /api/guilds/{guildId}/quests/{questId}/logs
  GET,PUT,DELETE  /api/guilds/{guildId}/quests/{questId}/logs/{logId}
```

Six path templates, **exactly fifteen operations** — matching the fifteen methods the lab
requires, with no extras and none missing. Every operation documents its error codes with
`ApiError` as the response schema, so the uniform error structure is part of the published
contract rather than an implementation detail.

### 7.2 The Postman collection

[`postman/DoneNDusted.postman_collection.json`](../postman/DoneNDusted.postman_collection.json) —
26 requests, 53 assertions, **5.2 seconds** end to end (the lab asks for roughly 15).

Three properties were designed in deliberately:

1. **It is re-runnable.** A pre-request script stamps the guild name with `Date.now()`, so the
   409-duplicate test never collides with a previous run, and the last four requests delete
   everything the run created. You can run it ten times in a row during the defence without
   resetting the database.
2. **Ids are never hard-coded.** Each create captures the id from the response into a collection
   variable (`guildA`, `guildB`, `questId`, `logId`) and later requests use it. The collection
   does not care what is already in the database.
3. **The assertions check meaning, not just status codes.** For example request 19 asserts
   `xpAwarded === 80` after approving without sending any XP — which is the *"XP niekada
   nepriimamas iš kliento"* rule, demonstrated rather than claimed.

Ordering is the story of the domain: create a guild → create a quest in it → log progress →
DM approves → clean up. Interleaved with it are the error cases the lab lists explicitly:

| Requirement from the lab | Request | Result |
| --- | --- | --- |
| resource created → 201 | 01, 09, 15 | `201` + `Location` |
| resource deleted → 200/204 | 21, 23, 24 | `204`, empty body asserted |
| resource not found → 404 | 08, 13, 22, 26 | `404` `ApiError` |
| bad payload → 400/422 | 07, 10, 16, 20 | `400` with `details[].field`; `422` for rule violations |

Request 13 is the one worth pausing on during the defence: it asks for a quest that **does
exist**, through a guild that does **not own it**, and gets `404 … nerasta grupeje` — the
parent-chain rule from the report, demonstrated live.

A second guild (request 02) exists purely to make that test possible without depending on seed
data.

Newman was used to verify the whole thing actually passes rather than assuming it does:

```
requests 26 / failed 0     assertions 53 / failed 0     duration 5.2s
```

### 7.3 README

[`README.md`](../README.md) is written **in Lithuanian**, unlike this walkthrough. Different
audiences: the README is the document the reviewer opens first, and it is the "dokumentacija
projekto Git saugykloje" the lab asks for; this walkthrough is written for you, to explain the
reasoning.

The README covers: one-command start, all fifteen methods in three tables, the paged-response and
`ApiError` shapes, the 400-vs-422 rule, how to run the Postman demo, the domain rules, the tech
stack, the project layout, and — deliberately — a closing section stating what is **not** in LD1
and why. Writing down the scope boundary is not an apology; it is what stops a reviewer from
reading a missing feature as a missing requirement.

### 7.4 Why the demo does not use the seeded data

The seed data exists so the database is not empty when someone opens it during the defence, and
so the filters have something to filter. The Postman run creates its own data instead, because a
demonstration that mutates or depends on seeded rows breaks the second time you run it. The two
serve different purposes and neither depends on the other.
