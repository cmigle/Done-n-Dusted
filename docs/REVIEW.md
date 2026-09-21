# LD1 — patikros ataskaita

Kiekviena šaka po sujungimo į `main` buvo peržiūrėta griežtai pagal užduoties reikalavimų sąrašą.
Šiame dokumente surašyta, kas buvo tikrinama, kas neatitiko reikalavimų ir kas dėl to pakeista.
Ataskaita rašoma po kiekvieno peržiūros ciklo; visi žemiau esantys taisymai jau atlikti šakoje
`chore/review-fixes`.

---

## 1. Reikalavimų atitikties suvestinė

| # | Reikalavimas | Būsena | Įrodymas |
| --- | --- | --- | --- |
| 1 | Realizuoti **visi** užduotyje numatyti API metodai | ✅ | 15 operacijų, žr. 2 skyrių |
| 2 | OpenAPI specifikacija **kiekvienam** metodui | ✅ | `docs/openapi.json` — 6 keliai, 15 operacijų |
| 3 | Aplinka, leidžianti patogiai paleisti ir demonstruoti | ✅ | `./mvnw spring-boot:run`, reikia tik Java 21 |
| 4 | Panaudotas pasirinktas DB sprendimas | ✅ | tikras PostgreSQL 18 procesas, ne pakaitalas |
| 5 | DB užpildyta prasmingais duomenimis | ✅ | `V2__seed_data.sql`: 3 grupės, 9 užduotys, 12 įrašų |
| 6 | Teisingas turinys, turinio tipas ir atsako kodas | ✅ | `application/json`, kodai 200/201/204/400/404/405/406/409/415/422 |
| 7 | Demonstracija iki ~15 s, visi 15 metodų | ✅ | Postman/newman: 28 užklausos, 57 patikros, **4,9 s** |
| 8 | 404, kai resursas nerandamas | ✅ | kolekcijos užklausos 08, 13, 22, 26 |
| 9 | 400/422 blogam payload, priklausomai nuo situacijos | ✅ | 07, 07a, 07b, 10, 16, 20 |
| 10 | 201 sukūrus resursą | ✅ | 01, 02, 09, 15 — su `Location` antrašte |
| 11 | 200/204 ištrynus, priklausomai nuo kūno | ✅ | 21, 23, 24, 25 — `204`, kūnas tuščias |
| 12 | Kodas Git saugykloje | ⚠️ | žr. 4 skyrių — laukiama naudotojo veiksmo |
| 13 | Dokumentacija saugykloje (`README`) | ✅ | `README.md` + `docs/WALKTHROUGH.md` |

---

## 2. Metodų inventorizacija

Sugeneruota iš `docs/openapi.json`, ne iš atminties:

```
paths: 6   operations: 15
  GET,POST        /api/guilds
  GET,PUT,DELETE  /api/guilds/{guildId}
  GET,POST        /api/guilds/{guildId}/quests
  GET,PUT,DELETE  /api/guilds/{guildId}/quests/{questId}
  GET,POST        /api/guilds/{guildId}/quests/{questId}/logs
  GET,PUT,DELETE  /api/guilds/{guildId}/quests/{questId}/logs/{logId}
```

Nei vienos operacijos per mažai, nei vienos per daug.

---

## 3. Rasti trūkumai ir jų taisymai

Peržiūra buvo atliekama ne skaitant kodą, o siunčiant užklausas, kurių kolekcija dar netikrino.
Rasti **penki** trūkumai; visi ištaisyti.

### 3.1 `?sort=bogus` grąžino 500 vietoj 400 — **ištaisyta**

Rikiavimas pagal neegzistuojantį lauką kėlė `PropertyReferenceException`, kurią pagaudavo tik
bendrasis `Exception` tvarkytojas. Klaida yra kliento, ne serverio, todėl 500 buvo neteisingas
kodas.

Pridėtas atskiras tvarkytojas, grąžinantis **400** su pranešimu
`Rikiuoti pagal lauka 'bogus' negalima - tokio lauko nera`.

Pastaba ateičiai: Spring Data 4 šią klasę perkėlė į `org.springframework.data.core`
(anksčiau `org.springframework.data.mapping`).

### 3.2 `Content-Type: text/plain` grąžino 500 vietoj 415 — **ištaisyta**

`HttpMediaTypeNotSupportedException` nebuvo tvarkoma, todėl netinkamu formatu atsiųstas kūnas
atrodė kaip serverio gedimas. Pridėtas tvarkytojas → **415 Unsupported Media Type** su `ApiError`
struktūra. Kartu pridėtas ir `HttpMediaTypeNotAcceptableException` → **406**.

### 3.3 Nedeklaruoti laukai buvo tyliai ignoruojami — **ištaisyta**

Pagal nutylėjimą Jackson praleidžia užklausoje esančius nežinomus laukus. Tai reiškė, kad
`POST .../logs` su `"xpAwarded": 500` grąžindavo `201`, o XP tyliai dingdavo.

Rezultatas buvo teisingas, bet **tyla čia netinka**: ataskaitoje aiškiai pasakyta, kad XP iš
kliento nepriimamas, todėl serveris turi tai pasakyti garsiai. Įjungta
`spring.jackson.deserialization.fail-on-unknown-properties: true`, ir tokia užklausa dabar gauna
**400** su pranešimu, nurodančiu, kuris laukas nepriimtinas.

### 3.4 Paleidžiamas `jar` failas svėrė 188 MB — **ištaisyta**

Į `jar` patekdavo `dev` profiliui skirtos PostgreSQL binarinės bibliotekos — įskaitant macOS ir
Linux versijas, kurių paleidžiamam artefaktui nereikia visai.

`spring-boot-maven-plugin` `repackage` tikslui pridėtas
`excludeGroupIds=io.zonky.test,io.zonky.test.postgres`. Dydis: **188 MB → 61 MB**.

Svarbi detalė: konfigūracija pririšta **tik prie `repackage` tikslo**. Įrašius ją į bendrą
`<configuration>`, ji pritaikoma ir `spring-boot:run` tikslui, ir `dev` profilis nustoja veikti su
`NoClassDefFoundError`. Tai buvo patikrinta praktiškai — abu paleidimo būdai dabar veikia:
`./mvnw spring-boot:run` pakelia įterptąjį PostgreSQL, o `java -jar` su `prod` profiliu jungiasi
prie išorinės duomenų bazės ir jokių trūkstamų klasių nebeieško.

### 3.5 `<scope>provided</scope>` nebuvo tinkamas sprendimas — **atmesta**

Pirmas bandymas sumažinti `jar` buvo pažymėti priklausomybę `provided`. Spring Boot 4
`spring-boot:run` tikslas `provided` srities priklausomybių **neįtraukia**, todėl `dev` profilis
nustojo veikti. Grąžinta numatytoji sritis, o atskyrimas padarytas 3.4 punkte aprašytu būdu.
Įrašoma čia, kad klaida nebūtų pakartota.

---

## 4. Likęs neatliktas punktas: Git saugykla

**Reikalinga naudotojo veiksmo.**

Pirmoji saugykla `github.com/cmigle/Done-n-Dusted` buvo sukurta su commit'ais, kurių autoriaus
el. paštas buvo asmeninis, o ne GitHub paskyros. Vietinė istorija perkurta nuo nulio — visi
commit'ai dabar yra
`cmigle <182432759+cmigle@users.noreply.github.com>` — bet **senoji saugykla dar neištrinta**,
nes turimam `gh` prieigos raktui trūksta `delete_repo` teisės.

Reikia atlikti vieną iš dviejų veiksmų:

```bash
gh auth refresh -h github.com -s delete_repo     # interaktyvus, po to saugykla ištrinama automatiškai
```

arba ištrinti `Done-n-Dusted` saugyklą per GitHub svetainę (*Settings → Danger Zone*).

Po to perkurta istorija su visomis šakomis bus įkelta.

---

## 5. Ką peržiūra patvirtino kaip tvarkingą

Šie dalykai buvo tikrinami atskirai ir **problemų nerasta**:

* **Tėvinė grandinė.** `GET /api/guilds/1/quests/4`, kai užduotis 4 priklauso grupei 2, grąžina
  `404`, o ne svetimos grupės duomenis. Taisyklė užtikrinta tuo, kad `QuestRepository` iš viso
  neturi `findById` metodo — yra tik `findByIdAndGuildId`.
* **Neegzistuojančios grupės užduočių sąrašas** grąžina `404`, o ne tuščią sąrašą.
* **Puslapiavimo nuorodos** išlaiko visus filtrus ir rikiavimą, nes statomos iš tikrosios
  užklausos URI; `prev`/`next` nepateikiami, kai tokio puslapio nėra.
* **`?size=99999`** apribojamas iki 100 (`max-page-size`), o ne bandoma serializuoti visą lentelę.
* **Numatytasis rikiavimas** nustatytas kiekvienam sąrašui, todėl puslapiai nesikartoja.
* **XP kilmė.** Patvirtinus įrašą be `xpAwarded`, XP paimamas iš užduoties sudėtingumo
  (`LEGENDARY` → 80). Patvirtintas XP lieka užšaldytas pakeitus užduotį.
* **`PENDING` ir `REJECTED` be taškų** — taisyklė galioja ir per API (422), ir duomenų bazėje
  (`CHECK` apribojimai), todėl jos neįmanoma apeiti nei per klaidą kode, nei rašant tiesiai į DB.
* **`DELETE` kaskada** veikia per duomenų bazės `ON DELETE CASCADE`, be našlaičių įrašų.
* **Pakartotinis kolekcijos paleidimas** neduoda klaidų: duomenys kuriami su laiko žyma
  pavadinime ir ištrinami pabaigoje.

---

## 6. Sąmoningai neįgyvendinta (ne trūkumas)

Autentifikacija, rolės, `User`, `GuildMembership`, `Friendship`, veikėjo kortelė ir React kliento
pusė nėra LD1 apimtis. Visos likusios LD0 ataskaitos taisyklės yra autorizacijos taisyklės („kas
turi teisę tai daryti“), todėl jos ateina kartu su JWT kitame darbe. Pagrindimas —
`docs/WALKTHROUGH.md`, skyrius 2.2.
