# `mulighetsrommet-flate`

<p>
Tilhørende flate for <strong>mulighetsrommet-api</strong> for behandling av tiltaksinformasjon.
</p>

# Innhold

- [Teknologier](#teknologier)
- [Kom i gang](#kom-i-gang)
  - [Forutsetninger](#forutsetninger)
  - [Steg for steg](#steg-for-steg)

# <a name="teknologier"></a>Teknologier

Øvrige teknologier, rammeverk og biblioteker som er blitt tatt i bruk:

[**Typescript**](https://www.typescriptlang.org/)

[**React**](https://reactjs.org/)

[**react-query**](https://react-query.tanstack.com/)

[**jotai**](https://github.com/pmndrs/jotai)

NAVs designsystem: [**@navikt/ds-css**](https://github.com/navikt/nav-frontend-moduler)

Mocking av testdata: [**MSW**](https://mswjs.io/)

Testverktøy for ende-til-ende-testing: [**Cypress**](https://www.cypress.io/)

# <a name="kom-i-gang"></a>Kom i gang

For å komme i gang kan du hoppe rett til [Steg for steg](#steg-for-steg) gitt at du har satt opp alt under [Forutsetinger](#forutsetninger).

## <a name="forutsetninger"></a>Forutsetninger

Under er det satt opp et par ting som må på plass for at applikasjonen og databasen skal fungere.

### Node/NPM

Last ned og installer Node versjon 16 (eller høyere, på eget ansvar) (NPM er inkludert) [her](https://nodejs.org/en/), eller gjennom tooling som f.eks. [asdf-vm](https://github.com/asdf-vm/asdf).

### Miljøvariabler

Kjører man opp frontend med scriptene som ligger i `package.json` trenger man ikke foreta seg noe. Men vi to miljøvariabler som man kan sette manuelt hvis man har lyst. `REACT_APP_ENABLE_MOCK` er en toggle for å kjøre in-memory mock-server sammen med applikasjonen. `REACT_APP_BACKEND_API_ROOT` setter root uri for alle fetch-kall.

```sh
export REACT_APP_ENABLE_MOCK=true/false
export REACT_APP_BACKEND_API_ROOT='http://localhost:8080'
```

Legg til disse enten i `.bashrc` eller `.zshrc` eller kjør dem per session rett i terminalen.

## <a name="steg-for-steg"></a>Steg for steg

### In-memory mock-server

Kjør `npm start` for å kjøre frontenden med MSW.

### Backend

Kjør `npm run backend` for å fyre opp frontend mot reell backend (`mulighetsrommet-api`). Forutsetning at denne kjører.

### Testing

#### Ende-til-ende testing med Cypress
Gå inn til frontend-mappen i terminalen
For å kjøre testene i IDE: `npx cypress run`.
For å se testene, kjør først `npm start` for å starte programmet lokalt, og deretter `npx cypress open`.
