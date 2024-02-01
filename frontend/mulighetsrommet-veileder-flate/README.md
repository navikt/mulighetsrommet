# `mulighetsrommet-veileder-flate`

Visning av arbeidsmarkedstiltak til veiledere og NAV-ansatte.

Applikasjonen bygges og distribueres i flere varianter avhengig av bruksområde.

**DEMO**
- En demo-versjon som samler alle variantene nevnte under og inkluderer mocks og fungerer uten en backend

**MODIA**
- En variant rettet med veiledere ved arbeidsrettet oppfølging.
  - Kjører med en bruker (borger) i kontekst og inkluderer en del funksjoner rettet mot samhendling mellom NAV-veileder og bruker.
  - Lar deg finne relevante arbeidsmarkedstiltak for bruker i kontekst.
- Blir distribuert som en [Web Component](https://developer.mozilla.org/en-US/docs/Web/API/Web_components) og inkludert som en microfrontend i [veilarbpersonflate](https://github.com/navikt/veilarbpersonflatefs).

**NAV**

- En variant som er tilgjengelig for alle NAV-ansatte.
  - Gir deg en oversikt over arbeidsmarkedstiltak i NAV.
- Inneholder også en modus for forhåndsvisning av redaksjonelt innhold
  - Tilgjengelig for administratorer/redaktører innen tiltaksadministrasjon.
  - Inkluderer noen mock-varianter av funksjoner som ellers kun er tilgjenglige for veiledere (via `MODIA`-varianten) slik at man kan forhåndsvise innhold uten å ha en reell bruker i kontekst.

## Lokal utvikling

Som standard vil `DEMO`-appen startes lokalt. Om ønskelig kan dette overstyres via miljøvariabelen `APP` i `.env`:

```.env
APP=DEMO # Eller MODIA, NAV
```

### Installere avhengigheter

```
npm install
```

### Start dev-server med HTTP-mocks via MSW

```
npm run start
```

### Start dev-server koblet mot lokal backend

`.env` må konfigureres med følgende variabler:

```.env
# Bearer token trengs for HTTP-kall mot mulighetsrommet-api.
# Dette tokenet kan genereres med å følge guiden beskrevet i README.md til mulighetsrommet-api.
VITE_MULIGHETSROMMET_API_AUTH_TOKEN=<token>

# Om ønskelig kan API-base settes til noe annet enn det som er standard
# VITE_MULIGHETSROMMET_API_BASE='http://localhost:8080'
```

Deretter kan dev-server startes:

```
npm run backend
```

## Testing og linting

Koden lintes og formatteres med `eslint` og `prettier`.

```
npm run lint

# Fiks det som kan gjøres automatisk, bl.a. kode-formattering
npm run fix-lint
```

E2E-tester er skrevet med `playwright`.
Ved lokal testing kan det være behjelpelig å kjøre `playwright` med UI'et.

```
# Kjør tester
npm run playwright

# Kjør tester med UI
npm run playwright:open
```

## Deploy

Ved merge til main-branch deployes appen til dev og prod.

## <a name="teknologier"></a>Teknologier

Øvrige teknologier, rammeverk og biblioteker som er blitt tatt i bruk:

- [**Typescript**](https://www.typescriptlang.org/)
- [**Vite**](vitejs.dev/)
- [**React**](https://reactjs.org/)
- [**react-query**](https://react-query.tanstack.com/)
- [**jotai**](https://github.com/pmndrs/jotai)
- NAVs designsystem: [**@navikt/ds-css**](https://github.com/navikt/nav-frontend-moduler)
- Mocking av testdata: [**MSW**](https://mswjs.io/)
- Testverktøy for ende-til-ende-testing: [**Playwright**](https://playwright.dev/)
