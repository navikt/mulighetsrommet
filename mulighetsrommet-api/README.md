# `mulighetsrommet-api`

<p>
Et API med endepunkter for å hente ut informasjon om forskjellige tiltak NAV kan tilby brukere.
</p>

# Innhold

- [`mulighetsrommet-api`](#mulighetsrommet-api)
- [Innhold](#innhold)
- [Teknologier](#teknologier)
- [Overvåking og alarmer](#overvåking-og-alarmer)
- [Kom i gang](#kom-i-gang)
  - [Forutsetninger](#forutsetninger)
    - [Miljøvariabler](#miljøvariabler)
  - [Steg for steg](#steg-for-steg)
    - [Databasemigrasjoner](#databasemigrasjoner)
    - [Autentisering](#autentisering)
    - [Feature toggles](#feature-toggles)
  - [Automatiske jobber](#automatiske-jobber)
    - [Oppdatere enheter fra NORG til Sanity](#oppdatere-enheter-fra-norg-til-sanity)
- [Strukturert innhold](#strukturert-innhold)
- [Datadeling til Datamarkedsplassen med Datastream](#datadeling-til-datamarkedsplassen-med-datastream)
    - [Ang. datastream](#ang-datastream)
    - [Tilganger for servicebruker (SA-bruker)](#tilganger-for-servicebruker-sa-bruker)

# <a name="teknologier"></a>Teknologier

Øvrige teknologier, rammeverk og biblioteker som er blitt tatt i bruk:

- [**Kotlin**](https://kotlinlang.org/)
- [**Ktor**](https://ktor.io/)
- [**PostgreSQL**](https://www.postgresql.org/)
- [**MocKK**](https://mockk.io/)
- [**Flyway**](https://flywaydb.org/)
- [**Gradle**](https://gradle.org/)

# <a name="overvaking"></a>Overvåking og alarmer

Under `./alerts-api.yaml` ligger det alarmer definert. Disse blir deployet automatisk ved endringer via egen Github
workflow som man finner under `../.github/workflows/alert-deploy-api.yaml`.

[Her kan man se en oversikt over alarmene](https://prometheus.dev-gcp.nais.io/alerts?search=mulighetsr) som er definert.

[Grafana kan benyttes for å se metrikker for kjørende applikasjoner](https://grafana.nais.io/d/8W2DNq6nk/mulighetsrommet-api?orgId=1&var-datasource=prod-gcp&var-duration=15m&var-team=team-mulighetsrommet&from=now-15m&to=now)

# <a name="kom-i-gang"></a>Kom i gang

For å komme i gang kan du hoppe rett til [Steg for steg](#steg-for-steg) gitt at du har satt opp alt
under [Forutsetinger](#forutsetninger).

## <a name="forutsetninger"></a>Forutsetninger

Sørg for at du har fulgt oppsettet beskrevet i prosjektets [README](../README.md#oppsett).

Under er det satt opp et par ting som må på plass for at applikasjonen og databasen skal fungere.

### Miljøvariabler

Disse miljøvariablene må være konfigurert og tilgjengelige for prosessen som skal kjøre applikasjonen:

```sh
export DB_HOST=localhost
export DB_PORT=5442
export DB_DATABASE=mulighetsrommet-api-db
export DB_USERNAME=valp
export DB_PASSWORD=valp
export DB_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_DATABASE}
```

Benytt et et verktøy som f.eks. [direnv](https://direnv.net/), konfigurerer de som
miljøvariabler under en
egen [Run Configuration i IntelliJ](https://www.jetbrains.com/idea/guide/tutorials/hello-world/creating-a-run-configuration/),
eller legg de direkte til i setup for shell'et du bruker (f.eks. `.bashrc` eller `.zshrc`) om du foretrekker dette.

## <a name="steg-for-steg"></a>Steg for steg

For å komme fort i gang fra en terminal gjør følgende (antar at du står i rot av prosjektet):

1. Sørg for at du kjører avhengigheter (database, wiremock etc.) som beskrevet i oppsettet
   til [Docker](../README.md#docker).
2. Hent avhengigheter og installer applikasjonen lokalt med `./gradlew mulighetsrommet-api:build`.
3. Start applikasjonen med å kjøre `./gradlew mulighetsrommet-api:run`.

Hvis alt gikk knirkefritt skal nå applikasjonen kjøre på <http://0.0.0.0:8080>.

### Databasemigrasjoner

Databasemigrasjoner administreres via Flyway og prosjektet inkluderer en
[Gradle plugin](https://plugins.gradle.org/plugin/org.flywaydb.flyway) for å gjøre det enklere å teste
databaseendringer med Flyway lokalt. Hvis du ønsker å benytte deg av dette må du overstyre noen Gradle-instillinger
pga. manglende support for [configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html) i
Flyway sin Gradle plugin.

Opprett filen `~/.gradle/gradle.properties` med følgende innhold for å komme deg rundt dette problemet:

```
# Overstyrt lokalt fordi flyway ikke funker med configuration-cache
org.gradle.configuration-cache=false
```

### Autentisering

For å kalle APIet lokalt må man være autentisert med et Bearer token.

Vi benytter [mock-ouath2-server](https://github.com/navikt/mock-oauth2-server) til å utstede tokens på lokal maskin.
Følgende steg kan benyttes til å generere opp et token:

1. Sørg for at containeren for `mock-oauth2-server` kjører lokalt (se [oppsett for Docker](../README.md#docker)).
2. Naviger til [mock-oauth2-server sin side for debugging av tokens](http://localhost:8081/azure/debugger).
3. Generer et token
    1. Trykk på knappen `Get a token`
    2. Skriv inn et random username og følgende JSON payload i `optional claims` (`NAVident` og `groups` claims matcher med annen mock-data):
       ```json
       {
         "NAVident": "B123456",
         "roles": [
           "access_as_application"
         ],
         "oid": "0bab029e-e84e-4842-8a27-d153b29782cf",
         "azp_name": "test name",
         "groups": [
           "52bb9196-b071-4cc7-9472-be4942d33c4b",
           "48026f54-6259-4c35-a148-bc4257bcaf03",
           "279039a0-39fd-4860-afdd-a1a2ccaa6323"
         ]
       }
       ```
    3. Trykk `Sign in`
4. Kopier verdien for `access_token` og benytt denne som `Bearer` i `Authorization`-header i `.env`-filen du har
   opprettet
   i `/mr-admin-flate`

Eksempel:

```sh
$ curl localhost:8080/api/v1/innsatsgrupper -H 'Authorization: Bearer <access_token>'
```

### Feature toggles

Vi administrerer en del feature toggles via [NAIS og Unleash](https://doc.nais.io/addons/unleash/).
Grensesnitt for å definere toggles finner du her: https://team-mulighetsrommet-unleash-web.nav.cloud.nais.io (logg inn
med @nav-brukeren din).

Standard oppsett er at Unleash blir [mocket lokalt](../README.md#mocks-via-wiremock), så husk gjerne å oppdatere mocken
med nye feature toggles etter hvert som de legges til.
Hvis du heller ønsker å peke lokal applikasjon direkte mot Unleash kan du gjøre følgende:

1. Konfigurer miljøvariabelen `UNLEASH_SERVER_API_URL` med riktig
   URL: https://team-mulighetsrommet-unleash-api.nav.cloud.nais.io
2. Opprett et
   personlig [Unleash token](https://team-mulighetsrommet-unleash-web.nav.cloud.nais.io/profile/personal-api-tokens)
   og konfigurerer miljøvariabelen `UNLEASH_SERVER_API_TOKEN` med dette tokenet
3. Start applikasjonen

## Automatiske jobber

Vi har flere automatiske jobber som kjører til gitte intervaller. Disse finner man under mappen `/tasks`. Jobbene
bruker `db-scheduler` for å vite når de skal kjøre.

### Oppdatere enheter fra NORG til Sanity

Jobben `synchronize-norg2-enheter-to-sanity` brukes for å oppdatere enheter i Sanity med fasit fra NORG2.
Jobben benytter seg av tokenet `Token for task: synchronize-norg2-enheter-to-sanity fra api (PROD | TEST)` som har
create/update-tilgang til enheter-dokumentene i Sanity.

# <a name="strukturert-innhold"></a>Strukturert innhold
Vi deler strukturert innhold om tiltakstyper via Kafka.
Innholdet lever i tabeller i databasen og kan oppdateres via filen `R__deltakerregistrering-innhold.sql`. Gjør de endringene som må til og deploy så vil migrasjonsfilen kjøres på nytt mot databasene i dev og prod.

# Datadeling til Datamarkedsplassen med Datastream
Databasen til api er satt opp med replikasjon av tabeller for tiltakstyper og tiltaksgjennomføringer.
Vi har fulgt guiden her https://github.com/navikt/nada-datastream for oppsett.

Datastream i dev: https://console.cloud.google.com/datastream/streams?authuser=1&project=team-mulighetsrommet-dev-a2d7
Datastream i prod: https://console.cloud.google.com/datastream/streams?authuser=1&project=team-mulighetsrommet-prod-5492

BigQuery i dev: https://console.cloud.google.com/bigquery?authuser=1&project=team-mulighetsrommet-dev-a2d7&ws=!1m0
BigQuery i prod: https://console.cloud.google.com/bigquery?authuser=1&project=team-mulighetsrommet-prod-5492&ws=!1m0

### Views
Vi har views til BigQuery definert i iac/bigquery-views.
Du kan gjøre endringer i view'ene og så vil de bygges på nytt når du deployer koden via Github Actions.

### Ang. datastream
Dersom datastreamen tuller seg eller man vil restarte prosessen så må man gå til datastreamen (lenke over) og så slette datastreamen.
[Kjør så datastream på nytt basert på guiden her ](https://github.com/navikt/nada-datastream)

### Tilganger for servicebruker (SA-bruker)
I dev og prod har vi servicebrukere som trenger tilgang til BigQuery for å opprette ressurser (feks. views). Disse har tilgang til `BigQuery Data Editor`-rollen. Den rollen trengs for å opprette og oppdatere views.
