# nav-enheter-to-sanity

Et script for å mappe om og laste opp NAV fylkesenheter og lokale kontorer til Sanity.

Scriptet blir deployet som en Naisjob og kjører jevnlig.

## Bygging

Pakken er avhengig av at api-klienten for Norg2 har blitt bygget.
Hvis bygging feiler pga. dette kan det hjelpe å kjøre `npm install` på nytt.

```
npm install
npm run build
```

## Miljøvariabler

For å kjøre lokalt (og i GCP) må følgende miljøvariabler være definert:

```
# Endepunkt til Norg2
NORG2_API_ENDPOINT=https://norg2.intern.nav.no/norg2
# Sanity prosjekt og dataset
SANITY_PROJECT_ID=xegcworx
SANITY_DATASET=production
# Sanity token, om nødvendig
SANITY_AUTH_TOKEN=...
```
