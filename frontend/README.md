<h1 align="center">amt-informasjon-flate</h1>
<p>
Tilhørende flate for <strong>amt-informasjon-api</strong> for behandling av tiltaksinformasjon.
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

[**@navikt/nav-frontend-moduler**](https://github.com/navikt/nav-frontend-moduler)

[**MirageJs**](https://miragejs.com/)

[**Cypress**](https://www.cypress.io/)

# <a name="kom-i-gang"></a>Kom i gang

For å komme i gang kan du hoppe rett til [Steg for steg](#steg-for-steg) gitt at du har satt opp alt under [Forutsetinger](#forutsetninger).

## <a name="forutsetninger"></a>Forutsetninger

Under er det satt opp et par ting som må på plass for at applikasjonen og databasen skal fungere.

### Node/NPM

Last ned og installer Node (NPM er inkludert) [her](https://nodejs.org/en/).

### Miljøvariabler

Kjører man opp frontend med scriptene som ligger i `package.json` trenger man ikke foreta seg noe. Men vi to miljøvariabler som man kan sette manuelt hvis man har lyst. `REACT_APP_ENABLE_MOCK` er en toggle for å kjøre in-memory mock-server sammen med applikasjonen. `REACT_APP_BACKEND_API_ROOT` setter root uri for alle fetch-kall.

```sh
export REACT_APP_ENABLE_MOCK=true/false
export REACT_APP_BACKEND_API_ROOT='http://localhost:8080'
```

Legg til disse enten i `.bashrc` eller `.zshrc` eller kjør dem per session rett i terminalen.

## <a name="steg-for-steg"></a>Steg for steg

### In-memory mock-server

Kjør `npm run start` for å fyre opp frontenden med MirageJS.

### Backend

Kjør `npm run start:backend` for å fyre opp frontend mot reell backend (`amt-informasjon-api`).
