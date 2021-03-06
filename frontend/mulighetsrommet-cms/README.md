# `mulighetsrommet-veileder-cms`

Forvaltning av arbeidsmarkedinformasjon rettet mot veiledere

## Sanity Content Studio

Sanity Studio satt opp med NAV SSO-login.

### 🚀 Kom i gang

Start Studio lokalt:

```
npm install
npm run start
```

### 👨‍👩‍👦‍👦 Nye brukere

Nye brukere som logger seg inn på Admin-panelet via NAV SSO vil automatisk få tildelt rollen `Viewer`.
Følgende steg må til for å logge inn første gang:

1. Legg til Sanity.io i [My Apps](https://myapps.microsoft.com/), og vent på at du får denne godkjent.
2. Logg deg inn på CMS'et vha. NAV SSO.
3. Hvis du ønsker andre rettigheter enn `Viewer` må du ta kontakt med en som har rollen `Administrator` i prosjektet.

### 📊 Opplasting av statistikkfil fra datavarehuset

Når vi får tilsendt Excel-fil med overgangsstatistikk fra datavarehuset så må Excel-filen få litt kjærlighet før den kan lastes opp til Sanity. Følg sjekklisten under for å få en fil du kan laste opp til Sanity.

1. Endre navn på headere - (Se fil som du skal erstatte med. Denne kan lastes ned fra Sanity)
2. Fyll inn blanke felter med verdien 0. (Feks. om kategorien "Ukjent" har 100% så må man fylle inn 0 for de andre kategoriene)
3. Marker alle kolonnene som inneholder tall-data og sjekk at Excel formaterer kolonnene som tall. På Mac markerer man kolonnene og trykker cmd + 1 for å velge datatype.
4. Eksporter til `CSV UTF-8 (kommadelt) (.csv)` fra Excel og last opp som et dokument under *Statistikkfil*
5. Vent noen minutter og applikasjonen bør begynne å bruke den siste opplastede filen.
6. Om man har mange statistikkfiler kan det være en idé å slette noen av de gamle etter hvert som de blir utdaterte.

### 🔗 Nyttige lenker

- [Sanity Studio Docs](https://www.sanity.io/docs/sanity-studio)
- [Sanity Reference Docs](https://www.sanity.io/docs/reference)
