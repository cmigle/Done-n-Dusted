# LD2 demonstracijos planas

Ką gynime parodyti veikiant, kokia tvarka ir ką turi matyti. Visos curl komandos yra
[`visual/DEMO_KOMANDOS.txt`](visual/DEMO_KOMANDOS.txt), paaiškinimai „kodėl taip“ —
[`LD2.md`](LD2.md) ir [`gynimas/LD2_gynimas.html`](gynimas/LD2_gynimas.html).

Seed naudotojai (visų slaptažodis `Demo123!`):

| ID | Vardas | Kas jis |
| --- | --- | --- |
| 1 | `admin` | `ADMIN` — mato ir trina viską, valdo naudotojus |
| 2 | `rokas` | Grupių 1 ir 2 savininkas ir DM, grupėje 3 — žaidėjas |
| 3 | `ieva` | Grupės 3 savininkė ir DM, grupėse 1 ir 2 — žaidėja |
| 4 | `jonas` | Žaidėjas grupėse 1 ir 2, grupės 3 **ne narys** |

Grupė 2 — Iron Vow; užduotis 4 — „Run 50 kilometers“; įrašas 7 — jono, `PENDING`.

---

## Paleidimo skriptai (`demo/`)

| Skriptas | Ką daro |
| --- | --- |
| `1-paleisti-api.cmd` | Atlaisvina prievadus 8080 ir 55432, paleidžia API su švaria DB ir, kai ji pasiruošusi, pats atidaro Swagger |
| `2-postman-testai.cmd` | Visa Postman kolekcija per newman: 78 užklausos, 176 patikros |
| `3-curl-komandos.cmd` | Atidaro `DEMO_KOMANDOS.txt` ir Git Bash, kuriame jau prisijungta: `$A` — rokas, `$J` — jonas, `$ADM` — admin. Komanda `jwt` parodo žetono turinį, `zetonai` — prisijungia iš naujo |
| `4-duomenu-baze.cmd` | psql: BCrypt maišos, narystės, refresh žetonų maišos, DB apribojimai; po to galima rašyti savo SQL |
| `0-sustabdyti.cmd` | Sustabdo API ir jos DB, jei langas uždarytas netvarkingai |

---

## Prieš gynimą (5 min)

- [ ] `1-paleisti-api` — palaukti, kol atsidarys Swagger. DB kiekvieną kartą švari.
- [ ] Postman: **Import** → `postman/DoneNDusted.postman_collection.json` (jei dar neimportuota).
      Kolekcija „Done n Dusted API (LD2)“, `baseUrl` = `http://localhost:8080/api`.
- [ ] Atsidaryti `gynimas/LD2_gynimas.html` (atsakymai į klausimus) ir `3-curl-komandos`.
- [ ] Po Postman *Run collection* DB lieka papildomų `postman_...` naudotojų. Prieš rodant rankiniu
      būdu — `1-paleisti-api` dar kartą (švari DB).

---

## 1. Dokumentacija (1 min) — Swagger

| Ką daryti | Ką turi matyti |
| --- | --- |
| Atidaryti `http://localhost:8080/swagger-ui.html` | 28 metodai; prie apsaugotų — spynelė, viršuje **Authorize** |
| Atidaryti bet kurį metodą, pvz. `GET /api/guilds/{guildId}` | Atsakymuose yra `401` ir `403` su `ApiError` schema |
| Grupė **Auth** | `register`, `login`, `refresh`, `logout` — be spynelės (vieši), `me` — su spynele |

Pasakyti: OpenAPI `bearerAuth` schema; `401`/`403` prie metodų prideda `OpenApiConfig`, todėl jie
nepamirštami. Tas pats failas — `docs/openapi.json`.

---

## 2. Autentifikacija (2 min) — Swagger

| # | Ką daryti | Ką turi matyti |
| --- | --- | --- |
| 2.1 | Neprisijungus: `GET /api/guilds` → *Execute* | `401`, `"Reikalingas prisijungimas: Authorization: Bearer <accessToken>"`, antraštė `www-authenticate: Bearer` |
| 2.2 | `POST /api/auth/register` `{"username":"gynimas","password":"Demo123!"}` | `201`, `Location: http://localhost:8080/api/users/5`, `role: USER`, slaptažodžio ir maišos atsakyme nėra |
| 2.3 | Tas pats dar kartą; vardą parašyti `Gynimas` | `409` — vardas unikalus nepaisant raidžių dydžio |
| 2.4 | `register` su `"password":"123"` | `400`, `details` nurodo lauką `password` (8–72 simboliai) |
| 2.5 | `POST /api/auth/login` `{"username":"jonas","password":"blogas"}` | `401`, `"Neteisingas naudotojo vardas arba slaptazodis"` — tas pats ir nežinomam vardui |
| 2.6 | `login` `{"username":"jonas","password":"Demo123!"}` (Swagger pavyzdys jau toks) | `200`: `accessToken`, `tokenType: Bearer`, `expiresIn: 900`, `refreshToken` |
| 2.7 | Nukopijuoti `accessToken` → **Authorize** → įklijuoti → `GET /api/guilds` | `200`, tik grupės 1 ir 2 (jonas grupės 3 nemato) |
| 2.8 | `GET /api/auth/me` | jonas, `role: USER`, `memberships`: dvi grupės su `PLAYER` |
| 2.9 | Git Bash (`3-curl-komandos`) → `jwt` | JWT turinys: `iss: done-n-dusted`, `sub: "2"`, `username: rokas`, `role: USER`, `iat`, `exp` (`exp - iat = 900`). Žetono neklijuoti į svetimas svetaines (jwt.io) |
| 2.10 | Postman → `0. Autentifikacija` → **Prisijungti** → *Test Results* | Testai patys iškoduoja JWT ir patikrina tuos pačius laukus |

Pasakyti: access žetonas — HS256 JWT, 15 min, serveris jo nesaugo; JWT pasirašytas, ne
užšifruotas, todėl jame nėra slaptažodžio. Slaptažodis DB — tik BCrypt maiša.

---

## 3. Žetonų atnaujinimas ir atsijungimas (1–2 min)

Patogiausia Postman aplanke `0. Autentifikacija` (užklausos eina iš eilės ir pačios perduoda
žetonus) arba curl `5.6` (`3-curl-komandos`).

| # | Ką daryti | Ką turi matyti |
| --- | --- | --- |
| 3.1 | `POST /api/auth/refresh` su `refreshToken` iš prisijungimo (R1) | `200`, nauja pora: access ir R2 |
| 3.2 | Tas pats R1 dar kartą | `401`, `"Refresh zetonas jau panaudotas arba atsauktas - del saugumo atsauktos visos sesijos"` |
| 3.3 | Dabar R2 | Irgi `401` — pakartotinis panaudojimas reiškia vagystę, atšaukiami **visi** naudotojo refresh žetonai |
| 3.4 | Prisijungti iš naujo → `POST /api/auth/logout` su refresh | `204` |
| 3.5 | `refresh` su ką tik atjungtu žetonu | `401` |
| 3.6 | Sugadinti access žetoną (pridėti raidę) ir kviesti `GET /api/guilds` | `401`, `"Access zetonas neteisingas"`, `www-authenticate: Bearer error="invalid_token"` |

Pasakyti: refresh žetonas — atsitiktiniai 32 baitai, DB saugoma tik SHA-256 maiša; kiekvienas
panaudojamas vieną kartą (rotacija). Access po atsijungimo galioja iki 15 min — tai sąmoninga kaina.

---

## 4. Rolės ir teisės (3 min) — curl arba Postman `6. Rolės ir teisės`

curl: `3-curl-komandos` → kopijuoti iš `DEMO_KOMANDOS.txt` 5 skyriaus. `$A` — rokas, `$J` — jonas.

| Taisyklė | curl | Ką turi matyti |
| --- | --- | --- |
| Svečias — tik prisijungimas ir dokumentacija | `5.1` | `401` |
| Ne narys nemato grupės | `5.7` jonas → `GET /guilds/3` | `403`, `"Nesate grupes 3 narys"` |
| Žaidėjas netvirtina įrašų | `5.8` jonas → `PATCH /guilds/2/quests/4/logs/7` `APPROVED` | `403`, `"Irasus perziuri tik grupes DM"` |
| DM tvirtina | `5.8` komanda su `$A` vietoj `$J` (rokas tvirtina jono įrašą 7) | `200`, `APPROVED`, `xpAwarded: 50` paskaičiavo serveris, `reviewerId: 2` |
| DM netvirtina savo įrašo | `5.9` | `403`, `"Savo iraso tvirtinti negalima"` |
| Žaidėjas mato tik savo įrašus, DM — visus | `5.10` | jono sąraše visų `authorId: 4`, roko — visi įrašai |
| `SIDE_QUEST` kuria tik DM | `5.11` | `403`, `"SIDE_QUEST uzduotis gali kurti tik grupes DM"` |
| Grupėje vienas DM | `5.12` | narių sąrašas, po to `409`, `"Grupe jau turi DM ..."` |
| Grupę keičia tik savininkas | jonas → `PATCH /guilds/2` | `403`, `"Grupe keisti gali tik jos savininkas"` |
| Savininko pašalinti negalima | rokas → `DELETE /guilds/2/members/2` | `422`, `"Grupes savininko pasalinti negalima"` |
| Naudotojų valdymas — tik ADMIN | `5.13` | jonas `403` (`"Neturite teises atlikti sio veiksmo"`), admin `200` |
| ADMIN mato visas grupes | `5.14` | admin mato visas, jonas — tik savo |

Pasakyti: globali rolė (`USER`/`ADMIN`) yra JWT `role` claim; rolė grupėje (`PLAYER`/`DUNGEON_MASTER`)
imama iš DB, nes skirtingose grupėse ji skirtinga ir gali keistis. Visos grupės užklausos eina
per `GuildService.access()`: `404` — grupės nėra, `403` — ne narys, toliau konkreti patikra.

---

## 5. Administratorius (1 min) — Postman `8. Administravimas` arba Swagger su `admin`

| Ką daryti | Ką turi matyti |
| --- | --- |
| `GET /api/users` | Visi naudotojai, be slaptažodžių |
| `PATCH /api/users/4` `{"enabled":false}` | `200`, jonas užblokuotas, jo refresh žetonai atšaukti |
| `login` kaip jonas | `403`, `"Paskyra uzblokuota"` |
| `PATCH /api/users/1` `{"enabled":false}` (pats save) | `422`, `"Savo administratoriaus teisiu nuimti ar paskyros uzblokuoti negalima"` |
| `PATCH /api/users/4` `{"enabled":true}` | Jonas vėl gali prisijungti |

---

## 6. Duomenų bazė (1 min) — `4-duomenu-baze`

| Ką rodo | Ką pasakyti |
| --- | --- |
| `app_user.password_hash` = `$2a$10$...` | BCrypt: algoritmas, kaina 10, druska ir maiša; slaptažodžio atgauti negalima |
| `guild_membership` | Ta pati rokas — DM grupėse 1, 2 ir žaidėjas grupėje 3 |
| `refresh_token.token_hash` ir `revoked_at` | DB tik SHA-256 maiša; po rotacijos ar atsijungimo matosi `revoked_at` |
| `log_entry` autorius ir peržiūrėjęs | `PENDING` įrašas neturi peržiūrėjusio (DB `CHECK`) |
| Indeksai ir apribojimai | `lower(username)` unikalus, dalinis indeksas — vienas DM grupėje |

DBeaver alternatyva: PostgreSQL, `localhost:55432`, DB ir naudotojas `postgres`, be slaptažodžio.

---

## 7. Automatiniai testai (1 min) — `2-postman-testai` arba Postman *Run collection*

Turi matyti: **176 patikros, 0 klaidų**, ~20 s. Kolekcijoje 78 užklausos; newman lentelėje
`requests` rodo 91, nes „Suvestinė“ papildomai kviečia sąrašus per `pm.sendRequest`. Aplankai: `0. Autentifikacija`,
`1. Suvestinė` („Is viso - grupiu: 3, uzduociu: 9, irasu: 12“), `2. Grupės` … `8. Administravimas`.
Kolekciją galima leisti kartotinai — ji pati susikuria vardus ir pasivalo.

---

## 8. LD1 dalis vis dar veikia (jei paprašys)

`DEMO_KOMANDOS.txt` 1–4 skyriai (visi jau su `-H "$A"`): CRUD, `PATCH`, puslapiavimas su
`links.next`, filtrai, `404` per neteisingą grandinę, `400`/`409`/`415`/`422`.

---

## 9. Versijų valdymas (jei paklaus)

`git log --oneline --graph` arba GitHub: kiekviena LD2 dalis — atskira šaka (`feat/users-and-tokens`,
`feat/jwt-auth`, `feat/guild-roles`, `feat/guild-members`, `feat/admin-users`, `feat/openapi-auth`,
`fix/guild-delete`, `feat/postman-ld2`, `docs/...`), sujungta į `main`. Žymos `v0.1.0` (LD1), `v0.2.0` (LD2).

---

## Jei kas nors neveikia

| Bėda | Ką daryti |
| --- | --- |
| `Port 8080 was already in use` arba DB nepasileidžia | `0-sustabdyti`, tada `1-paleisti-api` |
| `401 "Access zetonas pasibaiges ..."` | Praėjo 15 min: prisijungti iš naujo (Git Bash — parašyti `zetonai`) |
| Postman visur `401` | Pirma paleisti `0. Autentifikacija` → „Prisijungti“ (užpildo `accessToken`) |
| Postman pavienė užklausa iš `6.` aplanko meta klaidą | Ji naudoja ankstesnių aplankų kintamuosius — leisti visą kolekciją arba aplankus iš eilės |
| Swagger vis dar rodo seną žetoną | **Authorize** → *Logout* → įklijuoti naują |
| Swagger „Failed to load API definition“ | Viršutiniame laukelyje turi būti `/v3/api-docs` (jis skirtas API aprašui, ne užklausoms) → **Explore** |
| Prisijungus rodo tik 2 grupes | Swagger prisijungimo pavyzdyje įrašytas `jonas` — rokui pakeisti `username` |
| Duomenys pasikeitė po testų | `1-paleisti-api` iš naujo — DB švari |
