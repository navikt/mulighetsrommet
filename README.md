<h1 align="center">Mulighetsrommet 🎯</h1>

## Introduksjon

Mulighetsrommet er en applikasjonsportfølje som skal hjelpe både brukere og veiledere til å få en helhetlig oversikt
over alle arbeidsmarkedstiltak NAV kan tilby.
Brukere vil på sikt få en oversikt gjennom en egen åpen flate med sine muligheter og kan selv melde sin interesse på
diverse tiltak, oppfølging eller kurs.
Veiledere vil også kunne få en samlet oversikt over all informasjon fra flere fagsystemer som Navet og Arena.
Hensikten er å kunne gi begge parter lett tilgang til den samme kvalitetssikret tiltaksinformasjonen som vi har i NAV.

## Oppsett

Dette prosjektet er strukturert som et monorepo, der både backend- og frontend-kode er organisert i samme kodebase.
Enn så lenge benytter vi følgende tooling for å kjøre tasks for henholdsvis backend og frontend:

- [Gradle](https://gradle.org/) med subprojects
- [Turborepo](https://turborepo.org/) i kombinasjon med [NPM workspaces](https://turborepo.org/)

### Tooling via asdf

Om ønskelig så kan [asdf](https://asdf-vm.com/) benyttes til å installere vertkøyene som trengs for å kjøre dette prosjektet lokalt.
Dette prosjektet inkluderer en `asdf` [.tool-versions](https://asdf-vm.com/manage/configuration.html#tool-versions)-fil som spesifiserer versjoner for runtime-avhengigheter som matcher det vi kjører på Github Actions (CI) og på NAIS.

For å benytte `asdf` så holder det å [installere asdf](https://asdf-vm.com/guide/getting-started.html) og deretter kjøre kommandoen `asdf install` i rot av prosjektet.
Foreløpig liste over verktøy som blir håndtert via `asdf` for dette prosjektet er som følger:

- java
- gradle
- nodejs
- kubectl

Om du får beskjed om at du mangler plugins fra noen av verktøyene over kan du kjøre

```bash
asdf plugin-add java
asdf plugin-add gradle https://github.com/rfrancis/asdf-gradle.git
asdf plugin-add nodejs
asdf plugin-add kubectl https://github.com/asdf-community/asdf-kubectl.git
```

### Git hooks

For å gjøre noen rutineoppgaver enklere er det mulig å installere følgende git hooks på eget initiativ (ikke en komplett
liste, blir oppdatert etter hvert som behovet oppstår):

- Installasjon av pre-commit hook for å kjøre `ktlintFormat` på endrede filer: Kjør
  kommando `./gradlew addKtlintFormatGitPreCommitHook`
- Installasjon av pre-commit hook for å kjøre `ktlintCheck` på endrede filer: Kjør
  kommando `./gradlew addKtlintCheckGitPreCommitHook`

## Overvåking av løsninger

Det finnes noen tilgjenglige dashboards, men nytten med disse kan variere:

- [Innsikt i logger](<https://logs.adeo.no/app/dashboards#/view/6927d260-00ed-11ed-9b1a-4723a5e7a9db?_g=(filters:!(),refreshInterval:(pause:!t,value:0),time:(from:now-15m,to:now))>)
- [Metrikker fra frontend](<https://logs.adeo.no/app/dashboards#/view/b9e91b00-01ba-11ed-9b1a-4723a5e7a9db?_a=(viewMode:edit)&_g=(filters:!(),refreshInterval:(pause:!t,value:0),time:(from:now-15m,to:now))>)
- [Metrikker fra backend](https://grafana.nais.io/d/8W2DNq6nk/mulighetsrommet-api?orgId=1&refresh=5m&var-datasource=prod-gcp&var-duration=30m&var-team=team-mulighetsrommet&from=now-15m&to=now)

## Feature toggles

Vi bruker Unleash for å skru av eller på funksjonalitet tilknyttet
løsningen: [https://unleash.nais.io/#/features](https://unleash.nais.io/#/features)

## Moduler

### `mulighetsrommet-veileder-flate`

| |                                                                                                         |
|------------------|---------------------------------------------------------------------------------------------------------|
| Kildekode        | <https://github.com/navikt/mulighetsrommet/tree/main/frontend/mulighetsrommet-veileder-flate>           |
| README           | <https://github.com/navikt/mulighetsrommet/blob/main/frontend/mulighetsrommet-veileder-flate/README.md> |
| Url (dev-miljø)  | <https://veilarbpersonflate.intern.dev.nav.no/12118323058>                                              |
| Url (labs-miljø) | <https://mulighetsrommet-veileder-flate.ekstern.dev.nav.no/12345678910>                                                  |

### `mulighetsrommet-api-client`

Klient til frontend for å snakke med backend. Auto-generert med OpenAPI via `openapi.yaml` i `mulighetsrommet-api`.

| | |
| --------------- | ----------------------------------------------------------------------------------- |
| Kildekode | <https://github.com/navikt/mulighetsrommet/tree/main/frontend/mulighetsrommet-api-client> |
| README | <https://github.com/navikt/mulighetsrommet/blob/main/frontend/mulighetsrommet-api-client/README.md> |
| openapi.yaml | <https://github.com/navikt/mulighetsrommet/tree/main/mulighetsrommet-api/src/main/resources/web/openapi.yaml> |

### `mulighetsrommet-veileder-cms`

Sanity Studio til forvaltning av informasjon for veiledere.

| | |
| --------------- | ----------------------------------------------------------------------------------- |
| Kildekode | <https://github.com/navikt/mulighetsrommet/tree/main/frontend/mulighetsrommet-cms> |
| README | <https://github.com/navikt/mulighetsrommet/blob/main/frontend/mulighetsrommet-cms/README.md> |
| Url (test-datasett) | <https://mulighetsrommet-sanity-studio.intern.nav.no/test/desk> |
| Url (prod-datasett) | <https://mulighetsrommet-sanity-studio.intern.nav.no/production/desk> |

### `mulighetsrommet-api`

| |                                                                                     |
|-----------------|-------------------------------------------------------------------------------------|
| Kildekode       | <https://github.com/navikt/mulighetsrommet/tree/main/mulighetsrommet-api>           |
| README          | <https://github.com/navikt/mulighetsrommet/blob/main/mulighetsrommet-api/README.md> |
| Url (dev-miljø) | <https://mulighetsrommet-api.intern.dev.nav.no/>                                    |
| API             | <https://mulighetsrommet-api.intern.dev.nav.no/swagger-ui>                          |

### `mulighetsrommet-kafka-manager`

Applikasjon som gir oversikt over kafka-topics relevante for dette prosjektet.

| |                                                                         |
| --------------- |-------------------------------------------------------------------------|
| README | <https://github.com/navikt/kafka-manager>                               |
| Kildekode | <https://github.com/navikt/mulighetsrommet/tree/main/iac/kafka-manager> |
| Url (dev-miljø) | <https://mulighetsrommet-kafka-manager.intern.dev.nav.no>               |
| Url (prod-miljø | <https://mulighetsrommet-kafka-manager.intern.nav.no>                   |

### `mr-admin-flate`

Administrasjonsflate for tiltak- og fagansvarlige i NAV som jobber med tiltakstyper og tiltaksgjennomføringer.

| |                                                                                         |
|------------------|-----------------------------------------------------------------------------------------|
| README           | <https://github.com/navikt/mulighetsrommet/blob/main/frontend/mr-admin-flate/README.md> |
| Demo-miljø       | <https://mulighetsrommet-admin-flate.ekstern.dev.nav.no>                                      |
| Url (dev-miljø)  | <https://mulighetsrommet-admin-flate.intern.dev.nav.no>                                 |
| Url (prod-miljø) | <https://mulighetsrommet-admin-flate.intern.nav.no>                                     |

## Overvåking av automatiske jobber
Vi har satt opp to Slack-bots som kan gi beskjed til oss på Slack i kanalen #team-valp-monitoring dersom det oppstår feil under kjøring av de automatiske jobbene.

Botene finner man her:
- Dev-monitorering: https://api.slack.com/apps/A04PW7S8J94/general
- Prod-monitorering: https://api.slack.com/apps/A04Q2NNABDZ

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles via issues her på GitHub.

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team-valp.
