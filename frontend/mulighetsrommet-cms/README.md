# `mulighetsrommet-veileder-cms`

Forvaltning av arbeidsmarkedinformasjon rettet mot veiledere

## Sanity Content Studio

Sanity Studio satt opp med NAV SSO-login.

### Kom i gang

Start Studio lokalt:

```
npm install
npm run start
```

### Generer typer til veileder-flate

`npx sanity-codegen` Generer typer som blir tilgjengelig i veileder-flate for bruk med feks. `useSanity`-hooken.

Config finner man i sanity-codegen.config.ts-filen. 

### Nye brukere

Nye brukere som logger seg inn på Admin-panelet via NAV SSO vil automatisk få tildelt rollen `Viewer`.
Følgende steg må til for å logge inn første gang:

1. Legg til Sanity.io i [My Apps](https://myapps.microsoft.com/), og vent på at du får denne godkjent.
2. Logg deg inn på CMS'et vha. NAV SSO.
3. Hvis du ønsker andre rettigheter enn `Viewer` må du ta kontakt med en som har rollen `Administrator` i prosjektet.

### Nyttige lenker

- [Sanity Studio Docs](https://www.sanity.io/docs/sanity-studio)
- [Sanity Reference Docs](https://www.sanity.io/docs/reference)
