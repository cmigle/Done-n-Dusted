# Done n Dusted — API dokumentacija

KTU T120B165 *Saityno taikomųjų programų projektavimas*. Šis dokumentas aprašo REST API: paleidimą,
autentifikaciją, roles, visus metodus, atsakymų ir klaidų struktūrą. Angliška projekto apžvalga —
[`README.md`](../README.md), naudotojo sąsaja (LD3) — [`LD3.md`](LD3.md).

**Done n Dusted** — DnD žaidimo principu veikianti produktyvumo sistema. Žaidėjas registruoja
realaus gyvenimo progresą, grupės Dungeon Master jį patvirtina, o tik patvirtinti įrašai keičia
veikėjo charakteristikas.

Resursų hierarchija: **Guild (1:N) → Quest (1:N) → LogEntry**. Naudotojai grupėms priklauso per
**GuildMembership** su role `PLAYER` arba `DUNGEON_MASTER`.

| Darbas | Šaka / žyma | Gynimo medžiaga |
| --- | --- | --- |
| LD1 — REST API | `ld1`, `v0.1.0` | [`visual/GYNIMO_GIDAS.md`](visual/GYNIMO_GIDAS.md), [`visual/DEMO_KOMANDOS.txt`](visual/DEMO_KOMANDOS.txt) |
| LD2 — autentifikacija ir autorizacija | `ld2`, `v0.2.1` | [`LD2.md`](LD2.md), [`DEMO_PLANAS.md`](DEMO_PLANAS.md), [`gynimas/LD2_gynimas.html`](gynimas/LD2_gynimas.html) |
| LD3 — naudotojo sąsaja ir diegimas | `ld3`, `v0.3.0` | [`LD3.md`](LD3.md) |

---

## Greitas paleidimas

Reikia tik **Java 21**. Nei Maven, nei Docker, nei įdiegtos PostgreSQL neprireikia.

```bash
./mvnw spring-boot:run
```

Windows: tą patį daro `demo/1-paleisti-api.cmd` (atlaisvina prievadus ir pats atidaro Swagger);
kiti `demo/` skriptai paleidžia Postman testus, curl aplinką su žetonais ir psql.

Pirmas paleidimas atsisiunčia Maven ir PostgreSQL 18 binarinius failus (kelias minutes),
vėlesni — sekundes. Kai pasirodo `Started DoneNDustedApplication`:

| | |
| --- | --- |
| API | <http://localhost:8080/api/guilds> (be žetono — `401`) |
| Swagger UI | <http://localhost:8080/swagger-ui.html> (žetonas — mygtukas **Authorize**) |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |
| Duomenų bazė | `jdbc:postgresql://localhost:55432/postgres` (naudotojas `postgres`) |

Numatytasis `dev` profilis paleidžia **tikrą PostgreSQL** serverio procesą, o Flyway iškart
sukuria schemą ir užpildo ją demonstraciniais duomenimis (4 naudotojai, 3 grupės, 9 užduotys,
12 įrašų).

### Demonstraciniai naudotojai

Visų slaptažodis — `Demo123!`.

| ID | Vardas | Globali rolė | Grupėse |
| --- | --- | --- | --- |
| 1 | `admin` | `ADMIN` | — (mato visas) |
| 2 | `rokas` | `USER` | DM ir savininkas: *Thursday Night Crew*, *Iron Vow*; žaidėjas: *The Quiet Library* |
| 3 | `ieva` | `USER` | DM ir savininkė: *The Quiet Library*; žaidėja: kitose dviejose |
| 4 | `jonas` | `USER` | Žaidėjas: *Thursday Night Crew*, *Iron Vow* |

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" -d '{"username":"rokas","password":"Demo123!"}'
# -> {"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":900,"refreshToken":"..."}

curl -s http://localhost:8080/api/guilds -H "Authorization: Bearer <accessToken>"
```

### Paleidimas su tikra, nuolatine duomenų baze

```bash
docker compose up -d                      # arba savas PostgreSQL serveris
JWT_SECRET=<bent 32 simboliai> ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

Prisijungimo duomenys imami iš `DB_URL`, `DB_USER`, `DB_PASSWORD` aplinkos kintamųjų.
`prod` profilyje JWT parašo raktas **privalomas** (`JWT_SECRET`) — be jo programa nepasileidžia.

---

## Autentifikacija

| Žetonas | Kas tai | Galioja | Kur saugomas |
| --- | --- | --- | --- |
| Access | JWT, pasirašytas HS256. Turinys: `sub` (naudotojo ID), `username`, `role`, `iss`, `iat`, `exp` | 15 min | Serveris nesaugo — kiekvienoje užklausoje tikrina parašą ir galiojimą |
| Refresh | Atsitiktinė 256 bitų eilutė (ne JWT) | 7 d. | DB lentelė `refresh_token`, tik SHA-256 maiša |

* Access žetonas siunčiamas antraštėje `Authorization: Bearer <accessToken>`.
* **Rotacija:** `POST /api/auth/refresh` už galiojantį refresh žetoną išduoda naują porą, o senąjį
  atšaukia. Jei atšauktas žetonas atsiunčiamas dar kartą, jis laikomas pavogtu — atšaukiamos
  **visos** to naudotojo sesijos.
* **Atsijungimas** atšaukia refresh žetoną. Access žetonas galioja iki savo `exp`, todėl jis trumpas.
* Slaptažodžiai saugomi tik kaip **BCrypt** maiša su druska. Neteisingam vardui ir slaptažodžiui
  grąžinama ta pati klaida, o nežinomo vardo atveju vis tiek atliekamas BCrypt palyginimas, kad
  atsakymo laikas neišduotų, ar naudotojas egzistuoja.
* Užblokuotas naudotojas (`enabled = false`) prisijungti ir atnaujinti žetonų negali (`403`).
* API neturi sesijų ir slapukų (`STATELESS`), todėl CSRF apsauga išjungta: žetoną naršyklė prideda
  ne pati, o tik klientas, kuris jį žino.

## Rolės ir teisės

| Kas | Ką gali |
| --- | --- |
| **Svečias** (be žetono) | Registruotis, prisijungti, atnaujinti žetonus, žiūrėti Swagger. Kitur — `401` |
| **Naudotojas** (`USER`) | Kurti grupes (tampa savininku ir DM), matyti savo grupes, `/api/auth/me` |
| **`PLAYER`** grupėje | Matyti grupę, jos narius ir užduotis; kurti užduotis (ne `SIDE_QUEST`); registruoti progresą; matyti **tik savo** įrašus, keisti ir trinti juos — kol jie `PENDING`; išeiti iš grupės |
| **`DUNGEON_MASTER`** grupėje | Tai, ką ir žaidėjas, plius: matyti visus grupės įrašus, juos tvirtinti / atmesti ir skirti XP bei inspiration (**ne savo**), kurti `SIDE_QUEST`, keisti ir trinti bet kurią grupės užduotį |
| **Savininkas** (grupės kūrėjas) | Keisti ir trinti grupę, pridėti ir šalinti narius, keisti jų roles |
| **`ADMIN`** | Matyti visas grupes ir jų turinį, trinti bet kurią grupę, užduotį ar įrašą, valdyti naudotojus (`/api/users`) |

* Naudotojas skirtingose grupėse gali turėti skirtingas roles. Grupėje — **vienas DM**: bandymas
  paskirti antrą grąžina `409` (užtikrinta ir DB daliniu unikaliu indeksu).
* Ne grupės narys apie ją negauna nieko — `403`.
* Grupių roles serveris kiekvienoje užklausoje ima iš DB, todėl pakeista rolė galioja iš karto.
  Globali rolė (`ADMIN`) yra žetone ir pasikeičia nuo kito access žetono.

---

## API metodai

Visi keliai prasideda `/api`. Vaikinis objektas pasiekiamas **tik per tėvinį** — neteisinga
grandinė grąžina `404`. Stulpelis **Kas** — kam leidžiama (kitiems — `403`).

### Grupės (Guild)

| # | Metodas | Kelias | Aprašymas | Kas | Sėkmė |
| --- | --- | --- | --- | --- | --- |
| 1 | `GET` | `/api/guilds` | Sąrašas; filtras `name`, puslapiavimas, rikiavimas | Savo grupės; `ADMIN` — visos | `200` |
| 2 | `GET` | `/api/guilds/{guildId}` | Viena grupė | Narys, `ADMIN` | `200` |
| 3 | `POST` | `/api/guilds` | Sukurti grupę (kūrėjas — savininkas ir DM) | Prisijungęs | `201` + `Location` |
| 4 | `PATCH` | `/api/guilds/{guildId}` | Dalinai atnaujinti grupę | Savininkas | `200` |
| 5 | `DELETE` | `/api/guilds/{guildId}` | Ištrinti grupę su visu turiniu | Savininkas, `ADMIN` | `204` |

### Užduotys (Quest)

| # | Metodas | Kelias | Aprašymas | Kas | Sėkmė |
| --- | --- | --- | --- | --- | --- |
| 6 | `GET` | `/api/guilds/{guildId}/quests` | Sąrašas; filtrai `stat`, `type`, `difficulty`, `active` | Narys, `ADMIN` | `200` |
| 7 | `GET` | `/api/guilds/{guildId}/quests/{questId}` | Viena užduotis | Narys, `ADMIN` | `200` |
| 8 | `POST` | `/api/guilds/{guildId}/quests` | Sukurti užduotį | Narys; `SIDE_QUEST` — tik DM | `201` + `Location` |
| 9 | `PATCH` | `/api/guilds/{guildId}/quests/{questId}` | Dalinai atnaujinti užduotį | Kūrėjas, DM, savininkas | `200` |
| 10 | `DELETE` | `/api/guilds/{guildId}/quests/{questId}` | Ištrinti užduotį su įrašais | Kūrėjas, DM, savininkas, `ADMIN` | `204` |

### Progreso įrašai (LogEntry)

| # | Metodas | Kelias | Aprašymas | Kas | Sėkmė |
| --- | --- | --- | --- | --- | --- |
| 11 | `GET` | `/api/guilds/{guildId}/quests/{questId}/logs` | Sąrašas; filtras `status` | Žaidėjas — savo; DM, `ADMIN` — visi | `200` |
| 12 | `GET` | `/api/guilds/{guildId}/quests/{questId}/logs/{logId}` | Vienas įrašas | Autorius, DM, `ADMIN` | `200` |
| 13 | `POST` | `/api/guilds/{guildId}/quests/{questId}/logs` | Registruoti progresą (visada `PENDING`, 0 XP) | Narys | `201` + `Location` |
| 14 | `PATCH` | `/api/guilds/{guildId}/quests/{questId}/logs/{logId}` | Turinys — autorius (kol `PENDING`); peržiūra — DM, ne savo įrašo | Autorius / DM | `200` |
| 15 | `DELETE` | `/api/guilds/{guildId}/quests/{questId}/logs/{logId}` | Ištrinti įrašą | Autorius (kol `PENDING`), DM, `ADMIN` | `204` |

### Autentifikacija (Auth)

| # | Metodas | Kelias | Aprašymas | Kas | Sėkmė |
| --- | --- | --- | --- | --- | --- |
| 16 | `POST` | `/api/auth/register` | Registruotis (rolė visada `USER`) | Svečias | `201` + `Location` |
| 17 | `POST` | `/api/auth/login` | Prisijungti — access ir refresh žetonai | Svečias | `200` |
| 18 | `POST` | `/api/auth/refresh` | Nauja žetonų pora už refresh žetoną (rotacija) | Svečias | `200` |
| 19 | `POST` | `/api/auth/logout` | Atšaukti refresh žetoną | Svečias | `204` |
| 20 | `GET` | `/api/auth/me` | Prisijungęs naudotojas ir jo grupės su rolėmis | Prisijungęs | `200` |

### Grupės nariai (Member)

| # | Metodas | Kelias | Aprašymas | Kas | Sėkmė |
| --- | --- | --- | --- | --- | --- |
| 21 | `GET` | `/api/guilds/{guildId}/members` | Narių sąrašas; filtras `role` | Narys, `ADMIN` | `200` |
| 22 | `GET` | `/api/guilds/{guildId}/members/{userId}` | Vienas narys | Narys, `ADMIN` | `200` |
| 23 | `POST` | `/api/guilds/{guildId}/members` | Pridėti narį (`username`, `role`) | Savininkas, `ADMIN` | `201` + `Location` |
| 24 | `PATCH` | `/api/guilds/{guildId}/members/{userId}` | Pakeisti nario rolę | Savininkas, `ADMIN` | `200` |
| 25 | `DELETE` | `/api/guilds/{guildId}/members/{userId}` | Pašalinti narį arba išeiti pačiam | Savininkas, `ADMIN`, pats narys | `204` |

### Naudotojai (User)

| # | Metodas | Kelias | Aprašymas | Kas | Sėkmė |
| --- | --- | --- | --- | --- | --- |
| 26 | `GET` | `/api/users` | Naudotojų sąrašas; filtras `username` | `ADMIN` | `200` |
| 27 | `GET` | `/api/users/{userId}` | Vienas naudotojas | `ADMIN` | `200` |
| 28 | `PATCH` | `/api/users/{userId}` | Keisti rolę ar užblokuoti (`role`, `enabled`) | `ADMIN` | `200` |

### Veikėjo kortelė (Character sheet)

| # | Metodas | Kelias | Aprašymas | Kas | Sėkmė |
| --- | --- | --- | --- | --- | --- |
| 29 | `GET` | `/api/guilds/{guildId}/members/{userId}/sheet` | Nario lygis, XP, charakteristikos, inspiration ir serija — tik iš jo `APPROVED` įrašų šioje grupėje. `FULL` dar turi 10 naujausių įrašų ir `pendingCount` | Narys, `ADMIN`; `FULL` — pats narys, DM, `ADMIN`, kitiems nariams `PUBLIC` | `200` |

### Draugystės (Friendship)

| # | Metodas | Kelias | Aprašymas | Kas | Sėkmė |
| --- | --- | --- | --- | --- | --- |
| 30 | `GET` | `/api/friends` | Mano draugystės ir kvietimai abiem kryptimis; filtras `status` | Prisijungęs | `200` |
| 31 | `GET` | `/api/friends/{friendshipId}` | Viena draugystė | Abi pusės (kitiems ir užblokuotajam — `404`) | `200` |
| 32 | `POST` | `/api/friends` | Pakviesti į draugus (`username`); pora jau yra — `409` | Prisijungęs | `201` + `Location` |
| 33 | `PATCH` | `/api/friends/{friendshipId}` | `ACCEPTED` — priimti kvietimą, `BLOCKED` — užblokuoti | `ACCEPTED` — gavėjas; `BLOCKED` — abi pusės | `200` |
| 34 | `DELETE` | `/api/friends/{friendshipId}` | Atšaukti ar atmesti kvietimą, nutraukti draugystę, atblokuoti | Abi pusės; užblokuotą — tik užblokavęs | `204` |
| 35 | `GET` | `/api/friends/{friendshipId}/sheet` | Draugo `PUBLIC` kortelė, sujungta per visas jo grupes | Draugas (tik `ACCEPTED`) | `200` |

### Sistema

| # | Metodas | Kelias | Aprašymas | Kas | Sėkmė |
| --- | --- | --- | --- | --- | --- |
| 36 | `GET` | `/actuator/health` | Sveikatos patikra debesų platformai — tik `{"status":"UP"}`, be detalių | Svečias | `200` |

**Atnaujinimas — `PATCH` (dalinis).** Keičiami tik atsiųsti laukai, neatsiųsti lieka tokie, kokie
buvo. Todėl, pvz., keičiant tik užduoties sudėtingumą, aprašymas niekur nedingsta. Įrašo
peržiūros laukai (`xpAwarded`, `inspirationAwarded`, `reviewerNote`) priimami tik kartu su `status`.

**Aprašymas niekada nedingsta.** Sukūrus grupę ar užduotį be `description`, jis išsaugomas kaip
tuščias tekstas `""`, todėl atsakyme laukas visada yra. Tą užtikrina ir duomenų bazė (`NOT NULL
DEFAULT ''`, migracija `V3`).

Pilna specifikacija kiekvienam metodui — [`docs/openapi.json`](openapi.json) arba gyvai
Swagger UI. Kiekvienas apsaugotas metodas dokumentuotas su `401`, o konkrečios grupės
(`/guilds/{guildId}/...`) ir naudotojų metodai — ir su `403`.

---

## Atsakymų struktūros

### Puslapiuotas sąrašas

Visi `GET` sąrašai grąžina vienodą struktūrą. Navigacijos nuorodos statomos iš tikrosios užklausos
URI, todėl **visi filtrai ir rikiavimas jose išlieka**. `prev` ir `next` nepateikiami, kai tokio
puslapio nėra. `totalElements` pirmas, kad bendras kiekis (pvz. kiek iš viso grupių) matytųsi iškart.

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

Puslapiavimo parametrai: `?page=0&size=20&sort=name,asc`. Didžiausias leidžiamas `size` — 100,
`page` — 1 000 000. Numatytasis dydis — 20, puslapiai skaičiuojami nuo 0.

Rikiuoti galima tik pagal paprastus sąrašo laukus — kitaip `400` su leidžiamų laukų sąrašu:

| Sąrašas | `sort` laukai |
| --- | --- |
| Grupės | `id`, `name`, `createdAt` |
| Užduotys | `id`, `title`, `stat`, `type`, `difficulty`, `targetCount`, `active`, `createdAt` |
| Įrašai | `id`, `occurredAt`, `createdAt`, `status`, `xpAwarded`, `reviewedAt` |
| Nariai | `joinedAt`, `role` |
| Naudotojai | `id`, `username`, `role`, `enabled`, `createdAt` |
| Draugystės | `createdAt`, `status` |

Nuorodos (`links`) yra hypermedia elementas: klientas pats nekonstruoja kito puslapio URL, o tiesiog
eina serverio duota `links.next` nuoroda, kol jos nebėra. Taip veikia ir Postman suvestinės testas.

### Klaidos

Kiekviena klaida — vienoda `ApiError` struktūra, taip pat ir `401` / `403`, kuriuos grąžina
Spring Security filtrai dar prieš kontrolerį:

```json
{
  "timestamp": "2026-09-21T09:15:30.123Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Savo iraso tvirtinti negalima",
  "path": "/api/guilds/2/quests/5/logs/13"
}
```

`details` pateikiamas tik tada, kai klaida susijusi su konkrečiais laukais.

| Kodas | Kada |
| --- | --- |
| `400` | Užklausos nepavyko suprasti: netinkamas JSON, blogas tipas, nežinoma enum reikšmė, lauko apribojimo pažeidimas |
| `401` | Nėra žetono, jis sugadintas ar pasibaigęs, blogi prisijungimo duomenys, netinkamas refresh žetonas. Kartu — antraštė `WWW-Authenticate: Bearer` |
| `403` | Naudotojas žinomas, bet teisių neužtenka: ne grupės narys, netinkama rolė grupėje, ne `ADMIN`, užblokuota paskyra |
| `404` | Resurso nėra **arba** neteisinga tėvinė grandinė |
| `405` | Teisingas kelias, netinkamas HTTP metodas |
| `406` | Klientas `Accept` antraštėje paprašė formato, kurio API neturi |
| `409` | Užklausa teisinga, bet prieštarauja esamai būsenai (užimtas pavadinimas ar vardas, antras DM, jau narys) |
| `415` | Užklausos turinys (body) atsiųstas ne `application/json` tipu |
| `422` | JSON suprastas, laukai teisingi, bet pažeista dalykinės srities taisyklė |
| `500` | Netikėta serverio klaida |

**400 ar 422?** `400` — klaidą galima pastebėti žiūrint vien į užklausą. `422` — užklausa
nepriekaištinga, bet prieštarauja sistemos taisyklei (`GRIND` be `targetCount`, `occurredAt`
ateityje, XP neperžiūrėtam įrašui, naujas įrašas prie neaktyvios užduoties, patvirtinto įrašo
turinio keitimas, nežinomas naudotojas pridedant narį).

**401 ar 403?** `401` — serveris nežino, kas jūs. `403` — žino, bet to daryti neleidžia.

---

## Demonstracija su Postman

[`postman/DoneNDusted.postman_collection.json`](../postman/DoneNDusted.postman_collection.json) —
78 užklausos, 176 patikros (švariai paleistoje DB). Kolekcija kaskart užregistruoja naują
naudotoją, pati susikuria duomenis ir po savęs išsivalo, todėl ją galima paleisti kiek nori kartų
iš eilės.

```bash
npx newman run postman/DoneNDusted.postman_collection.json
```

Arba Postman programoje: *Import* → *Runner* → *Run*. Trukmė — apie **10 sekundžių**.

Ta pati kolekcija Postman v3 formatu (atskiri YAML failai) yra
[`postman/Done n Dusted API (LD2)/`](../postman/Done%20n%20Dusted%20API%20%28LD2%29/); JSON failas
sugeneruotas iš tų pačių šaltinių. Ją galima paleisti ir Postman CLI:
`postman collection run "postman/Done n Dusted API (LD2)"`.

Kolekcijos lygyje nustatyta *Bearer Token* `{{accessToken}}`. Užklausos, kurias siunčia žaidėjas
`jonas` ar `admin`, turi savo *Authorization* (`{{playerToken}}`, `{{adminToken}}`).

| Aplankas | Kas parodoma |
| --- | --- |
| 0. Autentifikacija | Registracija, `409`, `400`, prisijungimas ir JWT turinys, `/auth/me`, `401` be žetono ir su sugadintu žetonu, refresh rotacija ir pakartotinio panaudojimo aptikimas, atsijungimas |
| 1. Suvestinė | `admin` vardu: kiek DB yra grupių, užduočių ir įrašų su visais ID (`Is viso - grupiu: 3, uzduociu: 9, irasu: 12`) |
| 2. Grupės | LD1 grupių CRUD, kūrėjas tampa savininku, naudotojas mato tik savo grupes, puslapiavimas per `links.next` |
| 3. Nariai | Žaidėjo pridėjimas, `409` jau narys ir antras DM, `422` nežinomas naudotojas, `403` žaidėjui |
| 4. Užduotys | LD1 užduočių testai, `createdBy` |
| 5. Progreso įrašai | Įrašą registruoja žaidėjas, tvirtina DM; XP iš sudėtingumo; patvirtinto įrašo turinys užšaldytas (`422`) |
| 6. Rolės ir teisės | `403`: ne narys, žaidėjas tvirtina, DM keičia svetimą turinį, DM tvirtina savo įrašą, žaidėjas žiūri svetimą įrašą, `SIDE_QUEST`, grupės keitimas, `/api/users`; žaidėjas mato tik savo įrašus; išėjimas iš grupės |
| 7. Išvalymas | Ištrynimas `204`, antrą grupę trina `admin`, `404` po ištrynimo |
| 8. Administravimas | Naudotojų sąrašas, `422` bandant užblokuoti save, užblokavimas → prisijungti `403` → atblokavimas |

Komandos be Postman (curl) — [`docs/visual/DEMO_KOMANDOS.txt`](visual/DEMO_KOMANDOS.txt).

---

## Dalykinės srities taisyklės

* Užduotis privalo deklaruoti auginamą charakteristiką: `STR`, `DEX`, `CON`, `INT`, `WIS`, `CHA`.
* Užduoties tipas: `MILESTONE`, `HABIT`, `GRIND`, `SIDE_QUEST`. `targetCount` privalomas ir
  leidžiamas **tik** `GRIND` tipui. `SIDE_QUEST` kuria tik grupės DM.
* Sudėtingumas yra bazinio XP daugiklis: `TRIVIAL` ×1, `EASY` ×2, `MEDIUM` ×3, `HARD` ×5,
  `LEGENDARY` ×8; bazinis XP — 10.
* **XP niekada nepriimamas iš kliento kuriant įrašą.** `LogEntryCreateRequest` tokio lauko
  apskritai neturi; naujas įrašas visada gimsta `PENDING` su 0 XP, o jo autorius — prisijungęs
  naudotojas.
* DM gali perrašyti XP **tik** tvirtinimo metu. Praleidus `xpAwarded`, serveris jį išveda iš
  užduoties sudėtingumo. Įrašo **negali tvirtinti jo autorius**, net jei jis yra DM.
* Patvirtintas XP **užšaldomas** įraše. Vėliau pakeitus užduoties sudėtingumą, istorija
  neperrašoma. Peržiūrėto įrašo turinio (įrodymų) autorius pakeisti nebegali.
* `PENDING` ir `REJECTED` įrašai privalo likti be XP ir be inspiration — taisyklė užtikrinta ir
  Java kode, ir `CHECK` apribojimais duomenų bazėje.

---

## Technologijos

| Sluoksnis | Sprendimas |
| --- | --- |
| Kalba | Java 21 (LTS) |
| Karkasas | Spring Boot 4.1.1 (Web MVC, Data JPA, Validation) |
| Saugumas | Spring Security 7 + OAuth2 Resource Server (Nimbus JOSE JWT, HS256), BCrypt |
| Duomenų bazė | PostgreSQL 18 |
| Migracijos | Flyway (schema + demonstraciniai duomenys) |
| Dokumentacija | springdoc-openapi 3.1.1 (OpenAPI 3.1 + Swagger UI su Bearer autorizacija) |
| Surinkimas | Maven Wrapper (`mvnw`) |

## Projekto struktūra

```
src/main/java/lt/ktu/donendusted/
  config/        CorsConfig, EmbeddedPostgresConfig, OpenApiConfig, SecurityConfig
  security/      TokenService (JWT ir refresh žetonai), CurrentUser, JwtProperties, ApiAuthErrorHandler
  domain/        Guild, Quest, LogEntry, User, GuildMembership, RefreshToken, Friendship + enum
  repository/    Spring Data sąsajos
  service/       transakcijos, dalykinės taisyklės ir teisės (GuildAccess), CharacterSheetService, FriendshipService
  web/           kontroleriai (FriendController — draugystės), PagedResponse
  web/dto/       užklausų ir atsakymų įrašai (records)
  error/         ApiError, ApiExceptionHandler, išimtys (401, 403, 404, 409, 422)
src/main/resources/db/migration/
  V1__schema.sql        lentelės, indeksai, CHECK apribojimai
  V2__seed_data.sql     demonstraciniai duomenys
  V3__description_not_null.sql  aprašymas NOT NULL DEFAULT ''
  V4__users_and_tokens.sql      naudotojai (BCrypt) ir refresh žetonai
  V5__guild_membership.sql      narystės, savininkai, autoriai ir seed ryšiai
  V6__friendship.sql            draugystės; seed: ieva ir jonas — draugai, rokas pakvietė ievą
src/test/java/lt/ktu/donendusted/
  *IntegrationTest      MockMvc testai su įterptine PostgreSQL (vienas Spring kontekstas): ./mvnw test
  service/              veikėjo kortelės formulių vienetų testai
.github/workflows/      CI: backend testai, frontend lint/testai/build, Postman ir Playwright
demo/                   Windows paleidimo skriptai demonstracijai (API, newman, curl, psql)
frontend/               React + TypeScript + Vite klientas (LD3), Vitest ir Playwright testai
docs/
  API.md                šis dokumentas
  LD3.md                LD3 reikalavimai, sąsaja, diegimas ir gynimas
  wireframes/           LD3 langų wireframe'ai (SVG) ir jų generatorius
  screenshots/          programos ekrano nuotraukos
  LD2.md                LD2 pakeitimai ir gynimo klausimai
  DEMO_PLANAS.md        ką ir kokia tvarka rodyti gynime
  gynimas/              LD2 gynimo puslapis (HTML) ir jo šaltiniai (Archify)
  openapi.json          sugeneruota OpenAPI specifikacija
  visual/               LD1 gynimo gidas, demo komandos ir diagramos
postman/
  Done n Dusted API (LD2)/              kolekcija Postman v3 formatu (YAML)
  DoneNDusted.postman_collection.json   ta pati kolekcija v2.1 JSON (newman)
```

## Naudotojo sąsaja (LD3)

React klientas yra kataloge [`frontend/`](../frontend/), jo reikalavimai, wireframe'ai ir diegimas į
debesį aprašyti [`LD3.md`](LD3.md).

---

Autorė: **Miglė Cirtautaitė** · Dėstytojas: Tomas Blažauskas · Kaunas, 2026
