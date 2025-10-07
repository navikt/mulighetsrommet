# `mulighetsrommet-api`

<p>
Et API med endepunkter for å hente ut informasjon om forskjellige tiltak Nav kan tilby brukere.
</p>

# Innhold

- [`mulighetsrommet-api`](#mulighetsrommet-api)
- [Innhold](#innhold)
- [Teknologier](#teknologier)
- [Overvåking og alarmer](#overvåking-og-alarmer)
- [Kom i gang](#kom-i-gang)
    - [Forutsetninger](#forutsetninger)
    - [Steg for steg](#steg-for-steg)
        - [Databasemigrasjoner](#databasemigrasjoner)
        - [Autentisering](#autentisering)
        - [Feature toggles](#feature-toggles)
    - [Automatiske jobber](#automatiske-jobber)
        - [Oppdatere enheter fra NORG til Sanity](#oppdatere-enheter-fra-norg-til-sanity)
- [Strukturert innhold](#strukturert-innhold)

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

## <a name="steg-for-steg"></a>Steg for steg

For å komme fort i gang fra en terminal gjør følgende (antar at du står i rot av prosjektet):

1. Sørg for at du kjører avhengigheter (database, wiremock etc.) som beskrevet i oppsettet
   til [Docker](../README.md#docker).
2. Hent avhengigheter og installer applikasjonen lokalt med `./gradlew mulighetsrommet-api:build`.
3. Start applikasjonen med å kjøre `./gradlew mulighetsrommet-api:run`.

Hvis alt gikk knirkefritt skal nå applikasjonen kjøre på <http://0.0.0.0:8080>.

## Databasemigrasjoner

Databasemigrasjoner administreres via Flyway. Siden flyway sin gradle plugin ikke
støtter [configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html) så er det ikke mulig å
benytte denne lokalt.

## OpenAPI

Det blir generert OpenAPI-dokumentasjon via [ktor-openapi-tools](https://github.com/SMILEY4/ktor-openapi-tools), som
igjen blir benyttet til å generere klienter for frontend-applikasjonene.

Hvis du gjør endringer i modeller eller endepunkter som er eksponert via OpenAPI må du også huske å generere ny spec og
sjekke dette inn i git. Dette kan gjøres ved å kjøre følgende kommando:

```bash
gradle generateOpenApi
```

Det er også satt opp en egen workflow i Github Actions som skal sørge for at man ikke glemmer å generere skjemaet.

### Eksterne konsumenter

- Swagger UI er tilgjengelig på `<app ingress>/swagger-ui`.
- Og selve OpenAPI-dokumentasjonen finner du også [definert her](./src/main/resouces/web/openapi-public.yaml).

## Autentisering

For å kalle APIet lokalt må man være autentisert med et Bearer token. Vi
benytter [mock-ouath2-server](https://github.com/navikt/mock-oauth2-server) til å utstede tokens på lokal maskin.
Påkrevd innhold i tokenet varierer basert på hvilket endepunkt man ønsker å kalle (f.eks. som en Nav-ansatt, som en
ekstern bruker, eller som en Nais-app).

Det er beskrevet hvordan hvordan man skaffer seg et token for lokal utvikling i README per frontend-app.

## Feature toggles

Vi administrerer en del feature toggles via [NAIS og Unleash](https://doc.nais.io/addons/unleash/).
Grensesnitt for å definere toggles finner du her: https://team-mulighetsrommet-unleash-web.iap.nav.cloud.nais.io
(logg inn med @nav-brukeren din).

Standard oppsett er at Unleash blir [mocket lokalt](../README.md#mocks-via-wiremock), så husk gjerne å oppdatere mocken
med nye feature toggles etter hvert som de legges til.
Hvis du heller ønsker å peke lokal applikasjon direkte mot Unleash kan du gjøre følgende:

1. Konfigurer miljøvariabelen `UNLEASH_SERVER_API_URL` med riktig
   URL: https://team-mulighetsrommet-unleash-api.nav.cloud.nais.io
2. Opprett et
   personlig [Unleash token](https://team-mulighetsrommet-unleash-web.iap.nav.cloud.nais.io/profile/personal-api-tokens)
   og konfigurerer miljøvariabelen `UNLEASH_SERVER_API_TOKEN` med dette tokenet
3. Start applikasjonen

## Automatiske jobber

Vi har flere automatiske jobber som kjører til gitte intervaller. Disse finner man under mappen `/tasks`. Jobbene
bruker `db-scheduler` for å vite når de skal kjøre.

### Oppdatere enheter fra NORG til Sanity

Jobben `synchronize-norg2-enheter-to-sanity` brukes for å oppdatere enheter i Sanity med fasit fra NORG2.
Jobben benytter seg av tokenet `Token for task: synchronize-norg2-enheter-to-sanity fra api (PROD | TEST)` som har
create/update-tilgang til enheter-dokumentene i Sanity.

### Synkronisere kontaktpersoner til admin-flate

Administratorer kan selv legge til kontaktpersoner fra Tiltaksadministrasjon, men hvis kontaktperson ikke allerede
finnes og gjøres tilgjengelig i Sanity så kan kontaktperson også legges til manuelt av oss.

Dette gjøres på følgende måte:

1. Naviger til [Grupper jeg eier](https://myaccount.microsoft.com/groups/groups-i-own) i Entra Id
2. Finn frem til gruppen for kontaktpersoner i Tiltaksadministrasjon
3. Trykk på "Medlemmer" -> Trykk "Legg til"
4. Søk opp personen med navn -> Velg "Medlem" -> Trykk "Legg til"

**TIPS:** Du kan gå til [MAAM](https://mulighetsrommet-arena-adapter-manager.intern.nav.no/) og velge mr-api (i
toppmenyen) og så kjøre task'en `sync-navansatte`. Da skal kontaktpersoner blir synkronisert i løpet av ett minutt.

**MERK:** Hvis du mangler tilgang til AD så kan du selv be om tilgang ved å følge beskrivelse
her: https://github.com/navikt/azure-ad-self-service/blob/main/DirectoryRead/README.md

## <a name="strukturert-innhold"></a>Strukturert innhold

Vi deler strukturert innhold om tiltakstyper via Kafka.
Innholdet lever i tabeller i databasen og kan oppdateres via filen `R__deltakerregistrering-innhold.sql`. Gjør de
endringene som må til og deploy så vil migrasjonsfilen kjøres på nytt mot databasene i dev og prod.
