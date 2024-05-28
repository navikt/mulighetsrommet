# `mulighetsrommet-veileder-cms`

Forvaltning av arbeidsmarkedinformasjon rettet mot veiledere

## Sanity Content Studio

Sanity Studio satt opp med NAV SSO-login.

### 🚀 Kom i gang

Start Studio lokalt:

```
npm install
npm run dev
```

### Deploy Studio
Studioet deployes automatisk ved endringer til Github og hostes på nais.
Du kan nå studioet via url'en her https://mulighetsrommet-sanity-studio.intern.nav.no

**OBS** - Ikke bruk `sanity deploy` for å deploye studio til Sanity.

### 👨‍👩‍👦‍👦 Nye brukere

Nye brukere som logger seg inn på Admin-panelet via NAV SSO vil automatisk få tildelt rollen `Viewer`.
Følgende steg må til for å logge inn første gang:

1. Legg til Sanity.io i [My Apps](https://myapps.microsoft.com/), og vent på at du får denne godkjent.
2. Logg deg inn på CMS'et vha. NAV SSO.
3. Hvis du ønsker andre rettigheter enn `Viewer` må du ta kontakt med en som har rollen `Administrator` i prosjektet.

## Backup
Backup av Sanity kjøres som en cron-jobb på Github Actions hver natt kl. 04.00 og lagrer backup av både test- og produksjonsdatasettet i en bucket i prod-miljøet til Team mulighetsrommet.

[Her finner man bucket for backups](https://console.cloud.google.com/storage/browser/team-mulighetsrommet-sanity-backup;tab=objects?prefix=&forceOnObjectsSortingFiltering=false&authuser=1)

Man trenger følgende tilganger i IAM for å se innholdet:
* Storage Admin

### 🔗 Nyttige lenker

- [Sanity Studio Docs](https://www.sanity.io/docs/sanity-studio)
- [Sanity Reference Docs](https://www.sanity.io/docs/reference)
