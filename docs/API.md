# Done n Dusted - API reference

Done n Dusted is a quest log for tabletop RPG groups: players log real-life progress, the guild's
game master approves it, and approved entries turn into XP for the player's character.

This document describes the REST API: how to run it, authentication, roles, all endpoints, and the
response and error formats. Project overview - [`README.md`](../README.md), user interface (LD3) -
[`LD3.md`](LD3.md).

Resource hierarchy: **Guild (1:N) -> Quest (1:N) -> LogEntry**. Users belong to guilds through a
**GuildMembership** with the role `PLAYER` or `DUNGEON_MASTER` (the game master).

| Lab | Branch / tag | Defense material |
| --- | --- | --- |
| LD1 - REST API | `ld1`, `v0.1.0` | [`visual/GYNIMO_GIDAS.md`](visual/GYNIMO_GIDAS.md), [`visual/DEMO_KOMANDOS.txt`](visual/DEMO_KOMANDOS.txt) |
| LD2 - authentication and authorization | `ld2`, `v0.2.1` | [`LD2.md`](LD2.md), [`DEMO_PLANAS.md`](DEMO_PLANAS.md), [`gynimas/LD2_gynimas.html`](gynimas/LD2_gynimas.html) |
| LD3 - user interface and deployment | `ld3`, `v0.3.0` | [`LD3.md`](LD3.md) |

---

## Quick start

You only need **Java 21**. No Maven, Docker or installed PostgreSQL is required.

```bash
./mvnw spring-boot:run
```

On Windows, `demo/1-paleisti-api.cmd` does the same (it frees the ports and opens Swagger);
the other `demo/` scripts run the Postman tests, a curl shell with tokens, and psql.

The first run downloads Maven and the PostgreSQL 18 binaries (a few minutes), later runs take
seconds. Once `Started DoneNDustedApplication` appears:

| | |
| --- | --- |
| API | <http://localhost:8080/api/guilds> (without a token - `401`) |
| Swagger UI | <http://localhost:8080/swagger-ui.html> (token - **Authorize** button) |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |
| Database | `jdbc:postgresql://localhost:55432/postgres` (user `postgres`) |

The default `dev` profile starts a **real PostgreSQL** server process, and Flyway immediately
creates the schema and fills it with demo data (4 users, 3 guilds, 9 quests, 12 log entries).

### Demo users

The password for all of them is `Demo123!` (the live demo uses a private admin password). On the live demo all data goes back to this seed every night at 04:00 Vilnius time, and accounts created there are removed.

| ID | Username | Global role | Guilds |
| --- | --- | --- | --- |
| 1 | `admin` | `ADMIN` | - (sees all) |
| 2 | `rokas` | `USER` | Game master and owner: *Thursday Night Crew*, *Iron Vow*; player: *The Quiet Library* |
| 3 | `ieva` | `USER` | Game master and owner: *The Quiet Library*; player: the other two |
| 4 | `jonas` | `USER` | Player: *Thursday Night Crew*, *Iron Vow* |

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" -d '{"username":"rokas","password":"Demo123!"}'
# -> {"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":900,"refreshToken":"..."}

curl -s http://localhost:8080/api/guilds -H "Authorization: Bearer <accessToken>"
```

### Running with a real, persistent database

```bash
docker compose up -d                      # or your own PostgreSQL server
JWT_SECRET=<at least 32 characters> ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

Connection settings come from the `DB_URL`, `DB_USER` and `DB_PASSWORD` environment variables.
In the `prod` profile the JWT signing key is **required** (`JWT_SECRET`); without it the
application does not start.

---

## Authentication

| Token | What it is | Valid for | Where it is stored |
| --- | --- | --- | --- |
| Access | JWT signed with HS256. Claims: `sub` (user ID), `username`, `role`, `iss`, `iat`, `exp` | 15 min | Not stored on the server; every request checks the signature and expiry |
| Refresh | Random 256-bit string (not a JWT) | 7 days | Database table `refresh_token`, SHA-256 hash only |

* The access token is sent in the `Authorization: Bearer <accessToken>` header.
* **Rotation:** `POST /api/auth/refresh` exchanges a valid refresh token for a new pair and
  revokes the old one. If a revoked token is sent again, it is treated as stolen and **all** of
  that user's sessions are revoked.
* **Logout** revokes the refresh token. The access token stays valid until its `exp`, which is why
  it is short-lived.
* Passwords are stored only as **BCrypt** hashes with a salt. A wrong username and a wrong password
  return the same error, and for an unknown username a BCrypt comparison still runs, so the
  response time does not reveal whether the user exists.
* A blocked user (`enabled = false`) cannot log in or refresh tokens (`403`).
* The API has no sessions or cookies (`STATELESS`), so CSRF protection is disabled: the browser does
  not add the token on its own, only a client that knows it does.

## Roles and permissions

| Who | What they can do |
| --- | --- |
| **Guest** (no token) | Register, log in, refresh tokens, view Swagger. Everything else - `401` |
| **User** (`USER`) | Create guilds (becomes owner and game master), see their own guilds, `/api/auth/me` |
| **`PLAYER`** in a guild | See the guild, its members and quests; create quests (not `SIDE_QUEST`); log progress; see **only their own** log entries, edit and delete them while they are `PENDING`; leave the guild |
| **`DUNGEON_MASTER`** (game master) in a guild | Everything a player can do, plus: see all log entries in the guild, approve / reject them and award XP and inspiration (**not their own**), create `SIDE_QUEST` quests, edit and delete any quest in the guild |
| **Owner** (guild creator) | Edit and delete the guild, add and remove members, change their roles |
| **`ADMIN`** | See all guilds and their content, delete any guild, quest or log entry, manage users (`/api/users`) |

* A user can have different roles in different guilds. A guild has **one game master**: trying to
  assign a second one returns `409` (also enforced by a partial unique index in the database).
* A non-member gets nothing about a guild - `403`.
* The server reads guild roles from the database on every request, so a role change applies
  immediately. The global role (`ADMIN`) is in the token and changes with the next access token.

---

## API endpoints

All paths start with `/api`. A child resource is reachable **only through its parent** - a wrong
chain returns `404`. The **Who** column lists who is allowed (everyone else gets `403`).

### Guilds (Guild)

| # | Method | Path | Description | Who | Success |
| --- | --- | --- | --- | --- | --- |
| 1 | `GET` | `/api/guilds` | List; filter `name`, paging, sorting | Own guilds; `ADMIN` - all | `200` |
| 2 | `GET` | `/api/guilds/{guildId}` | One guild | Member, `ADMIN` | `200` |
| 3 | `POST` | `/api/guilds` | Create a guild (the creator becomes owner and game master) | Logged in | `201` + `Location` |
| 4 | `PATCH` | `/api/guilds/{guildId}` | Partially update a guild | Owner | `200` |
| 5 | `DELETE` | `/api/guilds/{guildId}` | Delete a guild with all its content | Owner, `ADMIN` | `204` |

### Quests (Quest)

| # | Method | Path | Description | Who | Success |
| --- | --- | --- | --- | --- | --- |
| 6 | `GET` | `/api/guilds/{guildId}/quests` | List; filters `stat`, `type`, `difficulty`, `active` | Member, `ADMIN` | `200` |
| 7 | `GET` | `/api/guilds/{guildId}/quests/{questId}` | One quest | Member, `ADMIN` | `200` |
| 8 | `POST` | `/api/guilds/{guildId}/quests` | Create a quest | Member; `SIDE_QUEST` - game master only | `201` + `Location` |
| 9 | `PATCH` | `/api/guilds/{guildId}/quests/{questId}` | Partially update a quest | Creator, game master, owner | `200` |
| 10 | `DELETE` | `/api/guilds/{guildId}/quests/{questId}` | Delete a quest with its log entries | Creator, game master, owner, `ADMIN` | `204` |

### Log entries (LogEntry)

| # | Method | Path | Description | Who | Success |
| --- | --- | --- | --- | --- | --- |
| 11 | `GET` | `/api/guilds/{guildId}/quests/{questId}/logs` | List; filter `status` | Player - own; game master, `ADMIN` - all | `200` |
| 12 | `GET` | `/api/guilds/{guildId}/quests/{questId}/logs/{logId}` | One log entry | Author, game master, `ADMIN` | `200` |
| 13 | `POST` | `/api/guilds/{guildId}/quests/{questId}/logs` | Log progress (always `PENDING`, 0 XP) | Member | `201` + `Location` |
| 14 | `PATCH` | `/api/guilds/{guildId}/quests/{questId}/logs/{logId}` | Content - author (while `PENDING`); review - game master, not their own log entry | Author / game master | `200` |
| 15 | `DELETE` | `/api/guilds/{guildId}/quests/{questId}/logs/{logId}` | Delete a log entry | Author (while `PENDING`), game master, `ADMIN` | `204` |

### Authentication (Auth)

| # | Method | Path | Description | Who | Success |
| --- | --- | --- | --- | --- | --- |
| 16 | `POST` | `/api/auth/register` | Register (role is always `USER`) | Guest | `201` + `Location` |
| 17 | `POST` | `/api/auth/login` | Log in - access and refresh tokens | Guest | `200` |
| 18 | `POST` | `/api/auth/refresh` | New token pair for a refresh token (rotation) | Guest | `200` |
| 19 | `POST` | `/api/auth/logout` | Revoke the refresh token | Guest | `204` |
| 20 | `GET` | `/api/auth/me` | Current user and their guilds with roles | Logged in | `200` |

### Guild members (Member)

| # | Method | Path | Description | Who | Success |
| --- | --- | --- | --- | --- | --- |
| 21 | `GET` | `/api/guilds/{guildId}/members` | List members; filter `role` | Member, `ADMIN` | `200` |
| 22 | `GET` | `/api/guilds/{guildId}/members/{userId}` | One member | Member, `ADMIN` | `200` |
| 23 | `POST` | `/api/guilds/{guildId}/members` | Add a member (`username`, `role`) | Owner, `ADMIN` | `201` + `Location` |
| 24 | `PATCH` | `/api/guilds/{guildId}/members/{userId}` | Change a member's role | Owner, `ADMIN` | `200` |
| 25 | `DELETE` | `/api/guilds/{guildId}/members/{userId}` | Remove a member or leave the guild | Owner, `ADMIN`, the member themselves | `204` |

### Users (User)

| # | Method | Path | Description | Who | Success |
| --- | --- | --- | --- | --- | --- |
| 26 | `GET` | `/api/users` | List users; filter `username` | `ADMIN` | `200` |
| 27 | `GET` | `/api/users/{userId}` | One user | `ADMIN` | `200` |
| 28 | `PATCH` | `/api/users/{userId}` | Change role or block (`role`, `enabled`) | `ADMIN` | `200` |

### Character sheet

| # | Method | Path | Description | Who | Success |
| --- | --- | --- | --- | --- | --- |
| 29 | `GET` | `/api/guilds/{guildId}/members/{userId}/sheet` | The member's level, XP, stats, inspiration and streak, only from their `APPROVED` log entries in this guild. `FULL` also has the 10 latest log entries and `pendingCount` | Member, `ADMIN`; `FULL` - the member, game master, `ADMIN`, other members get `PUBLIC` | `200` |

### Friendships (Friendship)

| # | Method | Path | Description | Who | Success |
| --- | --- | --- | --- | --- | --- |
| 30 | `GET` | `/api/friends` | My friendships and friend requests in both directions; filter `status` | Logged in | `200` |
| 31 | `GET` | `/api/friends/{friendshipId}` | One friendship | Both sides (others and the blocked user get `404`) | `200` |
| 32 | `POST` | `/api/friends` | Send a friend request (`username`); an existing pair - `409` | Logged in | `201` + `Location` |
| 33 | `PATCH` | `/api/friends/{friendshipId}` | `ACCEPTED` - accept the friend request, `BLOCKED` - block | `ACCEPTED` - recipient; `BLOCKED` - both sides | `200` |
| 34 | `DELETE` | `/api/friends/{friendshipId}` | Cancel or decline a friend request, unfriend, unblock | Both sides; a blocked one - only the user who blocked | `204` |
| 35 | `GET` | `/api/friends/{friendshipId}/sheet` | The friend's `PUBLIC` character sheet, combined across all their guilds | Friend (`ACCEPTED` only) | `200` |

### System

| # | Method | Path | Description | Who | Success |
| --- | --- | --- | --- | --- | --- |
| 36 | `GET` | `/actuator/health` | Health check for the cloud platform - only `{"status":"UP"}`, no details | Guest | `200` |

**Updates use `PATCH` (partial).** Only the fields sent are changed; the rest stay as they were.
So, for example, changing only a quest's difficulty never loses its description. The log entry
review fields (`xpAwarded`, `inspirationAwarded`, `reviewerNote`) are accepted only together with
`status`.

**The description never disappears.** When a guild or quest is created without `description`, it is
saved as an empty string `""`, so the field is always present in responses. The database enforces
this too (`NOT NULL DEFAULT ''`, migration `V3`).

The full specification of every endpoint is in [`docs/openapi.json`](openapi.json) or live in
Swagger UI. Every protected endpoint documents `401`, and the guild (`/guilds/{guildId}/...`) and
user endpoints also document `403`.

---

## Response formats

### Paged list

All `GET` lists return the same structure. Navigation links are built from the actual request
URI, so **all filters and sorting are kept in them**. `prev` and `next` are omitted when there is
no such page. `totalElements` comes first so that the total count (for example, how many guilds
there are) is visible right away.

```json
{
  "totalElements": 3,
  "content": [ { "id": 2, "name": "Iron Vow", "ownerId": 2, "...": "..." } ],
  "page":  { "number": 0, "size": 20, "totalElements": 3, "totalPages": 1 },
  "links": {
    "self":  "http://localhost:8080/api/guilds?name=iron&page=0&size=20",
    "first": "http://localhost:8080/api/guilds?name=iron&page=0&size=20",
    "last":  "http://localhost:8080/api/guilds?name=iron&page=0&size=20"
  }
}
```

Paging parameters: `?page=0&size=20&sort=name,asc`. The largest allowed `size` is 100, and `page`
is at most 1,000,000. The default size is 20, and pages are counted from 0.

Sorting is allowed only by simple list fields; anything else returns `400` with the list of
allowed fields:

| List | `sort` fields |
| --- | --- |
| Guilds | `id`, `name`, `createdAt` |
| Quests | `id`, `title`, `stat`, `type`, `difficulty`, `targetCount`, `active`, `createdAt` |
| Log entries | `id`, `occurredAt`, `createdAt`, `status`, `xpAwarded`, `reviewedAt` |
| Members | `joinedAt`, `role` |
| Users | `id`, `username`, `role`, `enabled`, `createdAt` |
| Friendships | `createdAt`, `status` |

The links (`links`) are a hypermedia element: the client does not build the next page URL itself,
it just follows the `links.next` link from the server until there is none. The Postman summary
test works this way too.

### Errors

Every error uses the same `ApiError` structure, including the `401` / `403` responses that Spring
Security filters return before the controller:

```json
{
  "timestamp": "2026-09-21T09:15:30.123Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You cannot review your own log entry.",
  "path": "/api/guilds/2/quests/5/logs/13"
}
```

`details` is present only when the error concerns specific fields.

| Code | When |
| --- | --- |
| `400` | The request could not be understood: malformed JSON, wrong type, unknown enum value, field constraint violation |
| `401` | No token, a broken or expired one, wrong login credentials, an invalid refresh token. Comes with the `WWW-Authenticate: Bearer` header |
| `403` | The user is known but lacks permission: not a guild member, wrong guild role, not `ADMIN`, blocked account |
| `404` | The resource does not exist **or** the parent chain is wrong |
| `405` | Correct path, wrong HTTP method |
| `406` | The client asked in the `Accept` header for a format the API does not offer |
| `409` | The request is valid but conflicts with the current state (name or username taken, a second game master, already a member) |
| `415` | The request body was not sent as `application/json` |
| `422` | The JSON was understood and the fields are valid, but a business rule is violated |
| `500` | Unexpected server error |

**400 or 422?** `400` means the error can be seen from the request alone. `422` means the request
is well-formed but breaks a system rule (`GRIND` without `targetCount`, `occurredAt` in the
future, XP on an unreviewed log entry, a new log entry on an inactive quest, editing an approved
log entry's content, an unknown user when adding a member).

**401 or 403?** `401` means the server does not know who you are. `403` means it knows, but does
not allow the action.

---

## Postman demo

[`postman/DoneNDusted.postman_collection.json`](../postman/DoneNDusted.postman_collection.json) -
78 requests, 176 checks (on a freshly started database). The collection registers a new user on
every run, creates its own data and cleans up after itself, so it can be run any number of times
in a row.

```bash
npx newman run postman/DoneNDusted.postman_collection.json
```

Or in the Postman app: *Import* -> *Runner* -> *Run*. It takes about **10 seconds**.

The same collection in the Postman v3 format (separate YAML files) is in
[`postman/Done n Dusted API (LD2)/`](../postman/Done%20n%20Dusted%20API%20%28LD2%29/), generated
from the JSON file with `postman collection migrate`. It can also be run with the Postman CLI:
`postman collection run "postman/Done n Dusted API (LD2)"`.

The collection sets *Bearer Token* `{{accessToken}}` at the collection level. Requests sent by the
player `jonas` or by `admin` have their own *Authorization* (`{{playerToken}}`, `{{adminToken}}`).

| Folder | What it shows |
| --- | --- |
| 0. Authentication | Registration, `409`, `400`, login and JWT claims, `/auth/me`, `401` without a token and with a broken token, refresh rotation and reuse detection, logout |
| 1. Overview | As `admin`: how many guilds, quests and log entries are in the database, with all IDs (`Total - guilds: 3, quests: 9, log entries: 12`) |
| 2. Guilds | Guild CRUD, the creator becomes the owner, a user sees only their own guilds, paging via `links.next` |
| 3. Members | Adding a player, `409` for an existing member and a second game master, `422` for an unknown user, `403` for a player |
| 4. Quests | Quest CRUD, filters, XP from difficulty, `createdBy` |
| 5. Log entries | A player logs progress, the game master approves it; XP from difficulty; an approved log entry's content is frozen (`422`) |
| 6. Roles and permissions | `403`: non-member, player approves, game master edits someone else's content, game master approves their own log entry, player views someone else's log entry, `SIDE_QUEST`, guild edit, `/api/users`; a player sees only their own log entries; leaving a guild |
| 7. Cleanup | Deletion `204`, `admin` deletes the second guild, `404` after deletion |
| 8. Administration | User list, `422` when blocking yourself, block -> login `403` -> unblock |

Commands without Postman (curl) - [`docs/visual/DEMO_KOMANDOS.txt`](visual/DEMO_KOMANDOS.txt).

---

## Business rules

* A quest must declare the stat it improves: `STR`, `DEX`, `CON`, `INT`, `WIS`, `CHA`.
* Quest type: `MILESTONE`, `HABIT`, `GRIND`, `SIDE_QUEST`. `targetCount` is required and allowed
  **only** for `GRIND`. Only the guild's game master creates `SIDE_QUEST` quests.
* Difficulty is a base XP multiplier: `TRIVIAL` x1, `EASY` x2, `MEDIUM` x3, `HARD` x5,
  `LEGENDARY` x8; the base XP is 10.
* **XP is never accepted from the client when a log entry is created.** `LogEntryCreateRequest`
  has no such field at all; a new log entry is always `PENDING` with 0 XP, and its author is the
  logged-in user.
* The game master can override XP **only** when approving. If `xpAwarded` is omitted, the server
  derives it from the quest difficulty. **The author cannot approve their own log entry**, even if
  they are the game master.
* Approved XP is **frozen** in the log entry. Changing the quest difficulty later does not rewrite
  history. The author can no longer change a reviewed log entry's content (evidence).
* `PENDING` and `REJECTED` log entries must have no XP and no inspiration; the rule is enforced in
  the Java code and by `CHECK` constraints in the database.

---

## Technologies

| Layer | Choice |
| --- | --- |
| Language | Java 21 (LTS) |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA, Validation) |
| Security | Spring Security 7 + OAuth2 Resource Server (Nimbus JOSE JWT, HS256), BCrypt |
| Database | PostgreSQL 18 |
| Migrations | Flyway (schema + demo data) |
| Documentation | springdoc-openapi 3.1.1 (OpenAPI 3.1 + Swagger UI with Bearer authorization) |
| Build | Maven Wrapper (`mvnw`) |

## Project structure

```
src/main/java/lt/ktu/donendusted/
  config/        CorsConfig, EmbeddedPostgresConfig, OpenApiConfig, SecurityConfig
  security/      TokenService (JWT and refresh tokens), CurrentUser, JwtProperties, ApiAuthErrorHandler
  domain/        Guild, Quest, LogEntry, User, GuildMembership, RefreshToken, Friendship + enums
  repository/    Spring Data interfaces
  service/       transactions, business rules and permissions (GuildAccess), CharacterSheetService, FriendshipService
  web/           controllers (FriendController - friendships), PagedResponse
  web/dto/       request and response records
  error/         ApiError, ApiExceptionHandler, exceptions (401, 403, 404, 409, 422)
src/main/resources/
  ValidationMessages.properties  Bean Validation messages (details[].message)
src/main/resources/db/migration/
  V1__schema.sql        tables, indexes, CHECK constraints
  V2__seed_data.sql     demo data
  V3__description_not_null.sql  description NOT NULL DEFAULT ''
  V4__users_and_tokens.sql      users (BCrypt) and refresh tokens
  V5__guild_membership.sql      memberships, owners, authors and seed links
  V6__friendship.sql            friendships; seed: ieva and jonas are friends, rokas sent ieva a friend request
src/test/java/lt/ktu/donendusted/
  *IntegrationTest      MockMvc tests with embedded PostgreSQL (one Spring context): ./mvnw test
  service/              unit tests for the character sheet formulas
.github/workflows/      CI: backend tests, frontend lint/tests/build, Postman and Playwright
demo/                   Windows launch scripts for the demo (API, newman, curl, psql)
frontend/               React + TypeScript + Vite client (LD3), Vitest and Playwright tests
docs/
  API.md                this document
  LD3.md                LD3 requirements, user interface, deployment and defense
  wireframes/           LD3 screen wireframes (SVG) and their generator
  screenshots/          application screenshots
  LD2.md                LD2 changes and defense questions
  DEMO_PLANAS.md        what to show at the defense and in which order
  gynimas/              LD2 defense page (HTML) and its sources (Archify)
  openapi.json          generated OpenAPI specification
  visual/               LD1 defense guide, demo commands and diagrams
postman/
  Done n Dusted API (LD2)/              collection in the Postman v3 format (YAML)
  DoneNDusted.postman_collection.json   the same collection as v2.1 JSON (newman)
```

## User interface (LD3)

The React client is in [`frontend/`](../frontend/); its requirements, wireframes and cloud
deployment are described in [`LD3.md`](LD3.md).

---

Author: **Miglė Cirtautaitė** · Kaunas, 2026
