# `mulighetsrommet-veileder-cms`

test
Forvaltning av arbeidsmarkedinformasjon rettet mot veiledere

## Sanity Content Studio

Sanity Studio satt opp med NAV SSO-login.

### 🚀 Kom i gang

Start Studio lokalt:

```
npm install
npm run start
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

### 📊 Opplasting av statistikkfil fra datavarehuset

Når vi får tilsendt Excel-fil med overgangsstatistikk fra datavarehuset så må Excel-filen få litt kjærlighet før den kan lastes opp til Sanity. Følg sjekklisten under for å få en fil du kan laste opp til Sanity.

1. Endre navn på headere - (Se fil som du skal erstatte med. Denne kan lastes ned fra Sanity)
2. Fyll inn blanke felter med verdien 0. (Feks. om kategorien "Ukjent" har 100% så må man fylle inn 0 for de andre kategoriene)
3. Marker alle kolonnene som inneholder tall-data og sjekk at Excel formaterer kolonnene som tall. På Mac markerer man kolonnene og trykker cmd + 1 for å velge datatype.
4. Eksporter til `CSV UTF-8 (kommadelt) (.csv)` fra Excel og last opp som et dokument under *Statistikkfil*
5. Vent noen minutter og applikasjonen bør begynne å bruke den siste opplastede filen.
6. Om man har mange statistikkfiler kan det være en idé å slette noen av de gamle etter hvert som de blir utdaterte.

### 📊 Oppdatering av nøkkeltall for "Overgang til arbeid"
Fra Excel-skjema tilsendt fra datavarehuset tar man og filtrer bort alt som ikke er 12 mnd og gjelder kategoriene **Kun arbeidstaker** og **Arbeidstaker med ytelse** 5 år tilbake i tid og regner ut gjennomsnittet for de to kategoriene.

**Oppskrift:**
"Kun arbeidstaker" + "Arbeidstaker med ytelse" per år
Legg sammen alle årene og del på antall år

Legg så inn i Sanity per tiltakstype.

For de tiltakstypene vi ikke har statistikk for er det bare å ikke legge inn nøkkeltall. Da vil vi heller ikke vise noe i frontend.

## Backup
Backup av Sanity kjøres som en cron-jobb på Github Actions hver natt kl. 04.00 og lagrer backup av både test- og produksjonsdatasettet i en bucket i prod-miljøet til Team mulighetsrommet.

[Her finner man bucket for backups](https://console.cloud.google.com/storage/browser/team-mulighetsrommet-sanity-backup;tab=objects?prefix=&forceOnObjectsSortingFiltering=false&authuser=1)

Man trenger følgende tilganger i IAM for å se innholdet:
* Storage Admin

### 🔗 Nyttige lenker

- [Sanity Studio Docs](https://www.sanity.io/docs/sanity-studio)
- [Sanity Reference Docs](https://www.sanity.io/docs/reference)
