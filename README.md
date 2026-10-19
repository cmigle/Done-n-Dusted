# Done n Dusted — REST API (LD1)

KTU T120B165 *Saityno taikomųjų programų projektavimas*. Antras laboratorinis darbas: REST
principais veikianti API sąsaja, OpenAPI specifikacija, PostgreSQL duomenų bazė su prasmingais
duomenimis ir paruošta demonstracija.

**Done n Dusted** — DnD žaidimo principu veikianti produktyvumo sistema. Žaidėjas registruoja
realaus gyvenimo progresą, grupės Dungeon Master jį patvirtina, o tik patvirtinti įrašai keičia
veikėjo charakteristikas.

Resursų hierarchija: **Guild (1:N) → Quest (1:N) → LogEntry**.

---

## Greitas paleidimas

Reikia tik **Java 21**. Nei Maven, nei Docker, nei įdiegtos PostgreSQL neprireikia.

```bash
./mvnw spring-boot:run
```

Pirmas paleidimas atsisiunčia Maven ir PostgreSQL 18 binarinius failus (kelias minutes),
vėlesni — sekundes. Kai pasirodo `Started DoneNDustedApplication`:

| | |
| --- | --- |
| API | <http://localhost:8080/api/guilds> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |
| Duomenų bazė | `jdbc:postgresql://localhost:55432/postgres` (naudotojas `postgres`) |

Numatytasis `dev` profilis paleidžia **tikrą PostgreSQL** serverio procesą, o Flyway iškart
sukuria schemą ir užpildo ją demonstraciniais duomenimis (3 grupės, 9 užduotys, 12 įrašų).

### Paleidimas su tikra, nuolatine duomenų baze

```bash
docker compose up -d                      # arba savas PostgreSQL serveris
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

Prisijungimo duomenys imami iš `DB_URL`, `DB_USER`, `DB_PASSWORD` aplinkos kintamųjų.

---

## API metodai

Visi keliai prasideda `/api`. Vaikinis objektas pasiekiamas **tik per tėvinį** — neteisinga
grandinė grąžina `404`.

### Grupės (Guild)

| # | Metodas | Kelias | Aprašymas | Sėkmė |
| --- | --- | --- | --- | --- |
| 1 | `GET` | `/api/guilds` | Sąrašas; filtras `name`, puslapiavimas, rikiavimas | `200` |
| 2 | `GET` | `/api/guilds/{guildId}` | Viena grupė | `200` |
| 3 | `POST` | `/api/guilds` | Sukurti grupę | `201` + `Location` |
| 4 | `PUT` | `/api/guilds/{guildId}` | Atnaujinti grupę | `200` |
| 5 | `DELETE` | `/api/guilds/{guildId}` | Ištrinti grupę su visu turiniu | `204` |

### Užduotys (Quest)

| # | Metodas | Kelias | Aprašymas | Sėkmė |
| --- | --- | --- | --- | --- |
| 6 | `GET` | `/api/guilds/{guildId}/quests` | Sąrašas; filtrai `stat`, `type`, `difficulty`, `active` | `200` |
| 7 | `GET` | `/api/guilds/{guildId}/quests/{questId}` | Viena užduotis | `200` |
| 8 | `POST` | `/api/guilds/{guildId}/quests` | Sukurti užduotį | `201` + `Location` |
| 9 | `PUT` | `/api/guilds/{guildId}/quests/{questId}` | Atnaujinti užduotį | `200` |
| 10 | `DELETE` | `/api/guilds/{guildId}/quests/{questId}` | Ištrinti užduotį su įrašais | `204` |

### Progreso įrašai (LogEntry)

| # | Metodas | Kelias | Aprašymas | Sėkmė |
| --- | --- | --- | --- | --- |
| 11 | `GET` | `/api/guilds/{guildId}/quests/{questId}/logs` | Sąrašas; filtras `status` | `200` |
| 12 | `GET` | `/api/guilds/{guildId}/quests/{questId}/logs/{logId}` | Vienas įrašas | `200` |
| 13 | `POST` | `/api/guilds/{guildId}/quests/{questId}/logs` | Registruoti progresą (visada `PENDING`, 0 XP) | `201` + `Location` |
| 14 | `PUT` | `/api/guilds/{guildId}/quests/{questId}/logs/{logId}` | Atnaujinti įrašą ir DM peržiūros rezultatą | `200` |
| 15 | `DELETE` | `/api/guilds/{guildId}/quests/{questId}/logs/{logId}` | Ištrinti įrašą | `204` |

Pilna specifikacija kiekvienam metodui — [`docs/openapi.json`](docs/openapi.json) arba gyvai
Swagger UI.

---

## Atsakymų struktūros

### Puslapiuotas sąrašas

Visi `GET` sąrašai grąžina vienodą struktūrą. Navigacijos nuorodos statomos iš tikrosios užklausos
URI, todėl **visi filtrai ir rikiavimas jose išlieka**. `prev` ir `next` nepateikiami, kai tokio
puslapio nėra.

```json
{
  "content": [ { "id": 2, "name": "Iron Vow", "...": "..." } ],
  "page":  { "number": 0, "size": 20, "totalElements": 3, "totalPages": 1 },
  "links": {
    "self":  "http://localhost:8080/api/guilds?name=iron&page=0&size=20",
    "first": "http://localhost:8080/api/guilds?name=iron&page=0&size=20",
    "last":  "http://localhost:8080/api/guilds?name=iron&page=0&size=20"
  }
}
```

Puslapiavimo parametrai: `?page=0&size=20&sort=name,asc`. Didžiausias leidžiamas `size` — 100.

### Klaidos

Kiekviena klaida — vienoda `ApiError` struktūra:

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

`details` pateikiamas tik tada, kai klaida susijusi su konkrečiais laukais.

| Kodas | Kada |
| --- | --- |
| `400` | Užklausos nepavyko suprasti: netinkamas JSON, blogas tipas, nežinoma enum reikšmė, lauko apribojimo pažeidimas |
| `404` | Resurso nėra **arba** neteisinga tėvinė grandinė |
| `405` | Teisingas kelias, netinkamas HTTP metodas |
| `406` | Klientas `Accept` antraštėje paprašė formato, kurio API neturi |
| `409` | Užklausa teisinga, bet prieštarauja esamai būsenai (pvz., užimtas pavadinimas) |
| `415` | Užklausos turinys (body) atsiųstas ne `application/json` tipu |
| `422` | JSON suprastas, laukai teisingi, bet pažeista dalykinės srities taisyklė |
| `500` | Netikėta serverio klaida |

**400 ar 422?** `400` — klaidą galima pastebėti žiūrint vien į užklausą. `422` — užklausa
nepriekaištinga, bet prieštarauja sistemos taisyklei (`GRIND` be `targetCount`, `occurredAt`
ateityje, XP neperžiūrėtam įrašui, naujas įrašas prie neaktyvios užduoties).

---

## Demonstracija su Postman

[`postman/DoneNDusted.postman_collection.json`](postman/DoneNDusted.postman_collection.json) —
28 užklausų, 57 patikros. Kolekcija pati susikuria duomenis ir po savęs išsivalo, todėl ją
galima paleisti kiek nori kartų iš eilės.

```bash
npx newman run postman/DoneNDusted.postman_collection.json
```

Arba Postman programoje: *Import* → *Runner* → *Run*. Trukmė — apie **5 sekundes**.

Kolekcija paeiliui iškviečia **visus 15 metodų** ir papildomai parodo:

* `201 Created` su `Location` antrašte;
* `204 No Content` — atsakymas be turinio;
* `404` neegzistuojančiam resursui, ištrintam resursui ir **neteisingai tėvinei grandinei**
  (užduotis egzistuoja, bet ne toje grupėje);
* `400` blogam payload (nurodomas konkretus laukas) ir `422` pažeistoms dalykinėms taisyklėms;
* `409` pasikartojančiam pavadinimui ir `415` ne JSON turinio tipui;
* `400`, kai užklausoje yra nedeklaruotas laukas (pvz., bandymas atsiųsti `xpAwarded`);
* kad XP ateina iš serverio: patvirtinant įrašą be `xpAwarded`, gaunamas `80` — `LEGENDARY`
  užduoties vertė.

---

## Dalykinės srities taisyklės

* Užduotis privalo deklaruoti auginamą charakteristiką: `STR`, `DEX`, `CON`, `INT`, `WIS`, `CHA`.
* Užduoties tipas: `MILESTONE`, `HABIT`, `GRIND`, `SIDE_QUEST`. `targetCount` privalomas ir
  leidžiamas **tik** `GRIND` tipui.
* Sudėtingumas yra bazinio XP daugiklis: `TRIVIAL` ×1, `EASY` ×2, `MEDIUM` ×3, `HARD` ×5,
  `LEGENDARY` ×8; bazinis XP — 10.
* **XP niekada nepriimamas iš kliento kuriant įrašą.** `LogEntryCreateRequest` tokio lauko
  apskritai neturi; naujas įrašas visada gimsta `PENDING` su 0 XP.
* DM gali perrašyti XP **tik** tvirtinimo metu. Praleidus `xpAwarded`, serveris jį išveda iš
  užduoties sudėtingumo.
* Patvirtintas XP **užšaldomas** įraše. Vėliau pakeitus užduoties sudėtingumą, istorija
  neperrašoma.
* `PENDING` ir `REJECTED` įrašai privalo likti be XP ir be inspiration — taisyklė užtikrinta ir
  Java kode, ir `CHECK` apribojimais duomenų bazėje.

---

## Technologijos

| Sluoksnis | Sprendimas |
| --- | --- |
| Kalba | Java 21 (LTS) |
| Karkasas | Spring Boot 4.1.1 (Web MVC, Data JPA, Validation) |
| Duomenų bazė | PostgreSQL 18 |
| Migracijos | Flyway (schema + demonstraciniai duomenys) |
| Dokumentacija | springdoc-openapi 3.1.1 (OpenAPI 3.1 + Swagger UI) |
| Surinkimas | Maven Wrapper (`mvnw`) |

## Projekto struktūra

```
src/main/java/lt/ktu/donendusted/
  config/        EmbeddedPostgresConfig, OpenApiConfig
  domain/        Guild, Quest, LogEntry + Stat, QuestType, Difficulty, LogStatus
  repository/    trys Spring Data sąsajos
  service/       transakcijos ir dalykinės taisyklės
  web/           kontroleriai, PagedResponse
  web/dto/       užklausų ir atsakymų įrašai (records)
  error/         ApiError, ApiExceptionHandler, trys išimtys
src/main/resources/db/migration/
  V1__schema.sql        lentelės, indeksai, CHECK apribojimai
  V2__seed_data.sql     demonstraciniai duomenys
docs/
  openapi.json          sugeneruota OpenAPI specifikacija
  WALKTHROUGH.md        detalus kiekvieno sprendimo paaiškinimas
  REVIEW.md             patikros ataskaita pagal užduoties reikalavimus
postman/
  DoneNDusted.postman_collection.json
```

## Ko šiame darbe dar nėra

LD0 ataskaitoje aprašyta sistema yra platesnė nei LD1 apimtis. Sąmoningai **neįgyvendinta**:

* autentifikacija (JWT su access/refresh žetonais) ir rolės `PLAYER` / `DUNGEON_MASTER` / `ADMIN`;
* `User`, `GuildMembership`, `Friendship` objektai;
* veikėjo kortelė (character sheet) ir jos pilnas / viešas pjūviai;
* React.js kliento pusė.

Visos likusios taisyklės yra autorizacijos taisyklės („kas turi teisę tai daryti“), todėl jos
ateina kartu su autentifikacija kitame laboratoriniame darbe. Sprendimo pagrindimas —
[`docs/WALKTHROUGH.md`](docs/WALKTHROUGH.md), skyrius 2.2.

---

Autorė: **Miglė Cirtautaitė** · Dėstytojas: Tomas Blažauskas · Kaunas, 2026
