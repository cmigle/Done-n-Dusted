# LD3 — naudotojo sąsaja ir diegimas

React klientas LD1–LD2 REST API, pikselinio RPG stiliaus. Kodas — [`frontend/`](../frontend/),
API aprašas — [`API.md`](API.md). Šaka `ld3`, žyma `v0.3.0`.

![Veikėjo kortelė](screenshots/05-sheet-desktop.png)

---

## 1. Reikalavimai ir jų įgyvendinimas

| Reikalavimas | Kaip įgyvendinta | Kur žiūrėti |
| --- | --- | --- |
| Sąsajos projektas (wireframe'ai) | 7 langų wireframe'ai: pradžia, taverna, grupė, užduotis, kortelė, DM modalinis langas, telefonas | [2 skyrius](#2-wireframeai), [`wireframes/`](wireframes/) |
| Sąsaja REST API | 31 iš 35 API metodų: grupės, užduotys, įrašai, nariai, kortelė, draugai, naudotojai, autentifikacija. Nenaudojami tik 4 pavieniai `GET` pagal ID (įrašas, narys, naudotojas, draugystė) — tie duomenys jau ateina sąraše | `frontend/src/api/endpoints.ts` |
| Responsive, bent 1 breakpoint | Pagrindinis lūžis **768 px** (meniu, tinklelis, formos, sąrašai); papildomai 1024 px (3 stulpelių kortelės) | `styles/layout.css`, `styles/pages.css` |
| Paveikslėlių prisitaikymas | `img, svg { max-width: 100%; height: auto }`; iliustracija ir avatarai mažėja kartu su konteineriu | `styles/base.css`, [ekrano nuotraukos](#3-ekrano-nuotraukos) |
| Header, Content, Footer — skirtingi stiliai | Header — akmenų siena, pikselinis logotipas, rėmeliuoti skirtukai; Content — `shadow` paviršiai su `slate` rėmeliu ant žvaigždėto fono; Footer — tamsiausia juosta su žolės skirtuku viršuje, taškiniai pabraukimai | `styles/layout.css`, `styles/components.css` |
| Analogiški elementai skirtingose srityse atrodo skirtingai | Nuorodos: header'yje — skirtukai su rėmeliu, turinyje — mėlynos pabrauktos, footer'yje — baltos su taškiniu žaliu pabraukimu. Antraštės: header'yje — logotipas, turinyje — su šešėliu, footer'yje — žalios mažos | tie patys |
| Įvedimo sąsaja, keli input tipai | `text`, `password`, `textarea`, `select`, `radio`, `checkbox`, `number`, `url`, `datetime-local`, `range`, `search` | `QuestFormModal`, `ReviewModal`, `QuestPage` (žygio forma), `AuthPages`, `AdminUsersPage` |
| Transitions / animacijos | XP ir charakteristikų juostos užsipildo pikseliniais žingsniais; modalinis langas „iššoka“; pranešimai įslysta; mygtukas „nusispaudžia“; meniu išsiskleidžia; laukų rėmelio spalvos perėjimas. `prefers-reduced-motion` — animacijos išjungiamos | `styles/components.css` |
| Responsive meniu (desktop — horizontalus, mobile — hamburger) | ≥ 768 px — horizontalūs skirtukai; < 768 px — hamburger mygtukas su `aria-expanded`, meniu išsiskleidžia po header'iu ir užsidaro pasirinkus punktą | `components/Layout.tsx`, `styles/layout.css` |
| Vektorinės ikonos | [pixelarticons](https://pixelarticons.com) (SVG, MIT) per `@nsmr/pixelart-react`; logotipas, iliustracija ir avatarai — taip pat SVG | `components/StatIcon.tsx`, `public/*.svg` |
| Kitoks šriftas | Google Fonts: **Pixelify Sans** — logotipui, H1 ir H2; **IBM Plex Mono** — tekstui, mygtukams, lentelėms ir skaičiams (`tabular-nums`). Pikselinio šrifto skaitmenys painiojosi (20 ↔ 80, 5 ↔ S), todėl jo skaičiams nenaudojam | `index.html`, `styles/tokens.css` |
| Modalinis langas su prasminga informacija | „Kaip skaičiuojama patirtis?“ (XP lentelė ir formulės), DM įrašo peržiūra, užduoties ir grupės formos, draugo kortelė, trynimo patvirtinimas. Natūralus `<dialog>`: fokusas lange, `Esc` ir paspaudimas šalia uždaro | `components/ui.tsx` (`Modal`), `XpRulesModal.tsx`, `ReviewModal.tsx` |
| Derančios spalvos | „Sweetie-16“ pikselių paletė (16 spalvų, sukurta derėti tarpusavyje); charakteristikos turi savo spalvas | `styles/tokens.css` |
| Išdėstymas pagal tinklelį | 12 kolonų logika per CSS Grid (`grid--2`, `grid--3`, `grid--sidebar` 4/8), visi tarpai — 8 px kartotiniai, konteineris 1120 px | `styles/layout.css` |
| Matomumas ir pasiekiamumas | Kontrastas tikrintas (tekstas ≥ 4,5:1), matomas fokusas (geltonas brūkšninis), „Pereiti prie turinio“ nuoroda, `aria-*` progreso juostoms, skirtukams ir meniu, etiketės visiems laukams, klaidos susietos su laukais | `styles/base.css`, `components/ui.tsx` |
| Aiškios ir nuoseklios formos | Visur ta pati struktūra: etiketė → laukas → užuomina → klaida; serverio klaidos rodomos prie konkretaus lauko; veiksmo mygtukas sako, kas įvyks („Patvirtinti ir skirti 50 XP“) | `components/ui.tsx` (`Field`) |
| Vientisas dizainas | Viena paletė su 60/30/10 taisykle, dvi šriftų rolės, vienas „pikselio“ vienetas (rėmeliai ir iškirpti kampai) visuose komponentuose | [5 skyrius](#5-dizaino-sistema) |
| Pasiekiama saityne (debesys) | Azure App Service + Azure Database for PostgreSQL: <https://done-n-dusted.azurewebsites.net> | [7 skyrius](#7-diegimas-į-debesį) |
| Kodas Git, dokumentacija | GitHub, šakos `ld1`/`ld2`/`ld3`, PR istorija, šis dokumentas ir [`README.md`](../README.md) | |

---

## 2. Wireframe'ai

Wireframe'ai sugeneruoti skriptu ([`wireframes/generate.mjs`](wireframes/generate.mjs)), todėl juos
lengva pakeisti ir jie visada ryškūs (SVG).

| | |
| --- | --- |
| ![Pradžia](wireframes/01-landing.svg) | ![Taverna](wireframes/02-tavern.svg) |
| ![Grupė](wireframes/03-guild.svg) | ![Užduotis](wireframes/04-quest.svg) |
| ![Kortelė](wireframes/05-sheet.svg) | ![DM modalinis langas](wireframes/06-review-modal.svg) |

![Telefonas](wireframes/07-mobile.svg)

---

## 3. Ekrano nuotraukos

| Kompiuteris | Telefonas |
| --- | --- |
| ![Pradžia](screenshots/01-landing-desktop.png) | ![Pradžia](screenshots/01-landing-mobile.png) |
| ![Grupė](screenshots/03-guild-desktop.png) | ![Grupė](screenshots/03-guild-mobile.png) |
| ![Užduotis](screenshots/04-quest-desktop.png) | ![Meniu](screenshots/06-menu-mobile.png) |

Daugiau — [`screenshots/`](screenshots/). Nuotraukos padarytos automatiškai:
`npm run screenshots -- http://localhost:5173 ../docs/screenshots`.

---

## 4. Sąsajos architektūra

```
frontend/src/
  api/          client.ts (fetch, žetonai, refresh), endpoints.ts (visi API metodai), types.ts
  auth/         AuthContext - prisijungęs naudotojas, login/logout, sesijos atkūrimas
  components/   Layout (header, meniu, footer), ui (Window, Modal, Bar, Field...), formų modaliniai langai
  pages/        Landing, Auth, Tavern, Guild, Quest, Sheet, Friends, AdminUsers, NotFound
  lib/          game.ts (pavadinimai, spalvos, XP lentelė), avatar.ts (pikselinis avataras)
  styles/       tokens, base, layout, components, pages
```

| Sprendimas | Kodėl |
| --- | --- |
| React 19 + TypeScript + Vite | Tipai sutampa su API DTO; Vite — greitas kūrimas ir build |
| TanStack Query | Kešavimas ir pakartotinis užkrovimas po pakeitimų (`invalidateQueries`); 4xx klaidos nekartojamos |
| React Router | Lietuviški keliai (`/taverna`, `/grupes/:id`), `RequireAuth` apsaugo puslapius, `RequireAuth admin` — administravimą |
| Access žetonas tik atmintyje | XSS negali jo perskaityti iš `localStorage`. Refresh — `localStorage`, kad sesija išliktų perkrovus |
| Vienas refresh vienu metu | Serveris rotuoja refresh žetoną ir pakartotinį panaudojimą laiko vagyste. Jei 3 užklausos vienu metu gautų `401`, trys lygiagretūs `refresh` atšauktų visas sesijas — todėl jie laukia to paties pažado (`client.ts`, testas `client.test.ts`) |
| Vystant — Vite proxy | `/api` nukreipiamas į `localhost:8080`, CORS nereikia. Debesyje — `VITE_API_URL` ir backend CORS |
| Teisės sąsajoje | Mygtukai rodomi pagal rolę (DM, savininkas, ADMIN), bet sprendžia serveris — sąsaja tik nerodo to, ko vis tiek neleistų |

---

## 5. Dizaino sistema

Tema — stalo RPG grupė, kuri tarp sesijų pildo savo žygių žurnalą. Vaizdinė kalba — 16 bitų RPG
meniu langai.

| Žetonas | Reikšmė |
| --- | --- |
| Paletė | Sweetie-16, taisyklė 60/30/10: `midnight #1a1c2c` fonas (60 %), `shadow #333c57` paviršiai su `slate` rėmeliu (30 %), `gold #ffcd75` — tik pagrindiniai veiksmai, lygis ir XP (10 %) |
| Charakteristikų spalvos | Visame saite tos pačios: STR `ruby`, DEX `lime`, CON `ember`, INT `sky`, WIS `aqua`, CHA rožinė `#f08fb0` (`--stat-*` žetonai) |
| Šriftai | Pixelify Sans (H1, H2, logotipas) ir IBM Plex Mono (visa kita), skalė 1,25, pagrindinis 16 px; H1 telefone ~32 px su `text-wrap: balance` |
| Tinklelis | 8 px žingsnis, konteineris 1120 px, lūžiai 768 ir 1024 px |
| „Pikselis“ | 4 px rėmeliai, iškirpti kampai (`clip-path`), šešėliai be suliejimo |
| Judesys | `steps()` laiptinė animacija — kaip senuose žaidimuose; vienas automatinis momentas — kortelės juostos |

Įsimintinas elementas — **procedūriškai generuojamas pikselinis avataras**: iš vardo gaunamas
skaičius, iš jo — simetriška 8×8 figūra ir dvi spalvos. Tas pats vardas visada duoda tą patį
avatarą, paveikslėlių saugoti nereikia (`lib/avatar.ts`, testas `avatar.test.ts`).

Grafika (logotipas, iliustracija) generuojama skriptu iš ASCII piešinių ir formų:
`npm run pixel-art`.

---

## 6. Testavimas

| Lygis | Kas | Kiekis | Komanda |
| --- | --- | --- | --- |
| Backend integraciniai | MockMvc + įterptinė PostgreSQL: autentifikacija, teisės, validacija, kortelė, draugystės, sąsajos maršrutai | 42 | `./mvnw test` |
| API kontraktas | Postman kolekcija | 78 užklausos, 176 patikros | `npx newman run postman/DoneNDusted.postman_collection.json` |
| Frontend unit | Vitest + Testing Library: API klientas (refresh vienu metu), avataras, komponentai, formos | 12 | `npm test` |
| End-to-end | Playwright: svečias, prisijungimas, žygis → DM patvirtinimas → kortelė, užduoties forma, `409` prie lauko, draugo kortelė, administravimas, telefono meniu, 404 | 10 | `npm run test:e2e` |

Viskas paleidžiama automatiškai kiekvienam Pull Request — [GitHub Actions](../.github/workflows/ci.yml).
E2E testai radę realią klaidą: atsijungus ir prisijungus kitu naudotoju programa grąžindavo į
ankstesnio naudotojo puslapį (pataisyta `AuthContext` — `signedOut`).

---

## 7. Diegimas į debesį

**Adresas: <https://done-n-dusted.azurewebsites.net>** — sąsaja, API (`/api`), Swagger
(`/swagger-ui.html`) ir sveikatos patikra (`/actuator/health`) vienu adresu.

```
Naršyklė ──HTTPS──► Azure App Service (Italy North, Linux, Java 21, B1)
                      Spring Boot "prod" + React build viduje (classpath:/static)
                          │ JDBC, sslmode=require
                          ▼
                    Azure Database for PostgreSQL 17 (Poland Central, Burstable B1ms, 32 GB)
```

| Sprendimas | Kodėl |
| --- | --- |
| Azure for Students | Nemokamas studentų kreditas; prenumeratos politika leidžia tik 5 regionus: Poland Central, Italy North, Austria East, Belgium Central, Denmark East |
| App Service Italy North | Poland Central regione studentų prenumeratos B1 App Service kvota — 0; Italy North — leidžiama |
| UI tame pačiame App Service | Azure Static Web Apps veikia tik regionuose, kurių politika neleidžia. Todėl React build supakuojamas į jar, o Spring Boot jį atiduoda (`FrontendConfig`): vienas adresas, nereikia CORS, Swagger tame pačiame domene |
| PostgreSQL Flexible Server | Ta pati PostgreSQL kaip lokaliai; Flyway pirmo paleidimo metu sukuria schemą ir demonstracinius duomenis |
| Konfigūracija aplinkos kintamaisiais | `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (48 atsitiktiniai simboliai), `CORS_ORIGINS`, `PORT`/`WEBSITES_PORT=8080` — App Service nustatymuose, ne Git'e |
| Saugumas | Tik HTTPS, TLS ≥ 1.2, FTP išjungtas; DB pasiekiama tik iš Azure paslaugų ir tik su SSL |
| Always On | Programa neužmiega — pirmas atsakymas greitas |

**Diegimas:**

```bash
scripts/package.sh                                   # React build -> static, mvn package
az webapp deploy -g rg-done-n-dusted -n done-n-dusted --type jar \
  --src-path target/done-n-dusted-0.3.0.jar
```

**Patikrinta debesyje:** Postman kolekcija — 176 patikros be klaidų, Playwright — 10 iš 10
scenarijų (`E2E_BASE_URL=https://done-n-dusted.azurewebsites.net npm run test:e2e`). Po testų
duomenų bazė sukurta iš naujo, todėl debesyje — švarūs demonstraciniai duomenys.

---

## 8. Demonstracijos eiga (~7 min)

1. **Pradžia** (svečias): iliustracija mažėja keičiant lango plotį; „Kaip skaičiuojama patirtis?“ —
   modalinis langas, uždaromas `Esc`.
2. **Responsive:** susiaurinti langą < 768 px — meniu tampa hamburger, tinklelis — vienas stulpelis.
3. **Prisijungti** kaip `jonas` (`Demo123!`): taverna, grupė *Iron Vow*, užduoties filtrai (select,
   checkbox).
4. **Užduotis** „Run 50 kilometers“: užregistruoti žygį — `textarea`, `url`, `datetime-local`.
   Bandyti datą ateityje — klaida prie lauko.
5. **Atsijungti, prisijungti** kaip `rokas` (DM): skirtukas *Peržiūra* → *Peržiūrėti* → modalinis
   langas su `radio`, `range`, `number`, `textarea` → patvirtinti.
6. **Veikėjo kortelė** (Nariai → jonas): XP ir charakteristikų juostų animacija.
7. **Nauja užduotis** (modalinis langas): `GRIND` → atsiranda `number` laukas tikslui.
8. **Draugai** (`ieva`): draugo kortelė modaliniame lange — tik viešas vaizdas.
9. **Administravimas** (`admin`): rolės `select`, blokavimo `checkbox`, paieška.
10. **Debesyje:** tas pats per viešą adresą; GitHub — šakos, PR ir žalias CI.

---

## 9. Tikėtini klausimai

**Kodėl access žetonas ne `localStorage`?** Jį perskaitytų bet koks įterptas skriptas (XSS).
Atmintyje jis dingsta perkrovus puslapį, bet tada sesija atkuriama per refresh žetoną.

**Kaip sąsaja žino, ką rodyti?** `/api/auth/me` grąžina naudotojo grupes ir roles jose. Pagal tai
rodomi mygtukai (pvz. *Peržiūra* tik DM). Tikrina vis tiek serveris.

**Kas vyksta, kai access žetonas pasibaigia?** Užklausa gauna `401`, klientas vieną kartą kviečia
`/api/auth/refresh`, išsaugo naują porą ir pakartoja užklausą. Nepavykus — grąžina į prisijungimą
ir po jo atgal į tą patį puslapį.

**Kodėl 768 px?** Ties juo telpa horizontalus meniu ir dviejų stulpelių tinklelis; žemiau —
telefono pločiai, kur viskas viename stulpelyje.

**Kodėl natūralus `<dialog>`?** Naršyklė pati sulaiko fokusą lange, uždaro `Esc`, uždeda foną
(`::backdrop`) ir praneša ekrano skaitytuvams, kad tai modalinis langas.

**Kaip užtikrintas spalvų kontrastas?** Visi teksto ir fono deriniai patikrinti pagal WCAG
(≥ 4,5:1). Pvz. pilkšvas `steel` tekstas footer'yje turėjo tik 2,5:1, todėl ten naudojamas `frost`.
