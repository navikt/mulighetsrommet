# `mulighetsrommet-veileder-flate`

Flate rettet mot veiledere for behandling av tiltaksinformasjon.

# Innhold

- [Deploy](#deploy)
- [Teknologier](#teknologier)
- [Kom i gang](#kom-i-gang)
- [kodestil](#kodestil)
  - [Forutsetninger](#forutsetninger)
  - [Steg for steg](#steg-for-steg)

# <a name="deploy"></a>Deploy

Applikasjonen brukes base-image fra [poao-frontend](https://github.com/navikt/poao-frontend) og deployes via Github Actions når det skjer endringer i `./.nais/nais-<miljø>.yaml`-filene.

Man kan også deploye manuelt fra terminalen ved å autentisere mot nais med

1. `gcloud auth login`
2. `kubectl apply -f .nais/nais-dev.yaml` for dev eller
3. `kubectl apply -f .nais/nais-prod.yaml` for produksjon.

**PS: sjekk at du står i korrekt cluster og har riktig namespace (team-mulighetsrommet) før du deployer manuelt**

# <a name="teknologier"></a>Teknologier

Øvrige teknologier, rammeverk og biblioteker som er blitt tatt i bruk:

- [**Typescript**](https://www.typescriptlang.org/)
- [**Vite**](vitejs.dev/)
- [**React**](https://reactjs.org/)
- [**react-query**](https://react-query.tanstack.com/)
- [**jotai**](https://github.com/pmndrs/jotai)
- NAVs designsystem: [**@navikt/ds-css**](https://github.com/navikt/nav-frontend-moduler)
- Mocking av testdata: [**MSW**](https://mswjs.io/)
- Testverktøy for ende-til-ende-testing: [**Playwright**](https://playwright.dev/)
- CMS for innhold: [**Sanity**](https://mulighetsrommet.sanity.studio/desk)

# <a name="kom-i-gang"></a>Kom i gang

For å komme i gang kan du hoppe rett til [Steg for steg](#steg-for-steg) gitt at du har satt opp alt under [Forutsetinger](#forutsetninger).

# <a name="kodestil"></a>Kodestil

For styling bruker vi css modules med .scss-filendelse. Vi skriver klassenavn med snake_case. Det vil si underscore som separator i klassenavn.

## <a name="forutsetninger"></a>Forutsetninger

Under er det satt opp et par ting som må på plass for at applikasjonen og databasen skal fungere.

### Node/NPM

Last ned og installer Node versjon 16 (eller høyere, på eget ansvar) (NPM er inkludert) [her](https://nodejs.org/en/), eller gjennom tooling som f.eks. [asdf-vm](https://github.com/asdf-vm/asdf).

### Miljøvariabler

Kjører man opp frontend med scriptene som ligger i `package.json` trenger man ikke foreta seg noe.
Følgende miljøvariabler kan settes manuelt i `.env`:

```
# Setter root url for alle HTTP-kall til mulighetsrommet-api.
VITE_MULIGHETSROMMET_API_BASE='http://localhost:8080'

# Setter Bearer token for HTTP-kall mot mulighetsrommet-api.
# Dette tokenet kan genereres med å følge guiden beskrevet i README.md til mulighetsrommet-api.
# MERK: det genererte tokenet trenger følgende custom claims for at login og access skal fungere:
#   - NAVident: En tilfeldig NAVident
#   - oid: En tilfeldig UUID
VITE_MULIGHETSROMMET_API_AUTH_TOKEN=...

# Toggle for å kjøre en in-memory mock av API'et sammen med applikasjonen.
VITE_MULIGHETSROMMET_API_MOCK=true/false

# I forbindelse med mock av API'et kan følgende variabler settes for å konfigurere tilgang direkte til
# et Sanity-prosjekt for utvikling.
# Husk at Access token ikke skal deles med noen, og man må legge tokenet i en egen .env-fil lokalt på maskinen.
# Se "Opprette Access Token i Sanity".
VITE_SANITY_PROJECT_ID=xegcworx
VITE_SANITY_DATASET=development
VITE_SANITY_ACCESS_TOKEN=...
```

### Opprette Access Token i Sanity

Gå til siden her https://www.sanity.io/organizations/ojSsHMQGf/project/xegcworx/api og velg "Add API Token".
Opprett et eget token med \<Navn\>s dev-token og velg "Developer" under "Permissions".

Legg til disse enten i lokal `.env`-fil (denne skal ikke sjekkes inn git), eller sett disse i lokalt miljø slik du selv ønsker.

## <a name="steg-for-steg"></a>Steg for steg

### In-memory mock-server

Kjør `npm start` for å kjøre frontenden med MSW.

### Backend

Kjør `npm run backend` for å fyre opp frontend mot reell backend (`mulighetsrommet-api`).
Forutsetning at denne kjører. Husk å sette `VITE_MULIGHETSROMMET_API_AUTH_TOKEN` med et gyldig token i `.env`.

### Testing

#### Ende-til-ende testing med Playwright

1. Gå inn til frontend-mappen i terminalen.
2. For å kjøre testene i IDE: `npx run playwright test`.
3. For å se testene, kjør først `npm start` for å starte programmet lokalt, og deretter `npm run playwright test --ui`.
