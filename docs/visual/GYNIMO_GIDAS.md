# Gynimo gidas — Done n Dusted (LD1)

Šis failas skirtas tik pasiruošti gynimui. Prie kiekvieno svarbaus termino ir atsakymo yra dvi dalys:

- **Tiksliai** — ką sakyti dėstytojui, kad skambėtų teisingai.
- **Paprastai (atsimink)** — viena frazė ar palyginimas, kad nepasimestum.

Turinys:

1. [30 sekundžių pristatymas](#1-30-sekundžių-pristatymas)
2. [Žodynėlis — pagrindiniai terminai](#2-žodynėlis--pagrindiniai-terminai)
3. [Atsakymai į tavo klausimus](#3-atsakymai-į-tavo-klausimus)
4. [Kaip keliauja viena užklausa](#4-kaip-keliauja-viena-užklausa)
5. [Kiekvienas failas: ką rodyti ir ką sakyti](#5-kiekvienas-failas-ką-rodyti-ir-ką-sakyti)
6. [Tikėtini gynimo klausimai su atsakymais](#6-tikėtini-gynimo-klausimai-su-atsakymais)
7. [Silpnos vietos ir kaip apie jas kalbėti](#7-silpnos-vietos-ir-kaip-apie-jas-kalbėti)
8. [Demonstracijos eiga](#8-demonstracijos-eiga)

---

## 1. 30 sekundžių pristatymas

Išmok šitą beveik mintinai. Nuo jo pradėk, kai paklaus „papasakok apie projektą“.

> Done n Dusted — produktyvumo sistema, veikianti DnD žaidimo principu. Yra trys resursai,
> sujungti hierarchija: **grupė (Guild) → užduotis (Quest) → progreso įrašas (LogEntry)**.
> Žaidėjas registruoja progresą, o grupės Dungeon Master jį patvirtina. Tik tada skiriami XP taškai.
>
> Tai REST API, parašytas su Java 21 ir Spring Boot. Jame yra 15 metodų: kiekvienam resursui po
> 5 (sąrašas, vienas, sukurti, atnaujinti, ištrinti). Duomenys saugomi PostgreSQL, schemą sukuria
> Flyway. Dokumentacija — OpenAPI / Swagger, o visi metodai patikrinti Postman kolekcija
> (31 užklausa, 75 patikros).
>
> Kodas suskirstytas sluoksniais: **Controller** priima HTTP užklausą, **Service** tikrina
> taisykles, **Repository** kalba su duomenų baze. Klientui grąžinami **DTO**, o visos klaidos
> turi vienodą `ApiError` formatą.

---

## 2. Žodynėlis — pagrindiniai terminai

### REST
- **Tiksliai:** architektūros stilius, kuriame sistema pateikiama kaip **resursai** su URL adresais
  (`/api/guilds/2`). Su jais dirbama standartiniais HTTP metodais (GET, POST, PATCH, DELETE).
  Serveris neatsimena kliento tarp užklausų (*stateless*).
- **Paprastai:** kiekvienas daiktas turi savo adresą, o veiksmą nusako HTTP metodas.

### Resursas
- **Tiksliai:** dalykinės srities objektas, pasiekiamas per URI. Pas tave tai Guild, Quest ir LogEntry.
- **Paprastai:** daiktas, su kuriuo API leidžia dirbti.

### Endpoint (galinis taškas)
- **Tiksliai:** konkretus HTTP metodo ir kelio derinys, pvz. `POST /api/guilds`.
- **Paprastai:** vienas „mygtukas“ API. Tavo API jų turi 15.

### HTTP metodai
| Metodas | Ką daro | Idempotentiškas? |
| --- | --- | --- |
| `GET` | gauna duomenis, nieko nekeičia | taip |
| `POST` | sukuria naują resursą | **ne** — du kartus = du įrašai |
| `PATCH` | **dalinai** pakeičia resursą — tik atsiųstus laukus (pas tave atnaujinimas yra šis) | pagal HTTP nebūtinai; pas tave taip, nes PATCH nustato reikšmes, o ne „prideda +1“ |
| `PUT` | **pilnai** pakeičia resursą — neatsiųstas laukas tampa `null` (pas tave nenaudojamas → `405`) | taip — kartojant rezultatas tas pats |
| `DELETE` | ištrina | taip — būsena po antro karto ta pati (nors atsakymas jau 404) |

- **Paprastai (idempotentiškumas):** paspaudus tą patį mygtuką 10 kartų, sistemos būsena tokia pati kaip paspaudus 1 kartą.

### Statuso kodas
- **Tiksliai:** trijų skaitmenų skaičius atsakyme. `2xx` — sėkmė, `4xx` — kliento klaida, `5xx` — serverio klaida.
- **Paprastai:** 2 — gerai, 4 — kaltas klientas, 5 — kaltas serveris.

### Užklausos turinys (request body) / payload
- **Tiksliai:** duomenys, siunčiami HTTP užklausos viduje (pas tave — JSON), pvz. naujos grupės
  pavadinimas. Siunčiami su `POST` ir `PATCH`. `GET` ir `DELETE` turinio neturi.
- **Paprastai:** „laiško tekstas“. URL yra adresas ant voko, o body — kas voke viduje.
- Lietuviškai sakyk **„užklausos turinys“**, ne „kūnas“. Tai jau pataisyta visuose failuose.

### Antraštė (header)
- **Tiksliai:** metaduomenys apie užklausą ar atsakymą. Pas tave svarbiausios yra trys:
  `Content-Type` (kokiu formatu siunčiu), `Accept` (kokio formato noriu atgal), `Location` (naujo resurso adresas po `201`).
- **Paprastai:** užrašai ant voko — kas viduje ir kur atsakyti.

### JSON
- **Tiksliai:** tekstinis duomenų formatas `{"name": "Iron Vow"}`.
- **Paprastai:** būdas užrašyti objektą tekstu, kad suprastų bet kuri programa.

### Java ir Spring Boot
- **Tiksliai:** Java — programavimo kalba. Spring Boot — karkasas (*framework*) Java programoms.
  Jis automatiškai sukonfigūruoja web serverį, JSON konvertavimą, prisijungimą prie DB, transakcijas ir validaciją.
- **Paprastai:** Java — kalba, Spring Boot — paruoštas konstruktorius. Jis duoda serverį ir
  visą infrastruktūrą, o aš rašau tik savo logiką.

### Anotacija (`@Something`)
- **Tiksliai:** metaduomenys Java kode, kuriuos skaito karkasas. Pvz., `@GetMapping` sako Spring:
  „šis metodas atsako į GET“.
- **Paprastai:** lipdukas ant kodo, kuris Spring'ui pasako, ką su juo daryti.

### Dependency Injection (DI) ir Bean
- **Tiksliai:** Spring pats sukuria objektus (*beans*): kontrolerius, servisus, repozitorijas.
  Priklausomybes jis paduoda per konstruktorių. Pvz., `GuildController` gauna `GuildService`,
  bet niekur nerašo `new GuildService()`.
- **Paprastai:** aš nesirūpinu, iš kur atsiranda reikalingi objektai — Spring juos atneša.

### Entity
- **Tiksliai:** Java klasė, atitinkanti DB lentelę (`@Entity`). Vienas objektas = viena lentelės eilutė.
- **Paprastai:** DB lentelės eilutė, paversta Java objektu.

### JPA / Hibernate / ORM
- **Tiksliai:** ORM (*Object-Relational Mapping*) susieja Java objektus su DB lentelėmis.
  JPA — standartas, Hibernate — jo realizacija. Spring Data JPA virš jo sugeneruoja repozitorijas.
- **Paprastai:** vertėjas tarp Java ir SQL. Aš dirbu su objektais, jis rašo SQL.

### Repository
- **Tiksliai:** interfeisas duomenų prieigai. Paveldi `JpaRepository`, todėl `findAll`, `save`,
  `delete` jau yra. Papildomus metodus Spring Data sugeneruoja **iš pavadinimo**
  (`existsByNameIgnoreCase`).
- **Paprastai:** sandėlininkas — paduoda ir padeda daiktus į DB.

### Service
- **Tiksliai:** verslo logikos sluoksnis — dalykinės srities taisyklės, transakcijos, kelių repozitorijų koordinavimas.
- **Paprastai:** smegenys. Čia sprendžiama, ar užklausa leistina.

### Controller
- **Tiksliai:** HTTP sluoksnis. Susieja URL ir metodą su Java metodu, paleidžia validaciją, kviečia servisą ir grąžina atsakymą.
- **Paprastai:** padavėjas — priima užsakymą ir atneša lėkštę, bet pats negamina.

### DTO (Data Transfer Object)
- **Tiksliai:** objektas, skirtas duomenims perduoti per API. `*Request` — ką klientas gali atsiųsti,
  `*Response` — ką klientas mato. Atskirtas nuo Entity.
- **Paprastai:** vokas, kuriuo duomenys keliauja per tinklą. Klientui niekada nerodome pačios DB lentelės.

### Record (`public record ...`)
- **Tiksliai:** Java 16+ konstrukcija nekintamiems duomenų objektams. Konstruktorių, getterius,
  `equals` ir `toString` kompiliatorius sugeneruoja pats.
- **Paprastai:** trumpas būdas aprašyti „duomenų paketą“. Visi tavo DTO yra record'ai.

### Validacija (`@Valid`, `@NotBlank`, `@Size`)
- **Tiksliai:** Bean Validation. Anotacijos prie DTO laukų aprašo apribojimus. `@Valid` kontroleryje
  juos paleidžia, o nepraėjus grąžinamas `400` su laukų sąrašu.
- **Paprastai:** apsauga prie durų — tuščias pavadinimas net neįleidžiamas į servisą.

### Transakcija (`@Transactional`)
- **Tiksliai:** operacijų grupė DB, kuri įvykdoma **visa arba nieko**. Jei metode išmetama
  `RuntimeException`, viskas atšaukiama (*rollback*).
- **Paprastai:** arba viskas pavyksta, arba lyg nieko nebūtų įvykę.

### Flyway / migracija
- **Tiksliai:** įrankis, kuris paleidimo metu paleidžia versijuotus SQL failus (`V1__schema.sql`,
  `V2__seed_data.sql`) ir atsimena, kurie jau įvykdyti.
- **Paprastai:** DB „istorija“ — schema kuriama tik SQL failais, ne ranka.

### Puslapiavimas (pagination)
- Žr. [3.5](#35-ar-naudoju-puslapiavimą-pagination-kas-tai-ir-kodėl).
- **Paprastai:** kaip Google rezultatai — po 10 per puslapį, o ne milijonas iš karto.

### OpenAPI / Swagger
- **Tiksliai:** OpenAPI — standartinis API aprašymo formatas (JSON/YAML). Swagger UI — naršyklės
  puslapis, kuris iš to aprašo sugeneruoja interaktyvią dokumentaciją. springdoc jį sugeneruoja iš tavo anotacijų.
- **Paprastai:** API instrukcija, kurioje galima ir paspausti „Try it out“.

### Profilis (`dev` / `prod`)
- **Tiksliai:** Spring konfigūracijos rinkinys. `dev` (numatytasis) paleidžia įterptą PostgreSQL,
  `prod` jungiasi prie tikro serverio pagal aplinkos kintamuosius.
- **Paprastai:** du režimai — demonstracijai ir „tikram gyvenimui“.

### Maven / `mvnw`
- **Tiksliai:** Maven — Java projekto surinkimo įrankis (priklausomybės, kompiliavimas, jar).
  `mvnw` — wrapper'is, kuris atsisiunčia reikiamą Maven versiją pats.
- **Paprastai:** `pom.xml` — pirkinių sąrašas, Maven — kas viską nuperka ir surenka.

### Enum
- **Tiksliai:** tipas su fiksuotu reikšmių rinkiniu (`PENDING, APPROVED, REJECTED`).
- **Paprastai:** pasirinkimas iš sąrašo, kitokios reikšmės būti negali.

---

## 3. Atsakymai į tavo klausimus

### 3.1 Kuo skiriasi `EnumType.STRING` nuo `ORDINAL`? Kodėl STRING geriau?

Vieta kode: [LogEntry.java:41-43](../../src/main/java/lt/ktu/donendusted/domain/LogEntry.java#L41-L43)
(tas pats naudojama [Quest.java](../../src/main/java/lt/ktu/donendusted/domain/Quest.java) laukams `stat`, `type`, `difficulty`).

**Tiksliai.** `@Enumerated` nurodo, kaip enum reikšmė įrašoma į DB stulpelį:

| | `ORDINAL` (numatytasis JPA!) | `STRING` (tavo pasirinkimas) |
| --- | --- | --- |
| Kas įrašoma | eilės numeris: `PENDING`=0, `APPROVED`=1, `REJECTED`=2 | pavadinimas: `'APPROVED'` |
| DB atrodo | `status = 1` — neaišku, ką reiškia | `status = 'APPROVED'` — aišku iškart |
| Pakeitus enum tvarką ar įterpus reikšmę į vidurį | **seni duomenys tyliai pakeičia prasmę** | nieko neatsitinka |
| Pervadinus reikšmę | nieko neatsitinka | reikia migracijos (bet klaida matoma, ne tyli) |
| Vieta | 4 baitai | keli baitai daugiau (nereikšminga) |

**Pavyzdys, kodėl ORDINAL pavojingas.** Tarkim, vėliau pridedi `CANCELLED` sąrašo pradžioje:
`CANCELLED, PENDING, APPROVED, REJECTED`. Su ORDINAL visi DB įrašai su `0` (buvę PENDING) staiga
tampa CANCELLED, o buvę APPROVED (`1`) — PENDING. Jokios klaidos nebus, duomenys tiesiog
sugadinti. Su STRING DB toliau saugo `'PENDING'` ir viskas veikia.

Dar vienas privalumas: tavo SQL schemoje yra `CHECK (status in ('PENDING','APPROVED','REJECTED'))`
([V1__schema.sql](../../src/main/resources/db/migration/V1__schema.sql)). Taip DB pati saugo nuo
neleistinų reikšmių, o su skaičiais tai būtų mažiau aišku.

**Paprastai (atsimink):** *ORDINAL įsimena vietą eilėje, STRING — vardą. Jei eilė pasikeis,
vieta sumeluos, o vardas — ne.*

### 3.2 Dėl kokių savybių reikėjo Spring Boot ir Java?

**Kodėl Java:** ji pasirinkta LD0 ataskaitoje. Tai griežtai tipizuota kalba, todėl daug klaidų
pagaunama dar kompiliuojant. Java 21 yra LTS (ilgai palaikoma) versija, joje yra `record` ir
naujas `switch`, kuriuos naudoji.

**Kodėl Spring Boot — konkrečios savybės, kurias projektas tikrai naudoja:**

| Spring Boot savybė | Kur matosi tavo projekte | Ko be jos reikėtų |
| --- | --- | --- |
| Įterptas web serveris (Tomcat) | `./mvnw spring-boot:run` ir veikia `:8080` | atskirai diegti ir konfigūruoti serverį |
| Spring MVC anotacijos | `@RestController`, `@GetMapping`, `@PathVariable` | patiems parsinti URL ir HTTP |
| Automatinis JSON (Jackson) | DTO record'ai virsta JSON savaime | rankomis rašyti JSON konvertavimą |
| Bean Validation | `@Valid`, `@NotBlank`, `@Size` | `if` sakiniai kiekvienam laukui |
| Spring Data JPA | repozitorijos be SQL, `Pageable`, `Specification` | rašyti SQL kiekvienai užklausai |
| `@Transactional` | servisuose | rankomis valdyti commit / rollback |
| `@RestControllerAdvice` | `ApiExceptionHandler` — visos klaidos vienoje vietoje | try/catch kiekviename metode |
| Flyway integracija | migracijos paleidžiamos automatiškai | paleisti SQL ranka |
| Profiliai | `dev` / `prod` | kelios konfigūracijos versijos |
| Dependency Injection | konstruktoriai be `new` | patiems kurti ir jungti objektus |
| springdoc | Swagger UI iš anotacijų | rašyti OpenAPI ranka |

**Paprastai (atsimink):** *Spring Boot padaro visą nuobodžią infrastruktūrą — serverį, JSON, DB,
klaidas, dokumentaciją. Aš rašau tik taisykles ir resursus.*

### 3.3 „Kūnas“ → „užklausos turinys“ (pataisyta)

„Kūnas“ — pažodinis angliško *body* vertimas, lietuviškoje terminijoje netinkamas. Pakeista taip:

| Buvo | Dabar |
| --- | --- |
| „atsakymo kūno nėra“ (204) | „atsakymas be turinio“ |
| „kūnas ne JSON“ (415) | „užklausos turinys (body) ne JSON“ |
| „kūno laukams“ | „užklausos turinio (body) laukams“ |
| „enum reikšmė kūne“ | „enum reikšmė užklausos turinyje“ |

Pataisyti failai: `ApiExceptionHandler.java`, `GuildController.java`, `QuestController.java`,
`LogEntryController.java`, `README.md`, `docs/openapi.json`, `docs/DEMO_KOMANDOS.txt`,
`postman/DoneNDusted.postman_collection.json` ir dvi Postman YAML užklausos. Kitų netinkamų
terminų komentaruose neradau. „Payload“, „kelio kintamasis“, „antraštė“ ir „puslapiavimas“ yra tinkami.

Gynime sakyk: **request body = užklausos turinys**, **response body = atsakymo turinys**,
**header = antraštė**, **path variable = kelio kintamasis**, **query parameter = užklausos parametras**.

### 3.4 Enumeration attack ant `guildId` — ar reikia UUID? Dabar, vėliau ar visai ne?

**Kas tai.** ID yra iš eilės einantys skaičiai (1, 2, 3…), todėl užpuolikas gali tiesiog bandyti
`/api/guilds/1`, `/2`, `/3`… ir sužinoti, kokie resursai egzistuoja ir kiek jų yra.

**Ar tai problema dabar? Ne, ir štai kodėl:**
1. LD1 neturi autentifikacijos. Visos grupės ir taip viešai matomos per `GET /api/guilds`.
   Spėliojant ID nesužinosi nieko daugiau, nei parodo sąrašas.
2. Tikroji apsauga nuo enumeration yra **autorizacija**, ne ID forma: serveris turi patikrinti,
   ar *šis* vartotojas gali matyti *šią* grupę. Tai bus LD2 (JWT, rolės).
3. UUID be autorizacijos — tik *security through obscurity* (saugumas paslėpimu). Ilgą ID
   sunkiau atspėti, bet jei jis kur nors nutekės, durys vis tiek atviros.

**Ką jau gerai darai:** svetima grandinė (`/guilds/1/quests/5`, kai quest 5 yra kitoje grupėje)
grąžina `404`, ne `403`. Taip neatskleidžiama, kad resursas egzistuoja.

**Ar verta implementuoti UUID dabar? Ne.** Kaina didelė, nauda LD1 lygyje beveik nulinė:
reiktų keisti schemą, seed duomenis, visus DTO, Postman kolekciją, OpenAPI ir demo komandas.
Be to, atsitiktiniai UUID (v4) blogina DB indeksų efektyvumą.

**Jei vėliau prireiktų** (pvz., nenorite atskleisti, kiek vartotojų turi sistema): paliekamas
`bigint id` kaip vidinis pirminis raktas ir pridedamas `public_id uuid` stulpelis, naudojamas URL'e.
Geriausia UUID v7 — jis laiko atžvilgiu didėjantis, todėl indeksai nenukenčia.

**Ką sakyti gynime:**
> „Suprantu, kad nuoseklūs ID leidžia enumeration. LD1 duomenys vieši, todėl tai nieko
> neatskleidžia. Tikra apsauga bus autorizacija LD2: tikrinsiu, ar vartotojas priklauso grupei,
> ir svetimiems grąžinsiu 404. UUID būtų papildomas sluoksnis, o ne pakaitalas.“

**Paprastai (atsimink):** *UUID — kaip buto numerį pakeisti atsitiktiniu kodu: vagiui sunkiau
atspėti, bet spynos (autorizacijos) vis tiek reikia. Spyna bus LD2.*

### 3.5 Ar naudoju puslapiavimą (pagination)? Kas tai ir kodėl?

**Taip.** Visi trys sąrašų metodai (`GET /guilds`, `GET .../quests`, `GET .../logs`) yra puslapiuoti.

**Kas tai.** Užuot grąžinus visus įrašus iš karto, grąžinama dalis (puslapis), o klientas gali
paprašyti kito: `?page=0&size=20&sort=name,asc`.

**Kodėl reikia:**
- jei DB turės 100 000 įrašų, vienas GET be puslapiavimo apkrautų serverį, DB ir tinklą;
- klientas (UI) vis tiek rodo po kelias dešimtis;
- apsauga: `max-page-size: 100` ([application.yml](../../src/main/resources/application.yml)) —
  net `?size=100000` bus apkarpytas iki 100.

**Kaip realizuota:**
1. Kontroleryje `@PageableDefault(size = 20, sort = "name") Pageable pageable` — Spring pats
   perskaito `page`, `size`, `sort` iš URL.
2. Repozitorija `findAll(spec, pageable)` sugeneruoja SQL su `LIMIT` / `OFFSET` ir papildomą `COUNT`.
3. [PagedResponse.java](../../src/main/java/lt/ktu/donendusted/web/PagedResponse.java) supakuoja
   atsakymą: `content` (įrašai), `page` (numeris, dydis, kiek iš viso) ir `links`
   (`self`, `first`, `prev`, `next`, `last`). Nuorodose išlieka visi filtrai.
4. Puslapiai skaičiuojami nuo 0.
5. Rūšiavimas pagal neegzistuojantį lauką (`?sort=bogus`) grąžina `400`.

**Paprastai (atsimink):** *kaip knyga — skaitai po puslapį, o apačioje yra „kitas“ ir „ankstesnis“.*

**Kaip parodyti Postman:** aplanke `1. Grupės` užklausa *Puslapiavimas – pirmas puslapis*
(`?page=0&size=2&sort=id,asc`) grąžina 2 grupes ir `links.next`, o *Puslapiavimas – kitas puslapis
pagal links.next* atidaro tą nuorodą. Kitos sąrašų užklausos rodo tiesiog duomenis — page info
tikrinti visur nebūtina. *Gauti grupių sąrašą* be parametrų grąžina **visas** grupes (iki 20).

### 3.5a Hypermedia — ne tik puslapiavimui, o patogumui

**Kas tai.** Atsakyme kartu su duomenimis pateikiamos **nuorodos** į susijusius resursus ar
galimus veiksmus. Klientas pats nekonstruoja URL — jis eina serverio duotomis nuorodomis
(REST principas HATEOAS: *Hypermedia As The Engine Of Application State*).

**Kur tai yra pas tave:**
- sąrašų `links` (`self`, `first`, `prev`, `next`, `last`) — klientui nereikia žinoti, kaip
  sudaromas kito puslapio URL ir kad reikia išlaikyti filtrus; jis tiesiog seka `next`;
- `Location` antraštė po `POST` — nuoroda į ką tik sukurtą resursą. Tai irgi hypermedia ir
  su puslapiavimu nesusijusi: klientas iškart žino naujo resurso adresą;
- Postman *Suvestinės* testas seka `links.next`, kol jos nebėra — todėl veiktų ir su 1000 grupių.

**Kodėl tai patogu, net kai puslapiavimo nėra:**
- nuorodos į **vaikinius** resursus (grupė → `/guilds/2/quests`) ir **tėvinį** (užduotis →
  `/guilds/2`) leidžia naršyti API kaip svetainę;
- nuorodos į **galimus veiksmus** pagal būseną: pvz. `PENDING` įrašas galėtų turėti nuorodą
  „patvirtinti“, o `APPROVED` — nebe. Klientas iš nuorodų mato, ką dabar galima daryti;
- pasikeitus URL struktūrai serveryje, klientas, sekantis nuorodas, nesulūžta.

**Sąžiningai:** pavieniai resursai (`GuildResponse` ir kt.) nuorodų dar neturi — jos yra tik
sąrašuose ir `Location` antraštėje. Pridėti būtų paprasta: `links: { self, quests }` grupei,
`links: { self, guild, logs }` užduočiai.

**Paprastai (atsimink):** *kaip svetainė su mygtukais — nereikia mintinai žinoti adresų, tiesiog
spaudi nuorodas, kurias duoda puslapis.*

### 3.6 Kaip kalbėti apie Service klases, DTO ir kitus sluoksnius?

**Restorano palyginimas (išmok šitą):**

| Sluoksnis | Restorane | Tavo projekte | Ką daro |
| --- | --- | --- | --- |
| Controller | padavėjas | `GuildController` | priima užsakymą (HTTP), atneša lėkštę (JSON). Pats negamina |
| DTO Request | užsakymo lapelis | `GuildRequest` | ką klientas **gali** užsakyti. Nežinomas laukas → 400 |
| Service | virėjas | `GuildService` | taisyklės: ar pavadinimas laisvas, ar GRIND turi targetCount |
| Repository | sandėlininkas | `GuildRepository` | paima ir padeda produktus į sandėlį (DB) |
| Entity | produktas sandėlyje | `Guild` | DB lentelės eilutė |
| DTO Response | lėkštė su patiekalu | `GuildResponse` | ką klientas **mato** — gražiai paruošta, be virtuvės detalių |
| ExceptionHandler | salės vadybininkas | `ApiExceptionHandler` | bet kokį nusiskundimą sutvarko vienodai (`ApiError`) |

**Kodėl Service atskirtas nuo Controller (tiksliai):**
- **atsakomybių atskyrimas** (*separation of concerns*): kontroleris žino HTTP, servisas — taisykles;
- taisyklės vienoje vietoje — tą pačią `QuestService.get()` naudoja ir `LogEntryService`;
- `@Transactional` dedamas ant serviso: visa operacija yra viena transakcija;
- servisą galima testuoti be HTTP.

**Kodėl DTO, o ne Entity tiesiai (tiksliai):**
1. **Saugumas.** `LogEntryCreateRequest` **neturi** `xpAwarded` lauko, todėl klientas fiziškai negali
   pats sau prisirašyti XP. O su `fail-on-unknown-properties: true` bandymas atsiųsti tokį lauką gauna 400.
2. **Kontrolė.** Response gali turėti apskaičiuotų laukų, kurių DB nėra (`xpPerCompletion` QuestResponse'e).
3. **Stabilumas.** Pakeitus DB lentelę, API sutartis klientams nesikeičia.
4. **Techniškai.** Entity turi LAZY ryšius. Grąžinant juos tiesiai būtų klaidų arba begalinė rekursija JSON'e.

**Paprastai (atsimink):** *Controller — padavėjas, Service — virėjas, Repository — sandėlininkas,
DTO — lėkštė. Klientas niekada neina į sandėlį.*

### 3.7 `target/` — automatiškai generuojamas?

**Taip, bet tai ne Java kodas.** `target/` — Maven **surinkimo rezultatų** aplankas. Jis sukuriamas,
kai paleidi `./mvnw spring-boot:run` arba `./mvnw package`. Viduje:

| Aplankas / failas | Kas tai |
| --- | --- |
| `classes/` | sukompiliuoti `.class` failai (baitkodas, kurį vykdo JVM) ir nukopijuoti `application.yml`, SQL migracijos |
| `generated-sources/` | anotacijų procesorių sugeneruotas kodas (pas tave praktiškai tuščias) |
| `done-n-dusted-0.1.0.jar` | visa programa viename faile. Paleidžiama `java -jar target/done-n-dusted-0.1.0.jar` |
| `*.jar.original` | jar be priklausomybių (prieš Spring Boot perpakavimą) |
| `maven-status/`, `maven-archiver/` | Maven vidiniai žymekliai |

Jis įrašytas į `.gitignore` — į git nepatenka. Jį galima drąsiai ištrinti (`./mvnw clean`),
kitą kartą surenkant atsiras iš naujo.

**Paprastai (atsimink):** *`src/` — receptas, `target/` — iškeptas pyragas. Pyragą visada gali iškepti iš naujo.*

### 3.8 Jei Docker neimplementuotas, kodėl yra Docker failas?

Yra tik `docker-compose.yml`. **Dockerfile nėra** — pati programa į konteinerį nedėta.

`docker-compose.yml` — **neprivalomas** būdas paleisti **nuolatinę** PostgreSQL duomenų bazę
`prod` profiliui. Paleidimo būdai:

| Būdas | Komanda | DB |
| --- | --- | --- |
| Numatytasis (`dev`) | `./mvnw spring-boot:run` | įterptas PostgreSQL, port 55432, duomenys dingsta sustabdžius |
| `prod` + Docker | `docker compose up -d` ir `./mvnw spring-boot:run -Dspring-boot.run.profiles=prod` | PostgreSQL konteineryje, port 5432, duomenys išlieka |
| `prod` be Docker | savas PostgreSQL + `DB_URL`, `DB_USER`, `DB_PASSWORD` | bet kuris serveris |

Kūrimo metu kompiuteryje Docker'io nebuvo, todėl numatytasis režimas jo nereikalauja. Dėstytojui
nereikia nieko diegti. Compose failas lieka kaip patogus kelias tiems, kas nori tikros
nuolatinės DB, ir kaip pagrindas vėlesniam diegimui (LD3).

**Ką sakyti:**
> „Docker nėra privalomas. Programa pagal nutylėjimą pati pasileidžia tikrą PostgreSQL be jokio
> diegimo. `docker-compose.yml` yra alternatyva `prod` profiliui, kai reikia nuolatinės duomenų bazės.“

**Paprastai (atsimink):** *Docker — atsarginis ratas. Važiuoti galima ir be jo.*

### 3.9 Ką rodyti ir kaip kalbėti apie kiekvieną failą

Žr. [5 skyrių](#5-kiekvienas-failas-ką-rodyti-ir-ką-sakyti).

---

## 4. Kaip keliauja viena užklausa

Pavyzdys: `POST /api/guilds/2/quests` su JSON `{"title":"Run","stat":"CON","type":"GRIND","difficulty":"HARD"}`
(be `targetCount`).

```
Klientas (Postman)
   │  POST /api/guilds/2/quests   Content-Type: application/json
   ▼
Spring (DispatcherServlet)
   │  1. Ar yra toks kelias + metodas?           ne → 404 / 405
   │  2. Ar Content-Type = JSON?                 ne → 415
   │  3. Ar "2" yra skaičius?                    ne → 400
   │  4. Ar JSON perskaitomas, laukai žinomi?    ne → 400
   ▼
QuestController.create()
   │  5. @Valid → @NotBlank title, @NotNull stat…  ne → 400 + details[]
   ▼
QuestService.create()            ← @Transactional prasideda
   │  6. guilds.get(2) — ar grupė yra?           ne → 404
   │  7. validate() — GRIND be targetCount?      TAIP → BusinessRuleException → 422  ◄── šis pavyzdys čia
   │  8. ar pavadinimas grupėje laisvas?         ne → 409
   │  9. quests.save(new Quest(...))
   ▼
QuestRepository → Hibernate → SQL INSERT → PostgreSQL
   │  10. DB CHECK / UNIQUE apribojimai           pažeista → 409
   ▼                                ← @Transactional commit
QuestController
   │  11. Quest (entity) → QuestResponse (DTO)
   ▼
201 Created + Location: /api/guilds/2/quests/10 + JSON
```

Kiekviena klaida bet kuriame žingsnyje patenka į `ApiExceptionHandler` ir grįžta vienodu `ApiError` formatu.

**Paprastai:** *pirma Spring patikrina formą, tada @Valid — laukus, tada servisas — taisykles,
galiausiai DB — paskutinė apsauga.*

---

## 5. Kiekvienas failas: ką rodyti ir ką sakyti

Kiekvienam failui: **Rodyk** (į kurią vietą užvesti pelę) ir **Sakyk** (1–3 sakiniai).

### Šaknis

**[pom.xml](../../pom.xml)**
- **Rodyk:** `<parent>` su Spring Boot 4.1.1, `java.version` 21, priklausomybių sąrašą.
- **Sakyk:** „Maven projekto aprašas. Starter'iai: `web` — REST, `data-jpa` — DB, `validation` —
  laukų tikrinimas, `flyway` — migracijos. `springdoc` generuoja Swagger, `embedded-postgres`
  leidžia paleisti tikrą PostgreSQL be diegimo.“

**[application.yml](../../src/main/resources/application.yml)**
- **Rodyk:** `ddl-auto: validate`, `open-in-view: false`, `fail-on-unknown-properties: true`, `max-page-size: 100`.
- **Sakyk:** „Schemą kuria Flyway, Hibernate tik patikrina, ar entity atitinka lenteles. Nežinomas
  JSON laukas grąžina 400 — taip klientas negali įsiųsti, pvz., `xpAwarded`. Puslapio dydis ribojamas iki 100.“

**[application-prod.yml](../../src/main/resources/application-prod.yml)**
- **Sakyk:** „`prod` profilis: DB prisijungimas iš aplinkos kintamųjų, slaptažodžiai kode neįrašyti.“

**[docker-compose.yml](../../docker-compose.yml)**
- **Sakyk:** žr. [3.8](#38-jei-docker-neimplementuotas-kodėl-yra-docker-failas).

### Paleidimas ir konfigūracija

**[DoneNDustedApplication.java](../../src/main/java/lt/ktu/donendusted/DoneNDustedApplication.java)**
- **Sakyk:** „Įėjimo taškas. `@SpringBootApplication` įjungia automatinę konfigūraciją ir
  susiranda visus kontrolerius, servisus ir repozitorijas šiame pakete.“

**[EmbeddedPostgresConfig.java](../../src/main/java/lt/ktu/donendusted/config/EmbeddedPostgresConfig.java)**
- **Rodyk:** `@Profile("dev")`, [eilutė 24](../../src/main/java/lt/ktu/donendusted/config/EmbeddedPostgresConfig.java#L24) `setPort(55432)`.
- **Sakyk:** „Tik `dev` profilyje paleidžiamas tikras PostgreSQL 18 procesas — ne H2 ir ne emuliacija.
  Portas fiksuotas, kad galėčiau parodyti lenteles DBeaver'yje. Sustabdžius programą, DB irgi sustoja.“

**[OpenApiConfig.java](../../src/main/java/lt/ktu/donendusted/config/OpenApiConfig.java)**
- **Sakyk:** „Bendra OpenAPI informacija: pavadinimas, versija, aprašymas. Endpoint'ų aprašai
  ateina iš `@Operation` / `@ApiResponse` anotacijų kontroleriuose.“

### Duomenų bazė

**[V1__schema.sql](../../src/main/resources/db/migration/V1__schema.sql)**
- **Rodyk:** `references guild (id) on delete cascade`, `unique (guild_id, title)`, `ck_log_pending_is_empty`.
- **Sakyk:** „Trys lentelės su išoriniais raktais. Ištrynus grupę, kaskadiškai dingsta jos užduotys
  ir įrašai. Taisyklės dubliuojamos DB lygyje per CHECK — net jei Java kode būtų klaida,
  PENDING įrašas negalėtų turėti XP.“

**[V2__seed_data.sql](../../src/main/resources/db/migration/V2__seed_data.sql)**
- **Sakyk:** „Prasmingi demonstraciniai duomenys: 3 grupės, 9 užduotys, 12 įrašų visose būsenose.“

### `domain/` — Entity ir enum

**[Guild.java](../../src/main/java/lt/ktu/donendusted/domain/Guild.java)**
- **Rodyk:** `@Entity`, `@GeneratedValue(strategy = IDENTITY)`, `protected Guild()`.
- **Sakyk:** „Entity — viena `guild` lentelės eilutė. ID generuoja PostgreSQL. Tuščias
  konstruktorius `protected`, nes jo reikia JPA, bet kode grupės be vardo sukurti negalima.“

**[Quest.java](../../src/main/java/lt/ktu/donendusted/domain/Quest.java)**
- **Rodyk:** `@ManyToOne(fetch = LAZY)` į Guild, `@Enumerated(STRING)`.
- **Sakyk:** „Užduotis priklauso vienai grupei — ryšys daug-su-vienu. LAZY reiškia, kad grupė iš DB
  kraunama tik kai jos prireikia.“

**[LogEntry.java](../../src/main/java/lt/ktu/donendusted/domain/LogEntry.java)**
- **Rodyk:** [STRING komentarą (41)](../../src/main/java/lt/ktu/donendusted/domain/LogEntry.java#L41),
  [`approve()` (79)](../../src/main/java/lt/ktu/donendusted/domain/LogEntry.java#L79), kad status setterio nėra.
- **Sakyk:** „Būsena keičiama tik per metodus `approve`, `reject`, `resetToPending`, ne per setterį.
  Taip XP gali atsirasti tik patvirtintame įraše. XP užšaldomas — pakeitus užduoties sudėtingumą,
  istorija nesikeičia.“

**[Difficulty.java](../../src/main/java/lt/ktu/donendusted/domain/Difficulty.java)**
- **Rodyk:** `HARD(5)`, `xpValue()`.
- **Sakyk:** „Enum su reikšme: sudėtingumas yra XP daugiklis, XP = 10 × daugiklis. Serveris
  pats apskaičiuoja XP, klientas jo neduoda.“

**[LogStatus.java](../../src/main/java/lt/ktu/donendusted/domain/LogStatus.java)**, **[QuestType.java](../../src/main/java/lt/ktu/donendusted/domain/QuestType.java)**, **[Stat.java](../../src/main/java/lt/ktu/donendusted/domain/Stat.java)**
- **Sakyk:** „Fiksuoti pasirinkimai. Neteisinga reikšmė JSON'e ar URL'e grąžina 400.“

### `repository/`

**[GuildRepository.java](../../src/main/java/lt/ktu/donendusted/repository/GuildRepository.java)**
- **Rodyk:** `existsByNameIgnoreCase`.
- **Sakyk:** „SQL nerašau — Spring Data sugeneruoja užklausą iš metodo pavadinimo.
  `JpaSpecificationExecutor` leidžia dinaminius filtrus.“

**[QuestRepository.java](../../src/main/java/lt/ktu/donendusted/repository/QuestRepository.java)**
- **Rodyk:** `findByIdAndGuildId`.
- **Sakyk:** „Užduoties ieškau pagal **abu** ID, todėl užduotis iš kitos grupės grąžina 404.“

**[LogEntryRepository.java](../../src/main/java/lt/ktu/donendusted/repository/LogEntryRepository.java)**
- **Rodyk:** [`findByIdAndQuestIdAndQuestGuildId` (15)](../../src/main/java/lt/ktu/donendusted/repository/LogEntryRepository.java#L15).
- **Sakyk:** „Tikrinama visa grandinė grupė → užduotis → įrašas viena SQL užklausa.“

### `service/` — logika

**[GuildService.java](../../src/main/java/lt/ktu/donendusted/service/GuildService.java)**
- **Rodyk:** `@Transactional` ant klasės, [`Specification` (34)](../../src/main/java/lt/ktu/donendusted/service/GuildService.java#L34),
  [komentarą apie `save()` (57)](../../src/main/java/lt/ktu/donendusted/service/GuildService.java#L57).
- **Sakyk:** „Kiekvienas metodas — viena transakcija. Prieš kuriant tikrinu, ar pavadinimas laisvas
  (409). Atnaujinant `save()` nekviečiu — Hibernate pats pastebi pakeitimus (*dirty checking*).“

**[QuestService.java](../../src/main/java/lt/ktu/donendusted/service/QuestService.java)**
- **Rodyk:** [`get()` (61)](../../src/main/java/lt/ktu/donendusted/service/QuestService.java#L61),
  [`validate()` (107)](../../src/main/java/lt/ktu/donendusted/service/QuestService.java#L107).
- **Sakyk:** „Jei užduotis nerasta, papildomai tikrinu grupę — klientas sužino tikslią priežastį.
  `validate()` — taisyklė tarp kelių laukų (GRIND ⇔ targetCount). Anotacija prie vieno lauko jos
  neišreikš, todėl tai 422, ne 400.“

**[LogEntryService.java](../../src/main/java/lt/ktu/donendusted/service/LogEntryService.java)**
- **Rodyk:** [`create()` (58)](../../src/main/java/lt/ktu/donendusted/service/LogEntryService.java#L58),
  [`switch` (83)](../../src/main/java/lt/ktu/donendusted/service/LogEntryService.java#L83).
- **Sakyk:** „Naujas įrašas visada PENDING su 0 XP. Data negali būti ateityje, užduotis turi būti
  aktyvi. Tvirtinant be `xpAwarded`, XP imamas iš sudėtingumo. PENDING ir REJECTED negali turėti XP.“

### `web/` — kontroleriai

**[GuildController.java](../../src/main/java/lt/ktu/donendusted/web/GuildController.java)**
- **Rodyk:** `@RequestMapping(produces = JSON)`, [`create()` (82)](../../src/main/java/lt/ktu/donendusted/web/GuildController.java#L82), `delete()` → `noContent()`.
- **Sakyk:** „Kontroleris neturi logikos: priima, paleidžia `@Valid`, kviečia servisą, verčia į DTO.
  POST grąžina `201` ir `Location` antraštę, DELETE — `204` be turinio. `consumes = JSON` duoda
  415, `produces = JSON` — 406.“

**[QuestController.java](../../src/main/java/lt/ktu/donendusted/web/QuestController.java)**, **[LogEntryController.java](../../src/main/java/lt/ktu/donendusted/web/LogEntryController.java)**
- **Rodyk:** kelią `/api/guilds/{guildId}/quests/{questId}/logs`.
- **Sakyk:** „Hierarchiniai keliai — vaikas pasiekiamas tik per tėvą. Filtrai yra užklausos parametrai (`?stat=CON&active=true`).“

**[PagedResponse.java](../../src/main/java/lt/ktu/donendusted/web/PagedResponse.java)**
- **Rodyk:** [`from()` (30)](../../src/main/java/lt/ktu/donendusted/web/PagedResponse.java#L30), `cloneBuilder()`.
- **Sakyk:** „Vienoda puslapiuoto sąrašo struktūra su navigacijos nuorodomis. Nuorodos statomos iš
  dabartinio URL, todėl filtrai išlieka.“

### `web/dto/`

**[GuildRequest.java](../../src/main/java/lt/ktu/donendusted/web/dto/GuildRequest.java)**, **[QuestRequest.java](../../src/main/java/lt/ktu/donendusted/web/dto/QuestRequest.java)**
- **Rodyk:** `@NotBlank @Size(min = 3, max = 60)`.
- **Sakyk:** „Request DTO — ką klientas gali atsiųsti. Apribojimai prie laukų tikrinami dar prieš servisą.“

**[LogEntryCreateRequest.java](../../src/main/java/lt/ktu/donendusted/web/dto/LogEntryCreateRequest.java)**
- **Sakyk (svarbu!):** „Čia **specialiai nėra** nei XP, nei status lauko. Kuriant įrašą klientas
  negali sau prisiskirti taškų — nei per kodą, nei per JSON (nežinomas laukas → 400).“

**[LogEntryUpdateRequest.java](../../src/main/java/lt/ktu/donendusted/web/dto/LogEntryUpdateRequest.java)**
- **Sakyk:** „Vienintelė vieta, kur XP gali būti paduotas — DM tvirtinimo metu. `@PositiveOrZero` neleidžia neigiamo.“

**[GuildResponse.java](../../src/main/java/lt/ktu/donendusted/web/dto/GuildResponse.java)**, **[QuestResponse.java](../../src/main/java/lt/ktu/donendusted/web/dto/QuestResponse.java)**, **[LogEntryResponse.java](../../src/main/java/lt/ktu/donendusted/web/dto/LogEntryResponse.java)**
- **Rodyk:** `static from(...)`, `xpPerCompletion` QuestResponse'e.
- **Sakyk:** „Response DTO — ką klientas mato. Entity niekada negrąžinamas tiesiai. Galiu pridėti
  apskaičiuotų laukų, pvz. `xpPerCompletion`.“

### `error/`

**[ApiExceptionHandler.java](../../src/main/java/lt/ktu/donendusted/error/ApiExceptionHandler.java)**
- **Rodyk:** `@RestControllerAdvice`, kelis `@ExceptionHandler`, paskutinį `Exception.class` ([141](../../src/main/java/lt/ktu/donendusted/error/ApiExceptionHandler.java#L141)).
- **Sakyk:** „Viena vieta, kur bet kuri išimtis virsta `ApiError` atsakymu su teisingu kodu.
  Spring parenka tiksliausią handlerį. `Exception.class` — paskutinė apsauga: grąžina 500 be
  stack trace, o klaida tik įrašoma į log'ą.“

**[ApiError.java](../../src/main/java/lt/ktu/donendusted/error/ApiError.java)**
- **Sakyk:** „Vienodas klaidos formatas: timestamp, status, error, message, path ir `details` —
  kuris laukas blogas. Klientui užtenka vienos klaidų apdorojimo logikos.“

**[ResourceNotFoundException](../../src/main/java/lt/ktu/donendusted/error/ResourceNotFoundException.java)** (404), **[ConflictException](../../src/main/java/lt/ktu/donendusted/error/ConflictException.java)** (409), **[BusinessRuleException](../../src/main/java/lt/ktu/donendusted/error/BusinessRuleException.java)** (422)
- **Sakyk:** „Savos išimtys su prasme. Servisas tik išmeta, o handleris parenka kodą. Servisas
  apie HTTP nieko nežino.“

### Dokumentacija ir testai

- **[docs/openapi.json](../openapi.json)** / Swagger UI — „OpenAPI specifikacija, sugeneruota iš kodo anotacijų.“
- **[Postman kolekcija](../../postman/DoneNDusted.postman_collection.json)** — „31 užklausa, 75
  automatinės patikros. Kolekcija pati susikuria duomenis ir po savęs išsivalo. Pirmas aplankas
  *0. Suvestinė* parodo, kiek DB yra grupių, užduočių ir įrašų (pvz. `grupiu: 3, uzduociu: 9,
  irasu: 12`) su visais ID ir patikrina, ar vaikų `guildId` / `questId` rodo į teisingą tėvą.“
- **[API.md](../API.md)** — paleidimas, metodų lentelė, klaidų kodai.

---

## 6. Tikėtini gynimo klausimai su atsakymais

### REST ir HTTP

**K: Kas yra REST? Kuo tavo API REST'inis?**
A: Resursai turi URI (`/api/guilds/2`), naudojami standartiniai HTTP metodai ir teisingi statuso
kodai, serveris *stateless*, atsakymai JSON. Po POST grąžinamas `201` + `Location`, sąrašuose yra navigacijos nuorodos.
*Paprastai: daiktai turi adresus, veiksmai — HTTP metodai.*

**K: Kuo skiriasi PUT ir PATCH? Kodėl PATCH?**
A: PUT pakeičia **visą** resursą — ko neatsiuntei, tas tampa `null`. Anksčiau su PUT, neatsiuntus
`description`, aprašymas dingdavo iš atsakymo (null laukai į JSON neįrašomi). Todėl atnaujinimas
dabar `PATCH`: keičiami tik atsiųsti laukai, `null` / neatsiųstas reiškia „palik kaip yra“, o
aprašymui išvalyti siunčiamas `""`. Kuriant be `description` išsaugomas `""` (DB: `NOT NULL
DEFAULT ''`, migracija V3), todėl laukas atsakyme **visada** yra.
*Paprastai: PUT — perrašyti visą lapą, PATCH — pataisyti vieną žodį.*

**K: Kas yra idempotentiškumas?**
A: Kartojant užklausą sistemos būsena nesikeičia po pirmo karto. GET, PUT ir DELETE
idempotentiški, POST — ne (kiekvienas sukuria naują resursą). PATCH pagal HTTP nebūtinai
(pvz. „padidink 1“), bet mano PATCH tik nustato reikšmes, todėl kartojant būsena ta pati.

**K: Kodėl POST grąžina 201, o ne 200?**
A: 201 Created konkrečiai reiškia „sukurtas naujas resursas“, o `Location` antraštė pasako jo adresą.

**K: Kodėl DELETE grąžina 204?**
A: 204 No Content — sėkmė, bet atsakymas be turinio, nes ištrinto objekto nebėra ką grąžinti.

**K: Kuo skiriasi 400 ir 422?**
A: 400 — užklausa sugadinta pagal formą (blogas JSON, tuščias laukas, blogas tipas). Tai matosi
žiūrint vien į užklausą. 422 — forma tobula, bet pažeista dalykinė taisyklė (GRIND be
targetCount, data ateityje).
*Paprastai: 400 — „nesuprantu, ką parašei“, 422 — „supratau, bet taip negalima“.*

**K: Kada 409?**
A: Užklausa teisinga, bet prieštarauja esamai būsenai: toks grupės pavadinimas jau užimtas.

**K: Kodėl neteisinga grandinė grąžina 404, o ne 400 ar 403?**
A: `/guilds/1/quests/5`, kai quest 5 priklauso grupei 2 — tokiu adresu resurso tiesiog nėra.
Be to, 404 neatskleidžia, kad toks objektas egzistuoja kitur.

**K: Kada 415 ir 406?**
A: 415 — klientas siunčia ne JSON (`Content-Type: text/plain`). 406 — klientas prašo atsakymo
formato, kurio neturime (`Accept: application/xml`).

**K: Kas būna, jei atsiųsiu nežinomą lauką?**
A: 400, nes įjungtas `fail-on-unknown-properties`. Taip apsaugota nuo, pvz., `xpAwarded`
įsiuntimo kuriant įrašą — parodyk Postman „Klaida 400 – nežinomas laukas“.

**K: Kas yra stateless?**
A: Serveris neatsimena kliento tarp užklausų — kiekvienoje užklausoje yra viskas, ko reikia.
Sesijų nėra. Tai palengvina mastelį (galima paleisti kelis serverius).

### Architektūra ir Spring

**K: Kokie sluoksniai ir kodėl?**
A: Controller (HTTP) → Service (taisyklės, transakcijos) → Repository (DB). Plius DTO ir
ExceptionHandler. Kiekvienas atsakingas už vieną dalyką, todėl lengviau keisti ir testuoti.
*Paprastai: padavėjas — virėjas — sandėlininkas.*

**K: Kas yra Dependency Injection? Kur jį naudoji?**
A: Spring pats sukuria objektus ir paduoda per konstruktorių. Pvz., `GuildController(GuildService guilds)`.
Konstruktoriaus injekcija leidžia laukus padaryti `final` ir lengvai testuoti.

**K: Ką daro `@Transactional`? Kodėl `readOnly = true`?**
A: Visas metodas yra viena DB transakcija, o išmetus RuntimeException — rollback. `readOnly`
skaitymo metoduose leidžia Hibernate netikrinti pakeitimų — greičiau ir saugiau.

**K: Kodėl `update()` nekviečia `save()`?**
A: Objektas gautas toje pačioje transakcijoje, todėl jis „managed“. Hibernate pats pastebi
pakeitimus (*dirty checking*) ir transakcijos pabaigoje įvykdo UPDATE.

**K: Kas yra `Specification`?**
A: Būdas dinamiškai sudėti WHERE sąlygas. Nepaduotas filtras tiesiog nepridedamas, todėl vienas
metodas aptarnauja bet kokį filtrų derinį.

**K: Kaip veikia `findByIdAndGuildId` be SQL?**
A: Spring Data išnagrinėja metodo pavadinimą ir sugeneruoja `WHERE id = ? AND guild_id = ?`.

**K: Kas yra LAZY ir EAGER?**
A: EAGER — susijęs objektas kraunamas iškart. LAZY — tik kai jo prireikia. Naudoju LAZY, kad be reikalo
nekrautume grupės kiekvienai užduočiai. `open-in-view: false` užtikrina, kad DB sesija nepalieka serviso ribų.

**K: Kodėl `record` DTO'ams?**
A: Nekintami, trumpi, `equals`/`toString` sugeneruojami. Tinka duomenims perduoti.

**K: Kaip veikia klaidų apdorojimas?**
A: Servisas išmeta savo išimtį (pvz., `ConflictException`). `@RestControllerAdvice` ją pagauna ir
paverčia `ApiError` su teisingu kodu. Kontroleriuose try/catch nėra.

### Duomenų bazė

**K: Kodėl PostgreSQL? Kodėl ne H2?**
A: Taip numatyta LD0. Naudoju tikrą PostgreSQL net dev režimu, kad elgsena (TIMESTAMPTZ, CHECK,
sekos) būtų tokia pati kaip produkcijoje.

**K: Kas yra Flyway? Kodėl `ddl-auto: validate`, o ne `update`?**
A: Flyway paleidžia versijuotus SQL failus, schema kontroliuojama ir atkartojama. `validate`
reiškia, kad Hibernate schemos nekeičia — tik patikrina, ar entity atitinka lenteles. `update`
galėtų netikėtai pakeisti produkcinę DB.

**K: Kas nutinka ištrynus grupę?**
A: DB išorinis raktas turi `ON DELETE CASCADE` — kartu ištrinamos užduotys ir jų įrašai.

**K: Kaip apsisaugai nuo dviejų vienodų pavadinimų, jei dvi užklausos ateina vienu metu?**
A: Servisas tikrina `existsBy…`, bet tarp patikros ir įrašymo gali įsiterpti kita užklausa
(*race condition*). Todėl DB turi `UNIQUE` apribojimą, o `DataIntegrityViolationException`
handleris grąžina 409. DB yra paskutinė apsauga.

**K: Kodėl taisyklės ir Java, ir SQL CHECK?**
A: Gynyba keliais sluoksniais (*defense in depth*). Net jei kas nors įrašytų tiesiai į DB arba
kode būtų klaida, PENDING įrašas negalės turėti XP.

**K: Kodėl `Instant` ir UTC?**
A: Laikas saugomas be laiko juostos dviprasmybių (TIMESTAMPTZ + UTC). Konvertuoti į vietinį laiką — kliento reikalas.

### Saugumas

**K: Ar API saugus? Kas gali ištrinti grupę?**
A: LD1 autentifikacija dar neimplementuota, taip numatyta užduotyje. LD2 bus JWT ir rolės.
Bet jau dabar yra validacija, nežinomų laukų atmetimas, XP negalima įsiųsti, stack trace klientui nerodomas.

**K: Enumeration attack?**
A: Žr. [3.4](#34-enumeration-attack-ant-guildid--ar-reikia-uuid-dabar-vėliau-ar-visai-ne).

**K: SQL injection?**
A: Neįmanoma, nes SQL ranka nekonstruoju. Spring Data / Hibernate naudoja parametrizuotas užklausas.

**K: Ką daryti, jei kas nors paprašys `?size=1000000`?**
A: `max-page-size: 100` apkarpys iki 100.

### Procesas ir įrankiai

**K: Kaip testavai?**
A: Postman kolekcija su 57 automatinėmis patikromis (statuso kodai, laukai, antraštės). Ji paleidžiama
viena komanda per newman. JUnit testų kol kas nėra — tai sekantis žingsnis (žr. 7 skyrių).

**K: Kas yra `mvnw`?**
A: Maven Wrapper — atsisiunčia reikiamą Maven versiją, todėl jo nereikia diegti.

**K: Kaip paleisti projektą?**
A: Reikia tik Java 21: `./mvnw spring-boot:run`. Swagger: `http://localhost:8080/swagger-ui.html`.

**K: Kas yra Swagger / OpenAPI ir kaip jis sugeneruotas?**
A: springdoc skaito kontrolerius, DTO ir `@Operation`/`@Schema` anotacijas ir sugeneruoja
OpenAPI 3.1 specifikaciją bei interaktyvų UI.

---

## 7. Silpnos vietos ir kaip apie jas kalbėti

Geriau pačiai pasakyti, nei būti „pagautai“. Formulė: **„Žinau → kodėl dabar taip → kaip pataisyčiau.“**

| Silpna vieta | Ką sakyti |
| --- | --- |
| Nėra autentifikacijos / rolių | „LD1 apimtis — REST sluoksnis. JWT ir rolės — LD2. Struktūra paruošta: pridėsiu Spring Security filtrą, kontrolerių keisti nereikės.“ |
| Nėra JUnit testų | „Testuota Postman kolekcija su 75 patikromis. Kitas žingsnis — `@SpringBootTest` + MockMvc integraciniai testai.“ |
| Nuoseklūs ID | žr. 3.4 |
| PATCH: `null` = „nesiųsta“ | „Record DTO negali atskirti neatsiųsto lauko nuo `null`, todėl aprašymui išvalyti siunčiamas `\"\"`. Tikslesnis variantas — JSON Merge Patch (`application/merge-patch+json`).“ |
| Dev DB duomenys dingsta | „Specialiai — kiekviena demonstracija prasideda nuo švarių duomenų. Nuolatinei DB — `prod` profilis.“ |
| Nėra Dockerfile | žr. 3.8 |

---

## 8. Demonstracijos eiga

Rekomenduojama tvarka (~5–7 min.). Komandos — [DEMO_KOMANDOS.txt](DEMO_KOMANDOS.txt).

1. **Paleidimas:** `./mvnw spring-boot:run` → parodyk `Started DoneNDustedApplication` ir kad
   Flyway pritaikė V1, V2.
2. **Swagger UI** (`/swagger-ui.html`): 3 grupės po 5 metodus, atsidaryk vieną ir parodyk klaidų
   kodus bei schemas.
3. **Postman Runner:** paleisk visą kolekciją → 57/57 žalių. Tai greičiausias įrodymas, kad viskas veikia.
4. **Rankiniai pavyzdžiai** (jei paprašys):
   - `GET /api/guilds?page=0&size=2&sort=name,desc` → nuorodos `next`, `last`;
   - `POST` grupė → `201` + `Location`;
   - pasikartojantis pavadinimas → `409`;
   - GRIND be targetCount → `422`;
   - užduotis per svetimą grupę → `404`;
   - įrašo kūrimas su `xpAwarded` → `400`;
   - patvirtinimas be `xpAwarded` → serveris paskiria XP iš sudėtingumo.
5. **Kodas:** eik pagal vienos užklausos kelionę (4 skyrius): Controller → Service → Repository →
   Entity → DTO → ExceptionHandler.
6. **DB (jei paklaus):** DBeaver, `localhost:55432`, user `postgres` — parodyk lenteles ir CHECK apribojimus.

**Paskutinis patarimas:** jei nežinai atsakymo, sakyk *„Tiksliai neprisimenu, bet logika tokia…“*
ir paaiškink savais žodžiais pagal restorano palyginimą. Dėstytojams svarbiau, kad supranti, nei kad žinai terminą mintinai.
