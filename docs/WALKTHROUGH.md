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
