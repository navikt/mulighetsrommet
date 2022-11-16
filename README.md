<h1 align="center">Mulighetsrommet 🎯</h1>

![Deploy (api)](https://github.com/navikt/mulighetsrommet/actions/workflows/mulighetsrommet-api.yaml/badge.svg)
![Deploy (frontend)](https://github.com/navikt/mulighetsrommet/actions/workflows/mulighetsrommet-veileder-flate.yaml/badge.svg)

## Introduksjon

Mulighetsrommet er en applikasjonsportfølje som skal hjelpe både brukere og veiledere til å få en helhetlig oversikt
over alle arbeidsmarkedstiltak NAV kan tilby.
Brukere vil på sikt få en oversikt gjennom en egen åpen flate med sine muligheter og kan selv melde sin interesse på
diverse tiltak, oppfølging eller kurs.
Veiledere vil også kunne få en samlet oversikt over all informasjon fra flere fagsystemer som Navet og Arena.
Hensikten er å kunne gi begge parter lett tilgang til den samme kvalitetssikret tiltaksinformasjonen som vi har i NAV.

## Overvåking av løsninger

Det finnes to dashboards for løsningen
hhv. [Her for API](<https://logs.adeo.no/app/dashboards#/view/6927d260-00ed-11ed-9b1a-4723a5e7a9db?_g=(filters:!(),refreshInterval:(pause:!t,value:0),time:(from:now-15m,to:now))>)
og [her for metrikker fra frontend](<https://logs.adeo.no/app/dashboards#/view/b9e91b00-01ba-11ed-9b1a-4723a5e7a9db?_a=(viewMode:edit)&_g=(filters:!(),refreshInterval:(pause:!t,value:0),time:(from:now-15m,to:now))>)

## Feature toggles

Vi bruker Unleash for å skru av eller på funksjonalitet tilknyttet
løsningen: [https://unleash.nais.io/#/features](https://unleash.nais.io/#/features)

## Moduler

### `mulighetsrommet-veileder-flate`

|                  |                                                                                                         |
| ---------------- | ------------------------------------------------------------------------------------------------------- |
| Kildekode        | <https://github.com/navikt/mulighetsrommet/tree/main/frontend/mulighetsrommet-veileder-flate>           |
| README           | <https://github.com/navikt/mulighetsrommet/blob/main/frontend/mulighetsrommet-veileder-flate/README.md> |
| Url (dev-miljø)  | <https://veilarbpersonflate.dev.intern.nav.no/12118323058>                                              |
| Url (labs-miljø) | <https://mulighetsrommet-veileder-flate.labs.nais.no/>                                                  |

### `mulighetsrommet-api-client`

Klient til frontend for å snakke med backend. Auto-generert med OpenAPI via `openapi.yml` i `mulighetsrommet-api`.
| | |
| ---------------- |
---------------------------------------------------------------------------------------------------- |
| Kildekode | <https://github.com/navikt/mulighetsrommet/tree/main/frontend/mulighetsrommet-api-client> |
| README | <https://github.com/navikt/mulighetsrommet/blob/main/frontend/mulighetsrommet-api-client/README.md> |
| openapi.yml
| <https://github.com/navikt/mulighetsrommet/tree/main/mulighetsrommet-api/src/main/resources/web/openapi.yml> |

### `mulighetsrommet-veileder-cms`

Sanity Studio til forvaltning av informasjon for veiledere.
| | |
| ---------------- |
---------------------------------------------------------------------------------------------------- |
| Kildekode | <https://github.com/navikt/mulighetsrommet/tree/main/frontend/mulighetsrommet-veileder-flate> |
| README | <https://github.com/navikt/mulighetsrommet/blob/main/frontend/mulighetsrommet-veileder-flate/README.md> |
| Url (test-datasett) | <https://mulighetsrommet.sanity.studio/test/desk> |
| Url (prod-datasett) | <https://mulighetsrommet.sanity.studio/production/desk> |

### `mulighetsrommet-api`

|                 |                                                                                     |
| --------------- | ----------------------------------------------------------------------------------- |
| Kildekode       | <https://github.com/navikt/mulighetsrommet/tree/main/mulighetsrommet-api>           |
| README          | <https://github.com/navikt/mulighetsrommet/blob/main/mulighetsrommet-api/README.md> |
| Url (dev-miljø) | <https://mulighetsrommet-api.dev.intern.nav.no/>                                    |
| API             | <https://mulighetsrommet-api.dev.intern.nav.no/swagger-ui>                          |

### `mulighetsrommet-kafka-manager`

Denne kjøres kun opp ved egen kommando `kubectl apply -f .nais/mulighetsrommet-kafka-manager.yaml`. Se README for mer
detaljer.
| | |
| --------------- | -------------------------------------------------------- |
| README | <https://github.com/navikt/kafka-manager> |
| Url (dev-miljø) | <https://mulighetsrommet-kafka-manager.dev.intern.nav.no/> |

## Oppsett

Dette prosjektet er strukturert som et monorepo, der både backend- og frontent-kode er organisert i samme kodebase.
Enn så lenge benytter vi følgende tooling for å kjøre tasks for henholdsvis backend og frontend:

- [Gradle](https://gradle.org/) med subprojects
- [Turborepo](https://turborepo.org/) i kombinasjon med [NPM workspaces](https://turborepo.org/)

### Git hooks

For å gjøre noen rutineoppgaver enklere er det mulig å installere følgende git hooks på eget initiativ (ikke en komplett
liste, blir oppdatert etter hvert som behovet oppstår):

- Installasjon av pre-commit hook for å kjøre `ktlintFormat` på endrede filer: Kjør
  kommando `./gradlew addKtlintFormatGitPreCommitHook`
- Installasjon av pre-commit hook for å kjøre `ktlintCheck` på endrede filer: Kjør
  kommando `./gradlew addKtlintCheckGitPreCommitHook`

# Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles via issues her på github.

## For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #valp-brukerstøtte
