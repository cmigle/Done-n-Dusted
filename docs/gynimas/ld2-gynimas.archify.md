# Done n Dusted LD2: autentifikacija ir rolės gynimui {#ld2-gynimas}

LD2 prie LD1 REST API prideda du sluoksnius: **kas tu esi** (JWT žetonai) ir **ką gali daryti** (rolė grupėje ir globali `ADMIN` rolė). Kiekviena užklausa pirma pereina Spring Security žetono patikrą, tada servise tikrinamos teisės grupėje, ir tik po to LD1 dalykinės taisyklės.

- Access žetonas yra pasirašytas JWT, galioja 15 min; serveris jo nesaugo, tik tikrina parašą ir galiojimą.[](source:token)
- Refresh žetonas vienkartinis ir DB saugomas tik kaip maiša; pakartotinis panaudojimas atšaukia visas naudotojo sesijas.[](source:refresh)
- Rolė grupėje (`PLAYER` arba `DUNGEON_MASTER`) imama iš DB kiekvienai užklausai, todėl pakeitimas galioja iš karto.[](source:access)
- Postman kolekcija: 78 užklausos ir 176 patikros, 0 klaidų švarioje DB.[](source:postman)

@answer scope="LD2 · Spring Boot 4.1.1 · Spring Security 7 · dev profilis · main fe41324"
@meta locale=en style=editorial
@source token title="TokenService.createAccessToken – JWT kūrimas" path=src/main/java/lt/ktu/donendusted/security/TokenService.java lines=36:49
@source sec title="SecurityConfig – filtrų grandinė" path=src/main/java/lt/ktu/donendusted/config/SecurityConfig.java lines=39:58
@source refresh title="AuthService.refresh – rotacija ir vagystės aptikimas" path=src/main/java/lt/ktu/donendusted/service/AuthService.java lines=69:87
@source access title="GuildService.access – įėjimas į grupę" path=src/main/java/lt/ktu/donendusted/service/GuildService.java lines=69:81
@source guildaccess title="GuildAccess – teisės grupėje" path=src/main/java/lt/ktu/donendusted/service/GuildAccess.java lines=15:44
@source logupdate title="LogEntryService.update – kas gali keisti įrašą" path=src/main/java/lt/ktu/donendusted/service/LogEntryService.java lines=91:109
@source autherr title="ApiAuthErrorHandler – 401/403 iš filtrų" path=src/main/java/lt/ktu/donendusted/security/ApiAuthErrorHandler.java lines=13:36
@source authex title="ApiExceptionHandler – 401 pranešimai" path=src/main/java/lt/ktu/donendusted/error/ApiExceptionHandler.java lines=55:65
@source v5 title="V5 migracija – narystės ir vienas DM" path=src/main/resources/db/migration/V5__guild_membership.sql lines=4:21
@source v4 title="V4 migracija – naudotojai ir refresh žetonai" path=src/main/resources/db/migration/V4__users_and_tokens.sql lines=1:36
@source postman title="Postman kolekcija (v2.1 JSON)" path=postman/DoneNDusted.postman_collection.json
@source readme title="README – metodai ir rolės" path=README.md
@source rfc7519 title="RFC 7519: JSON Web Token (JWT)" url=https://www.rfc-editor.org/rfc/rfc7519
@source rfc6750 title="RFC 6750: Bearer Token Usage" url=https://www.rfc-editor.org/rfc/rfc6750
@source rfc9700 title="RFC 9700: OAuth 2.0 Security Best Current Practice" url=https://www.rfc-editor.org/rfc/rfc9700
@source owasp title="OWASP Password Storage Cheat Sheet" url=https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html

## Kas pasikeitė nuo LD1? {#changes}

LD1 API buvo atviras: bet kas galėjo kurti, keisti ir trinti bet ką. LD2 nekeičia LD1 metodų kelių ir atsakymų formos. Jis prideda patikrą prieš kiekvieną veiksmą ir kelis naujus resursus.

| Sritis | LD1 | LD2 |
| --- | --- | --- |
| Kas gali kviesti API | Bet kas {bad:Atvira} | Tik su galiojančiu žetonu {good:JWT} |
| Naudotojai | Nėra | `app_user`, slaptažodis BCrypt maiša |
| Rolės | Nėra | `PLAYER` / `DUNGEON_MASTER` grupėje, `ADMIN` globaliai |
| Įrašo tvirtinimas | Bet kas | Tik grupės DM ir ne savo įrašo {good:LD0 taisyklė} |
| Įrašo autorius | Nežinomas | `author_id`, `reviewer_id` |
| Metodų skaičius | 15 | 28 (+5 auth, +5 nariai, +3 naudotojai) |
| Klaidų kodai | 400, 404, 405, 406, 409, 415, 422 | Papildomai 401 ir 403 |
| Postman | 31 užklausa, 75 patikros | 78 užklausos, 176 patikros |
| Versija | `v0.1.0` | `v0.2.0` |

LD1 dalis (REST, puslapiavimas, klaidų struktūra) aprašyta `docs/visual/GYNIMO_GIDAS.md`. Šis puslapis aiškina tai, kas nauja.

## Iš kokių dalių sudarytas LD2? {#layers}

@architecture id=ld2-layers title="LD2 sluoksniai"
| client | Postman / Swagger | frontend |
| filters | Security filtrai | security |
| decoder | JwtDecoder | security |
| controllers | Kontroleriai | backend |
| services | Servisai + GuildAccess | backend |
| tokens | TokenService | security |
| errors | ApiExceptionHandler | backend |
| db | PostgreSQL | database |
| client > filters | Bearer žetonas |
| filters > decoder | parašas, exp |
| filters > controllers | autentifikuota | emphasis |
| controllers > services | |
| services > tokens | login, refresh |
| services > db | rolė grupėje |
| filters > errors | 401, 403 | dashed |
| services > errors | 403, 404, 422 | dashed |

@steps id=layers-steps
| client | Postman / Swagger | Klientas siunčia antraštę `Authorization: Bearer <accessToken>`. Be jos pasiekiami tik `/api/auth/register`, `/login`, `/refresh`, `/logout` ir Swagger. | ld2-layers | client |
| filters | Security filtrai | Spring Security filtrų grandinė veikia dar prieš kontrolerį. Ji paima žetoną, o `/api/users/**` iš karto reikalauja `ADMIN` rolės. | ld2-layers | filters |
| decoder | JwtDecoder | Tikrina HS256 parašą, galiojimo laiką `exp` ir išdavėją `iss`. Netinka – `401`, kontroleris net nekviečiamas. | ld2-layers | decoder |
| controllers | Kontroleriai | Tokie patys kaip LD1: priima užklausą, `@Valid` patikrina laukus ir kviečia servisą. | ld2-layers | controllers |
| services | Servisai + GuildAccess | Kiekvienas grupės veiksmas prasideda `GuildService.access()`: randa grupę ir naudotojo rolę joje. Toliau `GuildAccess.require()` tikrina konkrečią teisę. | ld2-layers | services |
| tokens | TokenService | Sukuria JWT prisijungiant ir atnaujinant, sugeneruoja atsitiktinį refresh žetoną ir jo SHA-256 maišą. | ld2-layers | tokens |
| errors | ApiExceptionHandler | Visos klaidos, ir iš filtrų, ir iš servisų, virsta ta pačia `ApiError` struktūra. | ld2-layers | errors |
| db | PostgreSQL | Naujos lentelės `app_user`, `guild_membership`, `refresh_token`; seniems duomenims pridėti autoriai ir savininkai. | ld2-layers | db |

Svarbiausia mintis: **autentifikacija** (kas tu) vyksta filtruose, o **autorizacija** (ką gali) – servisuose. Filtrai nežino, kas yra grupė ar įrašo autorius, todėl smulkios teisės tikrinamos ten, kur tie duomenys jau įkrauti.

## Kaip serveris parenka atsakymo kodą? {#decision}

@workflow id=status-decision title="Nuo užklausos iki atsakymo kodo"
| hasToken | Yra žetonas? | security |
| validToken | Žetonas galioja? | security |
| adminPath | Reikia ADMIN? | security |
| bodyOk | Laukai teisingi? | backend |
| guildExists | Grupė yra? | backend |
| isMember | Narys ar ADMIN? | backend |
| roleOk | Rolė leidžia? | backend |
| ruleOk | Taisyklė tenkinama? | backend |
| ok | 200 / 201 / 204 | backend |
| noToken | 401 be žetono | external |
| badToken | 401 netinka | external |
| notAdmin | 403 ne ADMIN | external |
| badBody | 400 laukai | external |
| noGuild | 404 nėra | external |
| notMember | 403 ne narys | external |
| noRight | 403 rolė | external |
| badRule | 422 taisyklė | external |
| hasToken > validToken | taip |
| hasToken > noToken | ne |
| validToken > adminPath | taip |
| validToken > badToken | ne |
| adminPath > notAdmin | ne ADMIN |
| adminPath > bodyOk | gerai |
| bodyOk > badBody | ne |
| bodyOk > guildExists | taip |
| guildExists > noGuild | ne |
| guildExists > isMember | taip |
| isMember > notMember | ne |
| isMember > roleOk | taip |
| roleOk > noRight | ne |
| roleOk > ruleOk | taip |
| ruleOk > badRule | ne |
| ruleOk > ok | taip | emphasis |

@steps id=decision-steps
| s1 | Yra žetonas? | Nėra `Authorization` antraštės – `401` su `WWW-Authenticate: Bearer` ir pranešimu „Reikalingas prisijungimas“. | status-decision | hasToken |
| s2 | Žetonas galioja? | Sugadintas parašas arba praėjęs `exp` – `401` su `invalid_token`. Pasibaigusiam žetonui pranešimas siūlo `/api/auth/refresh`. | status-decision | validToken |
| s3 | Reikia ADMIN? | Tik `/api/users/**`. Ne administratoriui – `403` „Neturite teises“, dar prieš kontrolerį. | status-decision | adminPath |
| s4 | Laukai teisingi? | Kaip LD1: blogas JSON, tipas ar `@Valid` apribojimas – `400` su `details`. | status-decision | bodyOk |
| s5 | Grupė yra? | `GuildService.access()` pirmiausia ieško grupės: nėra – `404`. | status-decision | guildExists |
| s6 | Narys ar ADMIN? | Grupė yra, bet naudotojas ne narys ir ne `ADMIN` – `403` „Nesate grupes N narys“. | status-decision | isMember |
| s7 | Rolė leidžia? | Konkreti teisė: DM tvirtina, savininkas keičia grupę, autorius keičia savo įrašą. Ne – `403` su paaiškinimu. | status-decision | roleOk |
| s8 | Taisyklė tenkinama? | LD1 dalykinės taisyklės ir naujos (patvirtinto įrašo turinys užšaldytas) – `422`. | status-decision | ruleOk |
| s9 | 200 / 201 / 204 | Viskas tinka: pakeitimas įrašomas transakcijoje. | status-decision | ok |

Tvarka svarbi: pirma tikrinama, **kas** kreipiasi, tada **ar gali**, ir tik tada **ar veiksmas atitinka taisykles**. Laukų formatą (`400`) kontroleris tikrina dar prieš teises, o dalykines taisykles (`422`) – tik po jų. Todėl ne narys niekada nesužino, ar jo veiksmas pažeistų dalykinę taisyklę.

## Kaip gyvena žetonai? {#zetonu-ciklas}

@sequence id=token-life title="Žetonų gyvavimo ciklas"
| actor client | Klientas | frontend |
| actor auth | AuthService | security |
| actor api | Apsaugotas API | backend |
| actor db | PostgreSQL | database |
| phase | Prisijungimas |
| client | auth | POST /auth/login (vardas, slaptažodis) |
| auth | db | BCrypt palyginimas, R1 maiša įrašoma |
| auth | client | access (15 min) + R1 | return |
| phase | Darbas su API |
| client | api | Authorization: Bearer access |
| api | client | 200 OK | return |
| phase | Access pasibaigė |
| client | auth | POST /auth/refresh (R1) |
| auth | db | R1 atšaukiamas, R2 įrašomas |
| auth | client | naujas access + R2 | return |
| phase | Kažkas panaudoja seną R1 |
| client | auth | POST /auth/refresh (R1) | security |
| auth | db | atšaukiami VISI naudotojo žetonai | security |
| auth | client | 401 – prisijunkite iš naujo | return |
| phase | Atsijungimas |
| client | auth | POST /auth/logout (refresh) |
| auth | db | refresh atšaukiamas |
| auth | client | 204 No Content | return |

@steps id=token-steps
| t1 | Prisijungimas | `POST /api/auth/login` palygina slaptažodį su BCrypt maiša. Grąžina access žetoną (JWT) ir refresh žetoną R1; DB įrašoma tik R1 SHA-256 maiša. | token-life | auth |
| t2 | Darbas su API | Kiekviena užklausa siunčia access žetoną. Serveris jo DB neieško – tik patikrina parašą, todėl tai greita. | token-life | api |
| t3 | Access pasibaigė | Po 15 min klientas siunčia R1 į `/api/auth/refresh`. R1 atšaukiamas, išduodama nauja pora su R2 – tai **rotacija**. | token-life | auth |
| t4 | Kažkas panaudoja seną R1 | Atšauktas žetonas atkeliavo dar kartą – vadinasi, kažkas jį nukopijavo. Serveris atšaukia visas to naudotojo sesijas ir grąžina `401`. | token-life | db |
| t5 | Atsijungimas | `POST /api/auth/logout` atšaukia refresh žetoną. Access žetonas galioja iki savo `exp`, todėl jis ir trumpas. | token-life | client |

Rotacija ir pakartotinio panaudojimo aptikimas – rekomenduojama OAuth 2.0 saugumo praktika.[](source:rfc9700) Postman aplanke `0. Autentifikacija` šis scenarijus parodytas užklausomis „Atnaujinti žetonus“ ir „Klaida 401 – pakartotinai panaudotas refresh žetonas“.

## Kas yra JWT viduje? {#jwt}

JWT – trys Base64url dalys, atskirtos tašku: antraštė, turinys (claims) ir parašas.[](source:rfc7519) Serveris pasirašo HS256 raktu iš `app.jwt.secret`; `prod` profilyje raktas privalomas per `JWT_SECRET`.

@facts
| `sub` | Naudotojo ID, pvz. `"2"` |
| `username` | Naudotojo vardas, pvz. `rokas` |
| `role` | Globali rolė: `USER` arba `ADMIN` |
| `iss` | Išdavėjas `done-n-dusted`; kitas iss – `401` |
| `iat` / `exp` | Išdavimo ir galiojimo pabaigos laikas; skirtumas 900 s |

@code source=token language=java

@callout level=warning title="JWT nėra užšifruotas"
| Turinį gali perskaityti bet kas, kas turi žetoną – užtenka Base64 dekodavimo. |
| Todėl žetone nėra slaptažodžių ir asmeninių duomenų, tik ID, vardas ir rolė. |
| Pakeisti turinio negalima: sugestų parašas, ir serveris grąžintų `401`. |

Grupių rolių žetone **nėra** sąmoningai. Naudotojas skirtingose grupėse turi skirtingas roles, o jos gali pasikeisti bet kada. Jei būtų žetone, pakeitimas įsigaliotų tik po 15 min.

## Kodėl refresh žetonas saugomas DB? {#refresh}

| Savybė | Access žetonas | Refresh žetonas |
| --- | --- | --- |
| Forma | JWT su parašu | Atsitiktiniai 32 baitai (Base64url) |
| Galioja | 15 min | 7 d. |
| Kur saugomas serveryje | Niekur {good:Greita} | `refresh_token`, tik SHA-256 maiša |
| Ar galima atšaukti | Ne {caution:Tik laukti exp} | Taip {good:Logout, blokavimas} |
| Kada siunčiamas | Kiekvienoje užklausoje | Tik į `/api/auth/refresh` ir `/logout` |

Refresh žetonas nėra JWT, nes jo vis tiek reikia ieškoti DB: reikia žinoti, ar jis neatšauktas. Parašas čia nieko neduotų. DB laikoma tik maiša, todėl nutekėjus duomenų bazei žetonų panaudoti nepavyks.

@code source=refresh language=java

`@Transactional(noRollbackFor = ...)` čia būtinas. Metodas baigiasi `401` išimtimi, o be šios anotacijos Spring atšauktų transakciją – kartu ir visų sesijų atšaukimą DB.

## Kas ką gali daryti? {#roles}

| Kas | Grupė | Užduotys | Įrašai | Nariai | Naudotojai |
| --- | --- | --- | --- | --- | --- |
| Svečias (be žetono) | `401` | `401` | `401` | `401` | Tik registracija ir prisijungimas |
| `USER` ne narys | Kurti naują; svetimą – `403` | `403` | `403` | `403` | `403` |
| `PLAYER` | Matyti | Matyti, kurti (ne `SIDE_QUEST`), keisti savo | Registruoti, matyti ir keisti tik savo | Matyti, išeiti pačiam | `403` |
| `DUNGEON_MASTER` | Matyti | Viską, ir `SIDE_QUEST` | Matyti visus, tvirtinti ne savo {good:Vienintelis tvirtina} | Matyti | `403` |
| Savininkas | Keisti, trinti | Keisti, trinti | – | Pridėti, šalinti, keisti roles | `403` |
| `ADMIN` | Matyti visas, trinti | Matyti, trinti | Matyti visus, trinti | Valdyti | Sąrašas, rolė, blokavimas {good:Tik jis} |

Lentelė rodo, kas leidžiama; visa kita grąžina `403` su paaiškinimu, ko trūksta. Grupės kūrėjas automatiškai tampa jos savininku ir DM. Grupėje gali būti tik vienas DM: antras bandymas grąžina `409`.

Naudotojas skirtingose grupėse gali turėti skirtingas roles. Seed duomenyse `rokas` yra DM grupėse *Thursday Night Crew* ir *Iron Vow*, bet žaidėjas grupėje *The Quiet Library*.

`ADMIN` sąmoningai **netvirtina įrašų** ir nekuria turinio svetimose grupėse. LD0 ataskaitoje jo darbas – valdyti naudotojus ir šalinti netinkamą turinį.

## Kaip teisės tikrinamos kode? {#access-code}

Visi grupės, užduočių, įrašų ir narių metodai prasideda vienu kvietimu `guilds.access(guildId)`. Jis grąžina `GuildAccess` – grupę, prisijungusį naudotoją ir jo rolę toje grupėje.

@code source=access language=java

Rolė imama scalar užklausa `findRole()`, o ne visu `GuildMembership` objektu. Taip pataisyta klaida: kai narystė būdavo įkrauta į Hibernate sesiją, grupės trynimas grąžindavo `500`.

`GuildAccess` turi trumpus klausimus `isMember()`, `isDm()`, `isOwner()`, `isAdmin()`, `is(user)` ir vieną veiksmą `require()`. Netenkinama sąlyga meta `ForbiddenException`, kuri virsta `403`.

@code source=logupdate language=java

Šis fragmentas įgyvendina dvi LD0 taisykles. Turinį keičia tik autorius ir tik kol įrašas `PENDING`. Peržiūros laukus keičia tik DM, ir niekada ne savo įrašo.

Kodėl ne `@PreAuthorize`: teisės priklauso nuo duomenų (kas autorius, kas DM), o jie įkraunami servise. Viena Java eilutė su lietuvišku paaiškinimu lengviau skaitoma nei SpEL išraiška anotacijoje.

## Kokios lentelės atsirado? {#schema}

@diagram erd id=ld2-schema source=ld2-schema.erd.json title="LD2 duomenų bazė"

Diagramoje – visi šešių lentelių stulpeliai. Juostos rodo sritis: **naudotojai**, **grupės** ir **turinys**. Brūkšninės linijos – neprivalomi ryšiai: `reviewer_id` tuščias, kol įrašas neperžiūrėtas, o `created_by` tampa `null`, jei naudotojas pašalinamas DB lygiu.

DB apsaugos, kurių neapeisi net rašydamas tiesiai į bazę:[](source:v5)

- Vardas unikalus be raidžių dydžio: indeksas ant `lower(username)`, todėl „Jonas“ ir „jonas“ – tas pats.
- Narys grupėje tik kartą: `unique (guild_id, user_id)`.
- Grupėje daugiausia vienas DM: dalinis unikalus indeksas `where role = 'DUNGEON_MASTER'`.
- `PENDING` įrašas neturi peržiūrėtojo: `check (status <> 'PENDING' or reviewer_id is null)`.
- Slaptažodis tik BCrypt maiša `$2a$10$...` – lėta funkcija su atsitiktine druska.[](source:owasp)

Migracija `V5` ne tik sukūrė lenteles, bet ir susiejo LD1 seed duomenis su naudotojais. Tik tada `owner_id` ir `author_id` tapo `NOT NULL`.

## Kaip 401 ir 403 tampa ApiError? {#klaidos-401-403}

Filtrai suveikia anksčiau nei kontroleris, todėl `@RestControllerAdvice` jų klaidų nemato. `ApiAuthErrorHandler` perduoda tokią klaidą tam pačiam Spring MVC mechanizmui, ir ją sutvarko `ApiExceptionHandler`.

@code source=autherr language=java

| Situacija | Kodas | Pranešimas |
| --- | --- | --- |
| Nėra antraštės `Authorization` | `401` | Reikalingas prisijungimas: Authorization: Bearer `<accessToken>` |
| Sugadintas žetonas | `401` | Access zetonas neteisingas |
| Pasibaigęs žetonas | `401` | Access zetonas pasibaiges – gaukite nauja per /api/auth/refresh |
| Blogas vardas ar slaptažodis | `401` | Neteisingas naudotojo vardas arba slaptazodis |
| Užblokuota paskyra | `403` | Paskyra uzblokuota |
| Ne grupės narys | `403` | Nesate grupes N narys |
| Žaidėjas tvirtina įrašą | `403` | Irasus perziuri tik grupes DM |
| DM tvirtina savo įrašą | `403` | Savo iraso tvirtinti negalima |
| Ne ADMIN kviečia `/api/users` | `403` | Neturite teises atlikti sio veiksmo |

Prie kiekvieno `401` serveris prideda antraštę `WWW-Authenticate: Bearer`, kaip reikalauja Bearer žetonų standartas.[](source:rfc6750) **401** reiškia „nežinau, kas tu“, **403** – „žinau, bet negalima“.

## Kokie nauji API metodai? {#endpoints}

| # | Metodas | Kelias | Kas gali |
| --- | --- | --- | --- |
| 16 | `POST` | `/api/auth/register` | Svečias; rolė visada `USER` |
| 17 | `POST` | `/api/auth/login` | Svečias |
| 18 | `POST` | `/api/auth/refresh` | Svečias su refresh žetonu |
| 19 | `POST` | `/api/auth/logout` | Svečias su refresh žetonu |
| 20 | `GET` | `/api/auth/me` | Prisijungęs |
| 21 | `GET` | `/api/guilds/{guildId}/members` | Narys, `ADMIN` |
| 22 | `GET` | `/api/guilds/{guildId}/members/{userId}` | Narys, `ADMIN` |
| 23 | `POST` | `/api/guilds/{guildId}/members` | Savininkas, `ADMIN` |
| 24 | `PATCH` | `/api/guilds/{guildId}/members/{userId}` | Savininkas, `ADMIN` |
| 25 | `DELETE` | `/api/guilds/{guildId}/members/{userId}` | Savininkas, `ADMIN`, pats narys |
| 26 | `GET` | `/api/users` | `ADMIN` |
| 27 | `GET` | `/api/users/{userId}` | `ADMIN` |
| 28 | `PATCH` | `/api/users/{userId}` | `ADMIN` |

Narys identifikuojamas naudotojo ID grupės viduje, o pridedant – vardu (`{"username":"jonas","role":"PLAYER"}`). Nežinomas vardas grąžina `422`, nes klaida yra užklausos turinyje, o ne kelyje. Metodai #1–#15 liko LD1, tik su teisių patikromis.[](source:readme)

Swagger UI (`/swagger-ui.html`) turi mygtuką **Authorize**: įklijavus access žetoną, visos užklausos siunčiamos su juo.

## Kas yra seed duomenyse? {#seed}

| ID | Vardas | Globali rolė | Grupėse |
| --- | --- | --- | --- |
| 1 | `admin` | `ADMIN` | Niekur, bet mato visas |
| 2 | `rokas` | `USER` | DM ir savininkas: *Thursday Night Crew*, *Iron Vow*; žaidėjas: *The Quiet Library* |
| 3 | `ieva` | `USER` | DM ir savininkė: *The Quiet Library*; žaidėja kitose dviejose |
| 4 | `jonas` | `USER` | Žaidėjas: *Thursday Night Crew*, *Iron Vow*; ne *Library* narys |

Visų slaptažodis `Demo123!`.[](source:v4) Visus LD1 įrašus peržiūrėjo tos grupės DM, ir niekas netvirtino savo įrašo. `jonas` neturi prieigos prie *The Quiet Library*, todėl jis tinka `403` demonstracijai.

## Ką tikrina Postman kolekcija? {#postman}

| Aplankas | Ką parodo |
| --- | --- |
| 0. Autentifikacija | Registracija, `409`, `400`, prisijungimas, JWT turinys, `/auth/me`, `401`, rotacija, vagystės aptikimas, atsijungimas |
| 1. Suvestinė | `admin` vardu: `Is viso - grupiu: 3, uzduociu: 9, irasu: 12` |
| 2. Grupės | LD1 CRUD, kūrėjas tampa savininku, naudotojas mato tik savo grupes, puslapiavimas |
| 3. Nariai | Žaidėjo pridėjimas, `409` antras DM, `422` nežinomas naudotojas, `403` žaidėjui |
| 4. Užduotys | LD1 testai ir `createdBy` |
| 5. Progreso įrašai | Žaidėjas registruoja, DM tvirtina, patvirtinto įrašo turinys užšaldytas (`422`) |
| 6. Rolės ir teisės | Visi `403` atvejai su paaiškinimais, žaidėjas mato tik savo įrašus, išėjimas iš grupės |
| 7. Išvalymas | Ištrynimas, antrą grupę trina `admin`, `404` po ištrynimo |
| 8. Administravimas | Naudotojų sąrašas, `422` bandant užblokuoti save, blokavimas ir atblokavimas |

Kolekcijos lygyje nustatytas *Bearer Token* `{{accessToken}}`. Užklausos, kurias siunčia žaidėjas ar administratorius, turi savo `{{playerToken}}` arba `{{adminToken}}`. Kiekvienas paleidimas užregistruoja naują naudotoją `postman_<laikas>`, todėl kolekciją galima leisti kartotinai.

@facts
| Užklausų | 78 |
| Patikrų | 176, 0 klaidų |
| Trukmė | apie 10 s |
| Paleidimas | `npx newman run postman/DoneNDusted.postman_collection.json` |

## Kaip atrodo Git istorija? {#git}

LD2 sudėtas taip pat kaip LD1: kiekviena funkcija – atskira šaka, sujungta su `main` per `Merge branch '...'`. Visi commit'ai trumpi, angliški.

| Šaka | Commit'ai | Kas pridėta |
| --- | --- | --- |
| `feat/users-and-tokens` | Add users and refresh tokens | `V4` migracija, `User`, `RefreshToken` |
| `feat/jwt-auth` | Add JWT auth; Return 401 and 403 as ApiError | Security, `TokenService`, `/api/auth/*` |
| `feat/guild-roles` | Add guild membership, owners and authors; Enforce guild roles | `V5`, `GuildAccess`, teisės servisuose |
| `feat/guild-members` | Add guild member endpoints; Show memberships in /api/auth/me | `/members`, `MeResponse` |
| `feat/admin-users` | Add admin user management | `/api/users` |
| `feat/openapi-auth` | Document bearer auth in OpenAPI | `bearerAuth`, `401`/`403` dokumentacija |
| `fix/guild-delete` | Fix guild delete | `findRole()` vietoj entity |
| `feat/postman-ld2` | Postman: LD2 collection | 9 aplankai, 78 užklausos |
| `docs/ld2` | Update demo commands; README; LD2 notes | Dokumentacija |

Žymos: `v0.1.0` – LD1 būsena, `v0.2.0` – LD2. Pasirodžius klausimui „parodyk LD1 kodą“, užtenka `git checkout v0.1.0`.

## Tikėtini gynimo klausimai {#qa}

| Klausimas | Atsakymas |
| --- | --- |
| Kodėl JWT, o ne sesija? | API neturi būsenos: žetoną patikrina bet kuris serverio egzempliorius, bendros sesijų saugyklos nereikia. SPA ir mobiliam klientui tai paprasčiau nei slapukai. |
| Ar JWT užšifruotas? | Ne, tik pasirašytas. Turinį gali perskaityti kiekvienas, bet pakeisti negali – sugestų parašas. |
| HS256 ar RS256? | HS256: tas pats slaptas raktas pasirašo ir tikrina. Tinka, nes žetonus išduoda ir tikrina ta pati programa. RS256 reikėtų, jei tikrintų kitos tarnybos. |
| Kodėl access tik 15 min? | Jo atšaukti negalima, nes serveris jų nesaugo. Trumpas galiojimas riboja žalą, jei žetonas pavogtas. |
| Kodėl refresh ne JWT? | Jį vis tiek reikia ieškoti DB, ar neatšauktas. DB laikoma tik SHA-256 maiša. |
| Kas yra rotacija? | Kiekvienas refresh žetonas vienkartinis. Panaudotas pakeičiamas nauju, o pakartotinis panaudojimas reiškia vagystę – atšaukiamos visos sesijos. |
| Kodėl grupės rolės ne žetone? | Skirtingose grupėse rolės skiriasi ir gali keistis. Imant iš DB, pakeitimas galioja iš karto. |
| Kodėl ne narys gauna 403, o ne 404? | Aiškiau klientui ir demonstracijai. Kaina – galima sužinoti, kad grupė egzistuoja; pakeisti į `404` – viena eilutė `GuildService.access()`. |
| Kodėl išjungtas CSRF? | CSRF remiasi slapuku, kurį naršyklė prideda pati. Čia žetonas siunčiamas antraštėje, kurios naršyklė neprideda, ir sesijų nėra. |
| Kaip apsaugota nuo vardų spėliojimo? | Ta pati klaida blogam vardui ir slaptažodžiui. Nežinomam vardui vis tiek skaičiuojamas BCrypt, kad atsakymo laikas nesiskirtų. |
| Kodėl BCrypt? | Lėta maišos funkcija su druska: vienodi slaptažodžiai duoda skirtingas maišas, o perrinkti brangu. |
| Kur LD3 klientas laikys žetonus? | Access – atmintyje, ne `localStorage`, kad XSS jo nepavogtų. Refresh – idealiai `HttpOnly` slapuke. |
| Kodėl teisės servise, ne `@PreAuthorize`? | Teisės priklauso nuo duomenų (autorius, DM), kurie įkraunami servise. Java eilutė su paaiškinimu aiškesnė nei SpEL. |
| Kas nutinka užblokavus naudotoją? | Atšaukiami visi jo refresh žetonai, prisijungti – `403`. Turimas access galioja iki 15 min. |
| Kodėl `noRollbackFor`? | `refresh()` baigiasi `401`, o be jo transakcija atšauktų ir visų sesijų atšaukimą. |
| Kaip DM negali patvirtinti savo įrašo? | `log_entry.author_id` saugo autorių; `LogEntryService.update()` tikrina `!access.is(entry.getAuthor())`. |
| Ką gali ADMIN, o ko ne? | Mato viską, trina bet ką, valdo naudotojus. Netvirtina įrašų ir nekuria turinio svetimose grupėse. |

## Kokios yra silpnos vietos? {#weak}

@callout level=warning title="Pasakyk atvirai, jei paklaus"
| Access žetonas po atsijungimo ar užblokavimo galioja iki 15 min. Tai kaina už tai, kad serveris jų nesaugo. |
| Nėra prisijungimo bandymų ribojimo, slaptažodžio atkūrimo ir el. pašto patvirtinimo. |
| Naudotojo ištrinti negalima, tik užblokuoti: jo turinys ir istorija lieka. |
| Refresh žetonas grąžinamas JSON, ne `HttpOnly` slapuke, kad būtų patogu Postman. |
| `Friendship` ir veikėjo kortelė iš LD0 dar neįgyvendinti – jie liko kitam darbui. |

Kiekvienai silpnai vietai turėk paruoštą sprendimą: trumpesnis galiojimas arba atšauktų `jti` sąrašas, rate limiting filtras, refresh žetonas slapuke. Dėstytojui svarbiausia, kad žinai kompromisą ir kodėl jį pasirinkai.

## Ką pasitikrinti prieš gynimą? {#checklist}

- [ ] Paleisk iš naujo `demo/1-paleisti-api.cmd` (arba `./mvnw spring-boot:run`), kad DB būtų švari ir seed ID teisingi. Visa eiga su laukiamais atsakymais – `docs/DEMO_PLANAS.md`.
- [ ] Paleisk Postman kolekciją (`demo/2-postman-testai.cmd`): turi būti 176 patikros be klaidų ir suvestinė `grupiu: 3, uzduociu: 9, irasu: 12`.
- [ ] Swagger: parodyk `401` be žetono, prisijunk kaip `rokas`, įklijuok žetoną per **Authorize**.
- [ ] JWT turinys: `demo/3-curl-komandos.cmd` → komanda `jwt` parodo `sub`, `role`, `iat`, `exp` (`exp - iat = 900`); Postman „Prisijungti“ testai tai patikrina.
- [ ] Parodyk rotaciją: „Atnaujinti žetonus“, tada „Klaida 401 – pakartotinai panaudotas refresh žetonas“.
- [ ] Aplanke `6. Rolės ir teisės` parodyk „Klaida 403 – savo įrašo tvirtinti negalima“.
- [ ] DB (`demo/4-duomenu-baze.cmd`): `password_hash` prasideda `$2a$10$` – maiša, ne slaptažodis; refresh žetonų – tik SHA-256 maiša.
- [ ] Mokėk paaiškinti skirtumą tarp `401` ir `403` bei tarp autentifikacijos ir autorizacijos.
- [ ] GitHub: parodyk šakas ir žymas `v0.1.0`, `v0.2.0`.
