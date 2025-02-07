# mulighetsrommet-pdfgen

Denne applikasjonen benyttes til å generere PDF-filer i forbindelse med tiltaksøkonomi.
Den består i hovedsak av [Handlebars](https://handlebarsjs.com/) templates og en Dockerfile som pakker disse templatene
sammen med [pdfgen](https://github.com/navikt/pdfgen) og som deretter deployes til Nais.

Foreløpig er følgende templates er støttet:

- Utbetaling
    - Innsendelse fra tiltaksarrangører som arkiveres til Joark
    - Kvittering av utbetaling som arrangører kan laste ned til egen bruk

## Utvikling av maler

- Applikasjonen [startes lokalt via docker](../README.md#docker)
- Se dokumentasjon til [Handlebars](https://handlebarsjs.com/) for templates, syntax etc.
    - Det er også en
      del [hjelpefunksjoner](https://github.com/navikt/pdfgen-core?tab=readme-ov-file#handlerbars-helpers) tilgjengelig
      via pdfgen
- Mappestrukturen er satt av [pdfgen](https://github.com/navikt/pdfgen?tab=readme-ov-file#getting-started)
    - Templates skal legges i `templates`-mappa med struktur `<app>/<mal>.hbs`
    - Mockdata kan legges til i `data`-mappa med samme strutur `<app>/<mal>.json` (om ønskelig)

Følgende endepunkt kan benyttes til å laste ned PDF-filer lokal:

```
GET /api/v1/genpdf/<app>/<mal>
```

Man kan endre mockdata for å se hvordan PDF-filene blir seendes ut uten å restarte applikasjonen, men hvis man endrer på
selve malene så må applikasjonen restartes. Eksempel:

```sh
# Last ned utbetaling/journalpost til filen `mal.pdf` (gitt at du har curl installert)
curl localhost:8888/api/v1/genpdf/utbetaling/journalpost --output mal.pdf

# Restart pdfgen etter endringer på malene
docker compose restart pdfgen
```

## Produksjon

I produksjon er ikke oppsettet for mockdata skrudd på. Og den er tilgjengelig via tilgangskontroll definert i Nais.

Følgende endepunkt kan benyttes til å genere PDF-filer der `body` er et JSON-objekt som blir tilgjengelig under
template-prosesseringen.

```
POST /api/v1/genpdf/<app>/<mal>
{ body }
```
