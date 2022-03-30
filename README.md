<h1 align="center">Mulighetsrommet 🎯</h1>

[![Deploy (api)](https://github.com/navikt/mulighetsrommet/actions/workflows/build-deploy-api.yaml/badge.svg)](https://github.com/navikt/mulighetsrommet/actions/workflows/build-deploy-api.yaml)
[![Deploy (frontend)](https://github.com/navikt/mulighetsrommet/actions/workflows/build-deploy-frontend.yaml/badge.svg)](https://github.com/navikt/mulighetsrommet/actions/workflows/build-deploy-frontend.yaml)

## Introduksjon

Mulighetsrommet er en applikasjonsportfølje som skal hjelpe både brukere og veiledere til å få en helhetlig oversikt over alle arbeidsmarkedstiltak NAV kan tilby.
Brukere vil på sikt få en oversikt gjennom en egen åpen flate med sine muligheter og kan selv melde sin interesse på diverse tiltak, oppfølging eller kurs.
Veiledere vil også kunne få en samlet oversikt over all informasjon fra flere fagsystemer som Navet og Arena.
Hensikten er å kunne gi begge parter lett tilgang til den samme kvalitetssikret tiltaksinformasjonen som vi har i NAV.

## Moduler

### `mulighetsrommet-veileder-flate`

|                  |                                                                                                      |
| ---------------- | ---------------------------------------------------------------------------------------------------- |
| Kildekode        | https://github.com/navikt/mulighetsrommet/tree/dev/frontend/mulighetsrommet-veileder-flate           |
| README           | https://github.com/navikt/mulighetsrommet/blob/dev/frontend/mulighetsrommet-veileder-flate/README.md |
| Url (dev-miljø)  | https://mulighetsrommet-veileder-flate.dev.intern.nav.no/                                            |
| Url (labs-miljø) | https://mulighetsrommet-veileder-flate.labs.nais.no/                                                 |

### `mulighetsrommet-api`

|                 |                                                                                  |
| --------------- | -------------------------------------------------------------------------------- |
| Kildekode       | https://github.com/navikt/mulighetsrommet/tree/dev/mulighetsrommet-api           |
| README          | https://github.com/navikt/mulighetsrommet/blob/dev/mulighetsrommet-api/README.md |
| Url (dev-miljø) | https://mulighetsrommet-api.dev.intern.nav.no/                                   |
| API             | https://mulighetsrommet-api.dev.intern.nav.no/swagger-ui                         |

### `mulighetsrommet-kafka-manager`

Denne kjøres kun opp ved egen kommando `kubectl apply -f .nais/mulighetsrommet-kafka-manager.yaml`. Se README for mer detaljer.
| | |
| --------------- | -------------------------------------------------------- |
| README | https://github.com/navikt/kafka-manager |
| Url (dev-miljø) | https://mulighetsrommet-kafka-manager.dev.intern.nav.no/ |
