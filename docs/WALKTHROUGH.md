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
