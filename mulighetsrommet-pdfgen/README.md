# mulighetsrommet-pdfgen

Denne applikasjonen benyttes til å generere PDF-filer i forbindelse med tiltaksøkonomi.
Den består i hovedsak av [Handlebars](https://handlebarsjs.com/) templates og en Dockerfile som pakker disse templatene
sammen med [pdfgen](https://github.com/navikt/pdfgen) og som deretter deployes til Nais.

## Tilgjengelige maler

Foreløpig er er bare én mal støttet:

- `templates/block-content/document.hbs`: Genererer en PDF som består av flere seksjoner. Applikasjonen som ber om
  PDF'en må selv
  mappe om domenemodeller til å passe denne modellen.

## Generere PDF-filer lokalt

For å gjøre det enklere å teste generering av PDF'er fra lokal maskin så kan testdata legges i `data/<app>/<data>.json`.

Deretter kan du kjøre scriptet `pdf-curler.sh` for å generere PDF-filer basert på valgt mal og testdata
(scriptet lar deg velge mal og testdata basert på hva som er tilgjengelig).

```sh
./pdf-curler.sh
```

Man kan endre både testdata og handlebars partials for å se hvordan PDF-filene blir seendes ut uten å restarte
applikasjonen, men hvis man endrer på selve malene (altså filen som refereres til i endepunktet
`/genpdf/<app>/<template>`) så må `pdfgen` restartes.
Dette kan gjøres via Docker:

```sh
# Restart pdfgen etter endringer på malene
docker compose restart pdfgen
```

## Utvikling av nye maler

- Applikasjonen [startes lokalt via docker](../README.md#docker)
- Se dokumentasjon til [Handlebars](https://handlebarsjs.com/) for templates, syntax etc.
    - Det er også en
      del [hjelpefunksjoner](https://github.com/navikt/pdfgen-core?tab=readme-ov-file#handlerbars-helpers) tilgjengelig
      via pdfgen
- Standard mappestruktur er satt av [pdfgen](https://github.com/navikt/pdfgen?tab=readme-ov-file#getting-started)
    - Templates skal legges i `templates`-mappa med struktur `templates/<app>/<mal>.hbs`
    - Standard testdata kan legges til i `data`-mappa med samme strutur `data/<app>/<mal>.json` (om ønskelig)

Følgende endepunkt kan også benyttes til å laste ned PDF-filer lokal (som et alternativ til `pdf-curler.sh`):

```
GET /api/v1/genpdf/<app>/<mal>
```

## Produksjon

I produksjon er ikke oppsettet for mockdata skrudd på. Og den er tilgjengelig via tilgangskontroll definert i Nais.

Følgende endepunkt kan benyttes til å genere PDF-filer der `body` er et JSON-objekt som blir tilgjengelig under
template-prosesseringen.

```
POST /api/v1/genpdf/<app>/<mal>
{ body }
```
