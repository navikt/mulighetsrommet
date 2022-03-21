# `mulighetsrommet-api`

<p>
Et API med endepunkter for å hente ut informasjon om forskjellige tiltak NAV kan tilby brukere.
</p>
# TrIGGER

# Innhold

- [Teknologier](#teknologier)
- [Kom i gang](#kom-i-gang)
  - [Forutsetninger](#forutsetninger)
  - [Database](#database)
  - [Steg for steg](#steg-for-steg)
- [Integrasjoner](#integrasjoner)

# <a name="teknologier"></a>Teknologier

Øvrige teknologier, rammeverk og biblioteker som er blitt tatt i bruk:

[**Kotlin**](https://kotlinlang.org/)

[**Ktor**](https://ktor.io/)

[**PostgreSQL 12**](https://www.postgresql.org/)

[**MocKK**](https://mockk.io/)

[**Exposed**](https://github.com/JetBrains/Exposed)

[**Flyway**](https://flywaydb.org/)

[**Gradle**](https://gradle.org/)

# <a name="kom-i-gang"></a>Kom i gang

For å komme i gang kan du hoppe rett til [Steg for steg](#steg-for-steg) gitt at du har satt opp alt
under [Forutsetinger](#forutsetninger).

## <a name="forutsetninger"></a>Forutsetninger

Under er det satt opp et par ting som må på plass for at applikasjonen og databasen skal fungere.

### JDK 11

JDK 11 må være installert. Enkleste måten å installere riktig versjon av Java er ved å
bruke [sdkman](https://sdkman.io/install).

### Docker

Vår database kjører i en docker container og da må `docker` og `docker-compose` være installert (Captian Obvious). For å
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
export KAFKA_PORT=10002
export KAFKA_HOST=localhost
```

Legg til disse enten i `.bashrc` eller `.zshrc` eller kjør dem per session rett i terminalen.

## <a name="database"></a>Database

For å få opp databasen må man ha installert `docker-compose` på forhånd, se [Forutsetinger](#forutsetninger).

Man kjører enkelt opp en lokal instans av databasen ved å kjøre `docker-compose up` i en terminal.

For å avslutte kan man trykke `Ctrl+C` eller skrive `docker-compose down`.

## <a name="steg-for-steg"></a>Steg for steg

For å komme fort i gang fra en terminal gjør følgende:

1. Fyr opp databasen med å kjøre `docker-compose up` i terminalen.
2. Hent avhengigheter og installer applikasjonen lokalt med `./gradlew install`.
3. Migrer endringer og data til databasen ved å kjøre `./gradlew flywayMigrate`. (For å slette databasen og migrere alt
   på nytt kan man kjøre `./gradlew flywayClean` før migrate)
4. Start applikasjonen med å kjøre `./gradlew run`.

Hvis alt gikk knirkefritt skal nå applikasjonen kjøre på http://0.0.0.0:8080.

# Integrasjoner

Når vi har noe vi integrerer mot eller integrerer med, så vil det stå her...
