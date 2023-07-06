# Admin-flate

Kildekode for flate for administrative oppgaver rundt opprettelse og redigering av tiltakstyper og tiltaksgjennomføringer.

## Oppsett

### Lokal utvikling

`npm install`
`npm run dev`

### Lokal utvikling mot lokal backend

`.env` må konfigureres med følgende variabler:

```.env
# Setter root url for alle HTTP-kall til mulighetsrommet-api.
VITE_MULIGHETSROMMET_API_BASE='http://localhost:8080'

# Setter Bearer token for HTTP-kall mot mulighetsrommet-api.
# Dette tokenet kan genereres med å følge guiden beskrevet i README.md til mulighetsrommet-api.
# MERK: det genererte tokenet trenger følgende custom claims for at login og access skal fungere:
#   - NAVident: En tilfeldig NAVident
#   - oid: En tilfeldig UUID
VITE_MULIGHETSROMMET_API_AUTH_TOKEN=<token>
```

Deretter kan du kjøre `npm run backend`.

## Mikrofrontends
Vi rendret Team Komet sin Deltakerliste-app som en mikrofrontend hos oss.
Kildekoden hos Komet ligger her: https://github.com/navikt/amt-deltakerliste-flate/tree/main

Vi kan skjule appen via toggle: https://unleash.nais.io/#/features/strategies/mulighetsrommet.admin-flate-vis-deltakerliste-fra-komet

### Lokal utvikling med lokal microfrontend

For å sjekke hvordan appen rendrer sammen med microfrontend som kjører lokalt må man
først kjøre `npm run build` og `npm run preview` for å starte microfrontenden.

Så må man kjøre `npm run build:local` og `npm run preview` for å rendre admin-flate.

## Deploy

Ved merge til main-branch deployes appen til dev og prod.

## Demo

Se demo av løsningen her https://mulighetsrommet-admin-flate.ekstern.dev.nav.no

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles via issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #valp-brukerstøtte
