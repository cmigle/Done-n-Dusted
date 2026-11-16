# LD2 — autentifikacija ir autorizacija

Kas pridėta prie LD1, kaip tai veikia ir ką sakyti gynime. LD1 dalis (REST, puslapiavimas,
klaidos) aprašyta [`visual/GYNIMO_GIDAS.md`](visual/GYNIMO_GIDAS.md); diagramos `visual/*.html`
vaizduoja LD1 branduolį (be naudotojų lentelių).

Tas pats turinys su diagramomis (sluoksniai, atsakymo kodų medis, žetonų ciklas, DB schema) ir kodo
ištraukomis — [`gynimas/LD2_gynimas.html`](gynimas/LD2_gynimas.html). Puslapis atsidaro naršyklėje be
interneto; šaltinis `gynimas/ld2-gynimas.archify.md`.

---

## 1. Kas pridėta

| Reikalavimas | Kaip įgyvendinta |
| --- | --- |
| Autentifikacija | `POST /api/auth/register`, `/login`, `/refresh`, `/logout`, `GET /api/auth/me` |
| JWT | Access žetonas — HS256 JWT, 15 min. Pasirašo `TokenService`, tikrina Spring Security (`JwtDecoder`) |
| Žetonų atnaujinimas | Refresh žetonas (7 d.), DB saugoma SHA-256 maiša, **rotacija** ir pakartotinio panaudojimo aptikimas |
| Rolės | Svečias, `USER`, grupėje `PLAYER` / `DUNGEON_MASTER`, globalus `ADMIN` |
| Resursų apsauga | Kiekvienas grupės veiksmas eina per `GuildService.access()`: `404` — grupės nėra, `403` — ne narys; toliau — konkreti rolės ar nuosavybės patikra |
| Saugus slaptažodžių saugojimas | BCrypt su druska; atsakymuose maišos nėra |
| Klaidos | `401` ir `403` — ta pati `ApiError` struktūra, `401` su `WWW-Authenticate: Bearer` |
| Dokumentacija | OpenAPI `bearerAuth` schema, Swagger **Authorize**, `401` / `403` kiekviename metode |
| Demonstracija | Postman: 78 užklausos, 176 patikros; curl — `visual/DEMO_KOMANDOS.txt` 0.5 ir 5 skyriai |

Nauji API metodai: #16–#28 (README lentelės) — 5 autentifikacijos, 5 narių, 3 naudotojų valdymo.

---

## 2. Kaip keliauja apsaugota užklausa

Pavyzdys: `PATCH /api/guilds/2/quests/4/logs/7` su `{"status":"APPROVED"}`.

1. **`BearerTokenAuthenticationFilter`** (Spring Security) paima `Authorization: Bearer ...`.
   Nėra antraštės → `ExceptionTranslationFilter` → `ApiAuthErrorHandler` → `401`.
2. **`JwtDecoder`** (`SecurityConfig`) patikrina parašą (HS256 raktas), `exp` ir `iss`. Netinka →
   `401 "Access zetonas neteisingas"` arba `"... pasibaiges"`.
3. `JwtAuthenticationConverter` claim `role` paverčia `ROLE_USER` / `ROLE_ADMIN`.
   `/api/users/**` čia pat reikalauja `ROLE_ADMIN`, kitaip `403`.
4. Kontroleris — kaip LD1. Servisas kviečia **`guilds.access(2)`**: randa grupę (`404`), iš DB paima
   naudotojo rolę joje (`403`, jei ne narys ir ne `ADMIN`) ir grąžina `GuildAccess`.
5. `LogEntryService.update()`: peržiūros laukai → `access.require(access.isDm(), ...)` ir
   `access.require(!access.is(entry.getAuthor()), "Savo iraso tvirtinti negalima")`.
   Nepraeina → `ForbiddenException` → `ApiExceptionHandler` → `403`.
6. Tik po teisių tikrinamos dalykinės taisyklės (`422`) ir atliekamas pakeitimas.

Kodėl teisės servise, o ne `@PreAuthorize`: jos priklauso nuo duomenų (kas įrašo autorius, kas
grupės DM), o tie duomenys jau įkraunami servise. Viena Java eilutė su paaiškinimu lengviau
skaitoma ir testuojama nei SpEL išraiška.

Kodėl `401`/`403` iš filtrų eina per `HandlerExceptionResolver`: filtrai suveikia dar prieš
kontrolerį, todėl `@RestControllerAdvice` jų nepagautų. `ApiAuthErrorHandler` perduoda išimtį tam
pačiam MVC mechanizmui — visos klaidos lieka vienoje vietoje (`ApiExceptionHandler`).

---

## 3. Žetonų gyvavimo ciklas

```
login ──► access (15 min) + refresh R1
          │
          ├─ API užklausos su access
          │
access pasibaigė ──► POST /auth/refresh {R1} ──► naujas access + R2, R1 atšauktas
                                 │
             kažkas siunčia R1 dar kartą ──► 401 + atšaukiami VISI naudotojo refresh žetonai
                                 │
logout {R2} ──► R2 atšauktas (access galioja iki exp)
```

* Refresh žetonas — atsitiktiniai 32 baitai, ne JWT: jo vis tiek reikia ieškoti DB, todėl parašas
  nieko neduotų. DB saugoma tik SHA-256 maiša — nutekėjus DB, žetonų panaudoti nepavyks.
* Pakartotinio panaudojimo aptikimas: jei R1 pavogtas ir panaudotas, tikrasis naudotojas (ar
  vagis) su senu žetonu sukels visų sesijų atšaukimą. `AuthService.refresh()` pažymėtas
  `noRollbackFor`, kad atšaukimas DB išliktų, nors metodas baigiasi `401`.
* Užblokavus naudotoją (`PATCH /api/users/{id}` `enabled=false`) atšaukiami visi jo refresh
  žetonai; prisijungti — `403`.

---

## 4. Nauji ir pakeisti failai

| Failas | Ką sakyti |
| --- | --- |
| `config/SecurityConfig` | Viešų kelių sąrašas, `STATELESS`, CSRF išjungtas, JWT encoder/decoder, BCrypt, `/api/users/**` → `ADMIN` |
| `security/TokenService` | Sukuria JWT (claims), atsitiktinį refresh žetoną ir jo maišą |
| `security/CurrentUser` | Prisijungęs naudotojas iš patikrinto žetono (`sub`, `username`, `role`) |
| `security/ApiAuthErrorHandler` | Filtrų `401`/`403` → ta pati `ApiError` |
| `service/AuthService` | Registracija, prisijungimas (vienoda klaida, dummy BCrypt), rotacija, atsijungimas |
| `service/GuildAccess` | Naudotojo teisės grupėje: `isMember`, `isDm`, `isOwner`, `isAdmin`, `require()` |
| `service/GuildService.access()` | Vienas įėjimo taškas visoms grupės ir jos vaikų užklausoms |
| `service/MembershipService` | Nariai, vienas DM (`409`), savininko pašalinti negalima |
| `service/UserService` | Admin: sąrašas, rolė, blokavimas; savęs užblokuoti negalima |
| `domain/User`, `GuildMembership`, `RefreshToken` | Naujos lentelės `app_user`, `guild_membership`, `refresh_token` |
| `Guild.owner`, `Quest.createdBy`, `LogEntry.author/reviewer` | Kas sukūrė / patvirtino — nuosavybės patikroms |
| `V4`, `V5` migracijos | Lentelės, BCrypt seed naudotojai, LD1 duomenų susiejimas su naudotojais |
| `config/OpenApiConfig` | `bearerAuth` schema ir automatiškai pridedami `401`/`403` atsakymai |

DB apsaugos, kurių neapeisi ir rašydamas tiesiai į DB: vardas unikalus be raidžių dydžio
(`lower(username)` indeksas), narys grupėje vieną kartą (`unique (guild_id, user_id)`), grupėje vienas
DM (dalinis unikalus indeksas `where role = 'DUNGEON_MASTER'`), `PENDING` įrašas be peržiūrėtojo.

---

## 5. Tikėtini klausimai

**Kodėl JWT, o ne sesija?** API be būsenos: bet kuris serverio egzempliorius patikrina žetoną
pats, nereikia bendros sesijų saugyklos. Mobilios ar SPA kliento pusei paprasčiau nei slapukai.

**Kas JWT viduje? Ar jis užšifruotas?** Ne — tik pasirašytas. Turinį (Base64) gali perskaityti
bet kas, todėl ten nėra slaptažodžių ar asmeninių duomenų: tik ID, vardas, rolė, laikai. Pakeisti
negalima — sugestų parašas (Postman „sugadintas žetonas“ rodo `401`).

**HS256 ar RS256?** HS256 — vienas slaptas raktas pasirašo ir tikrina; tinka, kai žetonus išduoda
ir tikrina ta pati programa. RS256 reikėtų, jei žetonus tikrintų kitos tarnybos (jos gautų tik
viešąjį raktą). Raktas `prod` profilyje privalomas per `JWT_SECRET`, bent 32 baitai.

**Kodėl access tik 15 min?** Jo atšaukti negalima — serveris jų nesaugo. Trumpas galiojimas
riboja žalą, jei jis pavogtas. Ilgai sesijai — refresh žetonas, kurį atšaukti galima.

**Kodėl grupės rolės ne žetone?** Naudotojas turi skirtingas roles skirtingose grupėse, o jos gali
keistis (savininkas pakeičia rolę, narys išeina). Imant iš DB, pakeitimas galioja iš karto, o
žetonas nepasensta. Globali `ADMIN` rolė keičiasi retai, todėl ji žetone.

**Kodėl ne narys gauna `403`, o ne `404`?** Taip aiškiau klientui ir demonstracijai. Kaina —
galima sužinoti, kad grupė su tokiu ID egzistuoja. Jei tai svarbu, užtektų `GuildService.access()`
pakeisti vieną eilutę į `404`.

**Kodėl išjungtas CSRF?** CSRF ataka remiasi tuo, kad naršyklė pati prideda slapuką prie
svetimos svetainės užklausos. Čia autentifikacija — `Authorization` antraštė, kurios naršyklė pati
neprideda, ir sesijų nėra (`STATELESS`).

**Kaip apsaugota nuo slaptažodžių spėliojimo / naudotojų išsiaiškinimo?** Ta pati klaida blogam
vardui ir slaptažodžiui; nežinomam vardui vis tiek skaičiuojamas BCrypt, kad atsakymo laikas
nesiskirtų. BCrypt lėtas specialiai — perrinkti brangu.

**Kur klientas (LD3) saugos žetonus?** Access — atmintyje (ne `localStorage`, kad XSS jo
nepavogtų), refresh — idealiai `HttpOnly` slapuke. Dabar refresh grąžinamas JSON, kad būtų
patogu Postman; perkelti į slapuką — `AuthController` pakeitimas.

**Kaip DM negali patvirtinti savo įrašo?** `LogEntry.author` saugo autorių; `LogEntryService.update()`
tikrina `!access.is(entry.getAuthor())`. Postman: „Klaida 403 – savo įrašo tvirtinti negalima“.

**Ką gali ADMIN?** Mato visas grupes ir jų turinį, trina bet ką, valdo naudotojus. Tvirtinti įrašų
ir kurti turinio svetimose grupėse negali — tai grupės DM ir narių darbas.

**Ar API tikrinta su netinkamais duomenimis?** Taip. Automatinis testas kiekvienam metodui siuntė
apie 1700 blogų užklausų: tuščius ir per didelius ID, per ilgus laukus, NUL simbolį, skaičius vietoj
enum, sugadintą JSON, suklastotus žetonus, keistus `sort` ir `page`. `500` liko tik tikroms serverio
klaidoms; kliento klaidos — `400`/`401`/`403`/`404`/`422`. Pavyzdžiai:

* `sort` — tik iš leidžiamų laukų. Kitaip Spring Data priimtų bet kurį getterį: `?sort=class` lūžtų, o
  `?sort=user.passwordHash` leistų iš eilės tvarkos spręsti apie svetimus duomenis.
* `page` — iki 1 000 000, nes Hibernate poslinkį (`page * size`) laiko `int`.
* Slaptažodis — iki 72 **baitų** (BCrypt riba). Lietuviška raidė UTF-8 užima 2 baitus, todėl 72
  simbolių slaptažodis su „ą“ būtų per ilgas.
* Žetone `sub` turi būti skaičius, `role` — `USER` arba `ADMIN`; kitaip `401` jau saugumo filtre.

---

## 6. Silpnos vietos (pasakyti atvirai)

* Access žetonas po atsijungimo ar užblokavimo galioja iki 15 min — kaina už tai, kad serveris jų
  nesaugo. Sprendimas, jei prireiktų: trumpesnis galiojimas arba atšauktų `jti` sąrašas. Naudotojų
  teisių keitimas (`PATCH /api/users`) vis dėlto tikrina kvietėją DB, todėl ką tik atimtas ar
  užblokuotas administratorius per tas 15 min savęs atgal nepaaukštins.
* Nėra prisijungimo bandymų ribojimo (rate limiting), slaptažodžio atkūrimo ir el. pašto.
* Naudotojų ištrinti negalima — tik užblokuoti (jų turinys ir istorija lieka).
* Refresh žetonas grąžinamas JSON, ne `HttpOnly` slapuke (žr. 5 skyrių).

---

## 7. Demonstracijos eiga (~5 min)

Išsamus planas su laukiamais atsakymais — [`DEMO_PLANAS.md`](DEMO_PLANAS.md).

1. Paleisti iš naujo LD2 aplanke (`./mvnw spring-boot:run` arba `demo/1-paleisti-api.cmd`) — švari DB.
2. Swagger: **Authorize** neįvedus — `401`; `POST /api/auth/login` (`rokas` / `Demo123!`) →
   nukopijuoti `accessToken` → **Authorize** → `GET /api/guilds` veikia.
3. JWT turinys: Git Bash (`demo/3-curl-komandos.cmd`) komanda `jwt` (`sub`, `role`,
   `exp - iat = 900`); Postman „Prisijungti“ testai tuos pačius laukus patikrina.
   Žetono į svetimas svetaines (pvz. jwt.io) neklijuoti.
4. Postman *Run collection*: `0. Autentifikacija` (rotacija, pakartotinis panaudojimas),
   `6. Rolės ir teisės` (visi `403` su paaiškinimais), `8. Administravimas`.
5. DB: `SELECT username, left(password_hash, 7) FROM app_user;` — matosi `$2a$10$`, ne slaptažodis.
