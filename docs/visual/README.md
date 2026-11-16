# Done n Dusted: visual documentation guide

This folder holds the visual documentation for the LD1 REST API. Each HTML file is one diagram,
and `index.html` puts the most important ones on a single page. This guide explains every
diagram twice:

- **Technical:** the terms you would use in a report or an exam answer.
- **Plain English:** what the same thing means, in everyday words.

---

## Contents

1. [How to open and use the diagrams](#1-how-to-open-and-use-the-diagrams)
2. [The project in one minute](#2-the-project-in-one-minute)
3. [Hub page: `index.html`](#3-hub-page-indexhtml)
4. [Runtime architecture: `architecture.html`](#4-runtime-architecture-architecturehtml)
5. [Approving a log entry: `approve-log-sequence.html`](#5-approving-a-log-entry-approve-log-sequencehtml)
6. [Log entry status: `log-entry-lifecycle.html`](#6-log-entry-status-log-entry-lifecyclehtml)
7. [From request to status code: `status-code-workflow.html`](#7-from-request-to-status-code-status-code-workflowhtml)
8. [Database: `database-erd.html`](#8-database-database-erdhtml)
9. [Classes: `class-diagram.html`](#9-classes-class-diagramhtml)
10. [Glossary: technical term and plain meaning](#10-glossary-technical-term-and-plain-meaning)
11. [Questions an examiner may ask, answered with the diagrams](#11-questions-an-examiner-may-ask-answered-with-the-diagrams)
12. [How these files were made](#12-how-these-files-were-made)

---

## 1. How to open and use the diagrams

Double-click any `.html` file. It opens in your browser and needs no internet, server or
install. Every file carries its own viewer.

In the viewer you can:

- **Zoom and pan** with the mouse wheel and by dragging.
- **Switch the theme** between light, dark and system.
- **Hover a box** to see what it is. Most boxes also show the file and line numbers in the
  code they come from. Those links point at commit `6f927d7` on GitHub, so they always show
  the exact code the diagram was drawn from.
- **Export** the diagram as an image (PNG/SVG) for a report or slides.
- On `index.html`, click **Open interactive view** above a diagram to see it full size.

> **Tip for the defense:** keep `index.html` open in a browser tab. It answers "how does it
> work?" in one scroll, and every diagram can be opened full screen from there.

---

## 2. The project in one minute

**Technical.** Done n Dusted is a RESTful web API built with Spring Boot 4 on Java 21. It
exposes three resources arranged in a strict hierarchy: *Guild → Quest → LogEntry*, each a
one-to-many relationship. Every resource supports the five standard operations (list, read,
create, replace, delete), which gives 15 endpoints in total. Data is stored in PostgreSQL 18,
and the schema is managed by Flyway migrations.

**Plain English.** It's the "back office" of a game-like habit tracker. A **guild** is a group
of friends. A guild has **quests** (challenges like "run 50 km"). When someone does part of a
quest, they file a **log entry** ("I ran 8 km today"). The group's game master (the *Dungeon
Master*) then approves or rejects that entry, and only approved entries earn **XP**
(experience points).

There is no website with buttons. Other programs (Postman, a browser, a future mobile app) talk
to this one by sending short text messages over the internet, and it answers in a format
called JSON.

---

## 3. Hub page: `index.html`

**What it is.** A one-page explainer. It starts with the conclusion, then shows the details in
panels: four diagrams, a table of endpoints, a real code excerpt, the list of all diagram
files, and a demo checklist.

**How to read it.** Top to bottom:

| Panel | Question it answers |
| --- | --- |
| Answer | What is this system, in one sentence and four points? |
| 1 · What runs where? | Which programs run, and how do they connect? |
| 2 · Which endpoints exist? | Which URLs can a client call? |
| 3 · What happens when the DM approves an entry? | What does one request do inside the code? |
| 4 · How does a log entry change status? | What states can a log entry be in? |
| 5 · Which check produces which status code? | Why did I get a 400, 404, 409 or 422? |
| 6 · Where are the other diagrams? | Which file shows what? |
| 7 · How do I demo it? | What do I run, and what should I see? |

The small numbers after sentences (¹ ² ³) are **citations**. Hover one to see the exact source
lines in the repository it is based on.

---

## 4. Runtime architecture: `architecture.html`

### What kind of diagram

**Technical.** A *component / deployment-style architecture diagram*. It shows the runtime
components, the processes they run in (the outlined boundaries), and the communication
channels between them (the arrows, labelled with protocol or purpose). Think of it as a
mix of the C4 model's *container* and *component* levels.

**Plain English.** A map of "who talks to whom" while the app is running. Boxes are parts of
the system, the big outlined areas are separate programs, and arrows are messages being sent.

### What it shows

Reading left to right, following one request:

1. **API clients / web browser.** Postman, newman, curl or Swagger UI send an HTTP request
   with JSON to port **8080**.
2. **Embedded Tomcat.** The web server built into the app. It receives the request and hands
   it to Spring's *DispatcherServlet*, which picks the right controller.
3. **Controllers** (`GuildController`, `QuestController`, `LogEntryController`). They translate
   HTTP into a Java method call and back.
4. **Services** (`GuildService`, `QuestService`, `LogEntryService`). They hold the rules and run
   inside a database **transaction** (`@Transactional`). Note the vertical "uses" arrows:
   `QuestService` asks `GuildService` whether the guild exists, and `LogEntryService` asks
   `QuestService`.
5. **Spring Data repositories.** They turn method names into SQL queries.
6. **Hibernate / JPA.** Maps Java objects to table rows. Set to `ddl-auto: validate`, so it
   only *checks* the tables and never changes them.
7. **DataSource.** The database connection, chosen by **profile**:
   - `dev` (the default): `EmbeddedPostgresConfig` *starts a real PostgreSQL 18 process* on
     port **55432**, with temporary data that is recreated on every start.
   - `prod` (optional, dashed line): a normal PostgreSQL on port 5432, for example from
     `docker-compose.yml`.
8. **Flyway.** Runs `V1__schema.sql` (creates tables) and `V2__seed_data.sql` (fills demo
   data) when the app starts.
9. **ApiExceptionHandler.** Catches any error and turns it into one standard JSON error.
10. **springdoc OpenAPI.** Generates `/v3/api-docs` and the Swagger UI page from the code.
11. **DB GUI client.** DBeaver, HeidiSQL or psql connect directly to port 55432 to look inside
    the database.

### Everyday comparison

A restaurant:

| In the app | In the restaurant |
| --- | --- |
| Client (Postman) | The customer |
| Tomcat | The front door and host |
| Controller | The waiter: takes the order, brings the plate |
| Service | The chef: knows the recipes and the rules ("no XP for unapproved entries") |
| Repository | The pantry clerk: fetches exactly what is asked for |
| Hibernate | The labelling system that matches shelf items to the recipe book |
| PostgreSQL | The pantry itself |
| Flyway | The opening checklist that sets up the shelves each morning |
| ApiExceptionHandler | The complaints desk, which always answers on the same form |
| Swagger UI | The printed menu |

### Key ideas to remember

- **Layered architecture:** each layer only talks to the one below it, so a change in one
  place doesn't ripple everywhere.
- **The database is a separate program,** even though the app starts it. That's why DBeaver
  can connect to it, and why it must be stopped cleanly with Ctrl+C.
- **Profiles** let the same code run against different databases without changes.

---

## 5. Approving a log entry: `approve-log-sequence.html`

### What kind of diagram

**Technical.** A *UML sequence diagram*. Each vertical line (*lifeline*) is a participant.
Horizontal arrows are messages (method calls or HTTP requests), read **top to bottom in time
order**. Dashed arrows are return values. Shaded bands group the messages into stages.

**Plain English.** A comic strip of one request. Each column is a "character", and each arrow
is one of them saying something to another. Read it from top to bottom like a conversation.

### The scenario

The Dungeon Master approves log entry 9 of quest 6 ("Deadlift bodyweight x2", LEGENDARY) in
guild 2:

```
PATCH /api/guilds/2/quests/6/logs/9
{ "occurredAt": "2026-09-20T14:45:00Z", "status": "APPROVED" }
```

### The four stages

| Stage | Technical | Plain English |
| --- | --- | --- |
| 1 · Bind JSON + @Valid | Jackson deserialises the body into `LogEntryUpdateRequest`; Bean Validation checks `@PositiveOrZero`, `@URL`, `@Size`. On failure: **400**. | The app reads the message and checks it's filled in properly. If not: "your form is wrong". |
| 2 · Load through the chain | `LogEntryRepository.findByIdAndQuestIdAndQuestGuildId(9, 6, 2)`, one SQL query joining log_entry to quest. If empty: **404**, naming the level that's missing. | It checks that entry 9 really belongs to quest 6, and quest 6 really belongs to guild 2. If not: "not found". |
| 3 · Difficulty + rules | `QuestService.get` loads the quest; `requireNotInFuture` rejects future dates with **422**; the switch on `APPROVED` computes XP = `Difficulty.LEGENDARY.xpValue()` = 8 × 10 = **80**. | It checks the date makes sense, then works out the reward from how hard the quest is. |
| 4 · Approve, commit, respond | `LogEntry.approve(80, 0, null, now)` changes the entity; at transaction commit Hibernate's *dirty checking* issues an SQL `UPDATE`; the controller maps to `LogEntryResponse` and returns **200** with JSON. | It marks the entry approved, saves it, and sends back the updated entry. |

The **alt:** arrows going to *Error handler* show where each failure leaves the normal path.
They all end up as the same `ApiError` JSON.

### Things the diagram makes visible

- **The client never sends XP here.** The server calculates it. That's the anti-cheating rule.
- **PATCH is a partial update.** The example body has no `note` or `evidenceUrl`, so those
  stay exactly as they were. Only the fields that are sent change.
- **No `save()` call.** Hibernate notices the change itself when the transaction ends; this
  is called *dirty checking*.
- **Order matters:** a wrong URL chain (404) is detected *before* a future date (422).

---

## 6. Log entry status: `log-entry-lifecycle.html`

### What kind of diagram

**Technical.** A *state machine diagram* (UML state diagram / finite-state machine). Rounded
boxes are **states**, arrows are **transitions**, and the label on an arrow is the **event** that
causes it. A start marker shows the initial state and an end marker shows the final one.

**Plain English.** The "status tracker" of a log entry, like a parcel that goes "ordered →
shipped → delivered". It shows every status an entry can have and how it moves between them.

### States and transitions

| From | Event | To | What happens in code |
| --- | --- | --- | --- |
| (start) | `POST .../logs` | **PENDING** | Constructor sets PENDING, 0 XP. Rejected with 422 if the quest is inactive or the date is in the future. |
| any | `PATCH` with `APPROVED` | **APPROVED** | `approve()`: XP = DM's number or the difficulty value, inspiration default 0, `reviewedAt = now`. Re-approving recalculates XP. |
| any | `PATCH` with `REJECTED` | **REJECTED** | `reject()`: XP and inspiration forced to 0. Sending non-zero awards gives 422. |
| any | `PATCH` with `PENDING` | **PENDING** | `resetToPending()`: awards cleared, `reviewedAt` emptied. Non-zero awards give 422. |
| any | `DELETE`, or parent quest/guild deleted | **Deleted** (end) | Row removed; the cascade is done by the database (`ON DELETE CASCADE`). |

### Why it's designed this way

**Technical.** Status and rewards are **encapsulated**: `LogEntry` has no `setStatus` or
`setXpAwarded`. State changes only happen through three intention-revealing methods, so the
object can never reach an invalid state. The same **invariant** is enforced a second time in
the database with `CHECK` constraints (`ck_log_pending_is_empty`, `ck_log_reviewed_at`): this
is *defence in depth*.

**Plain English.** You can't just flip the status switch. You have to "approve", "reject" or
"reset", and each of those also fixes the reward and the review time. Even if someone bypassed
the app and edited the database directly, the database would refuse a pending entry with
points.

---

## 7. From request to status code: `status-code-workflow.html`

### What kind of diagram

**Technical.** A *flowchart / decision workflow*. Each node is a validation **gate** (a
condition). The main path is "pass"; the red box beside each gate is its failure outcome,
the HTTP status code returned.

**Plain English.** An airport security line. Your request walks through checkpoints. At each
one, if something is wrong, you're stopped and told why with a number. If you get through
all of them, you reach the gate (a success code).

### The two rows

**Row 1: checks done by the Spring framework** (configured by our annotations):

| Gate | Fails with | Example |
| --- | --- | --- |
| URL and method match a controller? | **404** / **405** | `/api/nothing`, or `PUT /api/guilds/2` |
| Content-Type is JSON? | **415** | sending `text/plain` |
| Client accepts JSON? | **406** (empty body) | `Accept: application/xml` |
| IDs in the URL are numbers? | **400** | `/api/guilds/abc` |
| Body is readable JSON with only known fields? | **400** | broken JSON, unknown field, `"stat":"LUCK"` |

**Row 2: checks done by our own code:**

| Gate | Fails with | Example |
| --- | --- | --- |
| `@Valid` field rules | **400** + `details[]` | empty `name` |
| Parent chain exists | **404** | quest 4 asked for under guild 1 |
| Domain rules | **422** | GRIND without `targetCount`, future `occurredAt`, inactive quest |
| Name/title still free | **409** | creating a second "Iron Vow" |
| Database constraints | **409** | two identical requests at the same moment |
| Success | **201** + Location / **200** / **204** | create / read or update / delete |

Anything unexpected becomes **500** with a generic message, so internal details never leak.

### The one distinction to know by heart: 400 vs 422

| | 400 Bad Request | 422 Unprocessable Content |
| --- | --- | --- |
| Technical | The request is malformed: syntax, types or field constraints | The request is well-formed, but violates a business rule |
| Plain English | "Your form is filled in wrong." | "Your form is fine, but we can't do that." |
| Test | Could a schema or validator catch it by looking at the request alone? | Do you need to know the rules of *this* game to see the problem? |
| Example | `"name": ""` | a GRIND quest with no target number |

---

## 8. Database: `database-erd.html`

### What kind of diagram

**Technical.** An *entity–relationship diagram (ERD)* of the physical schema created by
`V1__schema.sql`. Each table lists its columns with SQL types and key roles: **PK** (primary
key), **FK** (foreign key) and **UK** (unique key). Lines between tables show **cardinality**,
here one-to-many (1 : 0..N).

**Plain English.** A picture of the spreadsheets inside the database. Each table is a sheet,
each column a heading, and the lines show which sheet points to which.

### The three tables

| Table | Maps to class | Key points |
| --- | --- | --- |
| `guild` | `Guild` | `id` PK; `name` unique |
| `quest` | `Quest` | `guild_id` FK → `guild.id`; `(guild_id, title)` unique together, so the same title is allowed in different guilds; CHECKs on stat/type/difficulty/target_count |
| `log_entry` | `LogEntry` | `quest_id` FK → `quest.id`; CHECKs: XP and inspiration ≥ 0, PENDING has no awards, reviewed entries have a review time |

Both foreign keys have **`ON DELETE CASCADE`**: deleting a guild deletes its quests, and
deleting a quest deletes its log entries.

### Key terms

| Technical | Plain English |
| --- | --- |
| Primary key (PK) | The row's unique ID number, like a passport number |
| Foreign key (FK) | A column that points to a row in another table: "this quest belongs to guild 2" |
| Unique constraint (UK) | "No two rows may have the same value here" |
| CHECK constraint | A rule the database checks on every insert or update, e.g. "XP can't be negative" |
| Cardinality 1 : N | One guild can have many quests; each quest has exactly one guild |
| ON DELETE CASCADE | Delete the parent and the children go with it, like deleting a folder |
| Index | A lookup shortcut, like the index at the back of a book |
| Migration (Flyway) | A numbered SQL script that builds or changes the database, run once in order |

The cards under the tables list every index and CHECK constraint by name.

---

## 9. Classes: `class-diagram.html`

### What kind of diagram

**Technical.** A *UML class diagram*. Boxes are types (classes, interfaces, enums, records),
with selected fields and methods. Arrows are relationships:

| Arrow | UML meaning | Here |
| --- | --- | --- |
| Solid line, open arrowhead | **Association** (holds a reference as a field) | `Quest → Guild`, `LogEntry → Quest` (`@ManyToOne LAZY`); `Quest → Stat/QuestType/Difficulty`, `LogEntry → LogStatus` |
| Dashed line, open arrowhead | **Dependency** (uses another type to do its job) | controller → service, service → repository, `QuestService → GuildService`, `LogEntryService → QuestService` |
| Solid line, hollow triangle | **Inheritance / generalisation** (extends) | exceptions → `RuntimeException`; repositories → `JpaRepository`, `JpaSpecificationExecutor` |

The viewer has its own legend. If its line styles differ from the textbook ones above, trust the
relationship names in the legend.

**Plain English.** A family tree of the code. It shows which pieces exist, what each one holds
and can do, and which piece needs which other piece to work.

### Reading it top to bottom

1. **Controllers:** the entry points, one per resource.
2. **Services:** the rules. `QuestService` needs `GuildService`, and `LogEntryService` needs
   `QuestService`, because each must check its parent exists.
3. **Repositories:** interfaces only. Spring writes the code for them at startup.
4. **Entities:** `Guild`, `Quest`, `LogEntry`, the objects stored in the database.
   `LogEntry` shows `approve`, `reject` and `resetToPending`, and notes that there is *no
   status setter*.
5. **Enums:** fixed lists of allowed values. `Difficulty` also carries its XP multiplier
   (×1, ×2, ×3, ×5, ×8) and `BASE_XP = 10`.
6. **Exceptions:** each maps to one status code (404, 409, 422).

### Why association and not composition?

**Technical.** Only the child holds a reference (`@ManyToOne`); the parent has no collection
of children (no `@OneToMany`). In UML, composition would mean the parent owns and manages its
children's lifetime in code. Here that lifetime rule lives in the database (`ON DELETE
CASCADE`), not in the Java classes.

**Plain English.** A quest knows which guild it's in, but the guild object doesn't keep a list
of its quests. The database cleans up children on delete; the Java code doesn't need to.

### A note on layout

This diagram passed Archify's checks at the "standard" quality level, not the stricter
"showcase" level. The repositories each extend two Spring interfaces, which forces one line
crossing that can't be avoided on a flat page. Nothing was left out to hide it.

---

## 10. Glossary: technical term and plain meaning

| Technical term | Plain meaning |
| --- | --- |
| **API** (Application Programming Interface) | A way for programs to talk to each other |
| **REST** | A style of API where everything is a "thing" with a web address, and you use standard verbs on it |
| **Resource** | One of those things: a guild, a quest, a log entry |
| **Endpoint** | One address + verb combination, e.g. `GET /api/guilds` |
| **HTTP method / verb** | GET = read, POST = create, PATCH = partial update (PUT is not used: 405), DELETE = remove |
| **Status code** | A three-digit result: 2xx success, 4xx your mistake, 5xx server's mistake |
| **JSON** | A text format for data: `{"name": "Iron Vow"}` |
| **Content-Type / Accept** | Headers saying "I'm sending JSON" / "I want JSON back" |
| **Location header** | After creating something, the address of the new thing |
| **Idempotent** | Doing it twice has the same effect as doing it once (GET, PUT, DELETE are; POST isn't) |
| **Pagination** | Returning a long list in pages, with links to the next and previous page |
| **HATEOAS links** | Those navigation links inside the response |
| **Controller** | Code that receives web requests |
| **Service** | Code that applies the rules |
| **Repository** | Code that reads and writes the database |
| **Entity** | A Java object that matches a database row |
| **DTO** (Data Transfer Object) | The exact shape of the JSON the outside world sees, kept separate from the entity |
| **Validation** (`@Valid`, `@NotBlank`) | Automatic checks that fields are filled in correctly |
| **Transaction** | A group of database changes that all succeed or all fail together |
| **Dirty checking** | Hibernate noticing you changed an object and saving it automatically |
| **Lazy loading** | Fetching related data only when it's actually needed |
| **ORM / JPA / Hibernate** | The tool that translates between Java objects and database tables |
| **Flyway migration** | A numbered SQL script that sets up the database in a known way |
| **Profile (dev/prod)** | A named set of settings, e.g. which database to use |
| **Embedded PostgreSQL** | A real database server that the app starts and stops by itself |
| **OpenAPI / Swagger** | A machine-readable description of the API, plus a web page to try it |
| **Invariant** | A rule that must always be true, e.g. "a pending entry has 0 XP" |
| **Encapsulation** | Hiding internals so they can only be changed through safe methods |
| **Exception handler** | One central place that turns errors into proper responses |

---

## 11. Questions an examiner may ask, answered with the diagrams

| Question | Show this | Say this |
| --- | --- | --- |
| "How is the system structured?" | `architecture.html` | Layered: controller → service → repository → database, with one central error handler. |
| "What happens when I call this endpoint?" | `approve-log-sequence.html` | Walk the four stages; point out where XP is computed. |
| "Why is this 422 and not 400?" | `status-code-workflow.html` | 400 = malformed request; 422 = valid request that breaks a rule. |
| "Can a player give themselves XP?" | `log-entry-lifecycle.html` + sequence | No: the create request has no XP field, the entity has no setter, and the database has a CHECK. |
| "How are the tables related?" | `database-erd.html` | One-to-many twice, foreign keys with cascade delete, unique and CHECK constraints. |
| "What does deleting a guild do?" | ERD | The database cascades: its quests and their log entries go too. Returns 204. |
| "Why can't I reach a quest through another guild?" | sequence, stage 2 | Every lookup filters by the parent id too, so a wrong chain is a 404. |
| "Is the database real?" | architecture, dev path | Yes: PostgreSQL 18 runs as its own process on port 55432. Connect DBeaver to show it. |
| "Where do the rules live?" | `class-diagram.html` | In the services and the `LogEntry` methods, never in the controllers. |

---

## 12. How these files were made

The diagrams were generated with [Archify](https://github.com/tt-a1i/archify), an open-source
tool that turns a structured description into a self-contained HTML diagram. Each diagram was
written from the source code at commit `6f927d7`, and each box links back to the files and
lines it is based on.

Every file passed Archify's automated checks: schema validation, delivery, a source-provenance
check, and a test in a real browser. The checks prove the files are valid and that the cited
code exists at that commit. They don't replace reading the diagram yourself, so click through
each one before the defense.

If the code changes, the diagrams do **not** update themselves. Regenerate them, or note the
commit they describe (`6f927d7`).
