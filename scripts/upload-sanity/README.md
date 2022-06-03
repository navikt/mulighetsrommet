# Opplasting av tiltaksgjennomføringer til Sanity

For å laste dokumenter opp til Sanity gjør man følgende:

1. Last ned Forms-data og åpne i Excel. Endre kolonnen med oppstartsdato til formatet YYYY-MM-DD
2. Lagre excel-ark som csv-fil og kall den tiltak.csv. Lagre den på rot i dette prosjektet.
3. Opprett et Access token og gi det `developer`-tilganger
4. Opprett en .env-fil med følgende verdier



```
SANITY_PROJECT_ID=<Kopier inn Sanity project id her>
SANITY_DATASET=<Skriv inn hvilke dataset man skal populere>
SANITY_TOKEN=<Lim inn Access token opprettet i steg 3>
```

5. Kjør så `npx ts-node ./upload.ts`
6. Hvis alt gikk ok så skal du se i loggen at dokumentene ble lastet opp til Sanity.


#### Lokal utvikling
1. `npm install`
2. Dersom du ønsker å se opplastet data i Sanity studio må du endre dataset basert på hvor du laster opp. Du endrer dataset i `./frontend/mulighetsrommet-cms/sanity.json`.
3. Åpne et nytt terminal-vindu/tab og kjør `npx nodemon ./upload.ts` - nodemon restarter prosessen ved hver endring i fil. Hack i vei!