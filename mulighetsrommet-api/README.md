# `mulighetsrommet-api`

<p>
Et API med endepunkter for å hente ut informasjon om forskjellige tiltak NAV kan tilby brukere.
</p>

# Innhold

- [`mulighetsrommet-api`](#mulighetsrommet-api)
- [Innhold](#innhold)
- [<a name="teknologier"></a>Teknologier](#teknologier)
- [<a name="kom-i-gang"></a>Kom i gang](#kom-i-gang)
  - [<a name="forutsetninger"></a>Forutsetninger](#forutsetninger)
    - [JDK 11](#jdk-11)
    - [Docker](#docker)
    - [Miljøvariabler](#miljøvariabler)
  - [<a name="steg-for-steg"></a>Steg for steg](#steg-for-steg)
    - [Autentisering](#autentisering)

# <a name="teknologier"></a>Teknologier

Øvrige teknologier, rammeverk og biblioteker som er blitt tatt i bruk:

- [**Kotlin**](https://kotlinlang.org/)
- [**Ktor**](https://ktor.io/)
- [**PostgreSQL**](https://www.postgresql.org/)
- [**MocKK**](https://mockk.io/)
- [**Flyway**](https://flywaydb.org/)
- [**Gradle**](https://gradle.org/)

# <a name="kom-i-gang"></a>Kom i gang

For å komme i gang kan du hoppe rett til [Steg for steg](#steg-for-steg) gitt at du har satt opp alt
under [Forutsetinger](#forutsetninger).

## <a name="forutsetninger"></a>Forutsetninger

Under er det satt opp et par ting som må på plass for at applikasjonen og databasen skal fungere.

### JDK 11

JDK 11 må være installert. Enkleste måten å installere riktig versjon av Java er ved å
bruke [sdkman](https://sdkman.io/install).

### Docker

Vår database kjører i en docker container og da må `docker` og `docker-compose` være installert (Captain Obvious). For å
installere disse kan du følge oppskriften på [Dockers](https://www.docker.com/) offisielle side. For installering på Mac
trykk [her](https://docs.docker.com/desktop/mac/install/) eller
trykk [her](https://docs.docker.com/engine/install/ubuntu/) for Ubuntu.

Man må også installere `docker-compose` som en separat greie
for [Ubuntu](https://docs.docker.com/compose/install/#install-compose-on-linux-systems). For Mac følger dette med når
man installerer Docker Desktop.

### Miljøvariabler

Disse miljøvariablene må være satt opp:

```sh
export DB_HOST=localhost
export DB_PORT=5442
export DB_DATABASE=mulighetsrommet-db
export DB_USERNAME=valp
export DB_PASSWORD=valp
export DB_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_DATABASE}
```

Legg til disse enten i `.bashrc` eller `.zshrc` eller kjør dem per session rett i terminalen. Eller bruk et verktøy som [direnv](https://direnv.net/).

## <a name="steg-for-steg"></a>Steg for steg

For å komme fort i gang fra en terminal gjør følgende:

1. Fyr opp avhengigheter (database etc.) med å kjøre `docker-compose --profile dev -d up` i terminalen. For å få med mock data for enhet og innsatsgruppe kan man kjøre `docker-compose --profile dev --profile wiremock up` i stedet for å også kjøre opp en wiremock instans.
2. Hent avhengigheter og installer applikasjonen lokalt med `./gradlew install`.
3. Migrer endringer og data til databasen ved å kjøre `./gradlew flywayMigrate`. (For å slette databasen og migrere alt
   på nytt kan man kjøre `./gradlew flywayClean` før migrate)
4. Start applikasjonen med å kjøre `./gradlew run`.

Hvis alt gikk knirkefritt skal nå applikasjonen kjøre på <http://0.0.0.0:8080>.

### Autentisering

For å kalle APIet lokalt må man være autentisert med et Bearer token.

Vi benytter [mock-ouath2-server](https://github.com/navikt/mock-oauth2-server) til å utstede tokens på lokal maskin.
Følgende steg kan benyttes til å generere opp et token:

1. Sørg for at containeren for `mock-oauth2-server` kjører lokalt (`docker-compose up --profile dev -d`)
2. Naviger til [mock-oauth2-server sin side for debugging av tokens](http://localhost:8081/azure/debugger)
3. Trykk på knappen `Get a token`
4. Full ut felt for `user/subject` (spiller ingen trille hva dette er) og trykk deretter `Sign in`
5. Kopier verdien for `access_token` og benytt denne som `Bearer` i `Authorization`-header

Eksempel:
```sh
$ curl localhost:8080/api/v1/innsatsgrupper -H 'Authorization: Bearer <access_token>'
```
