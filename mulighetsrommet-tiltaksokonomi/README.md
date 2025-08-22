# mulighetsrommet-tiltaksokonomi

Applikasjon for tiltaksøkonomi mellom Tiltaksadministrasjon og Oebs.

## Feilkoder fra Oebs og indikasjoner på hva det kan bety

- **PO_PDOI_INVALID_PROJ_INFO:** Det er noe feilkonfigurasjon hos Oebs
    - Mulig årsak kan være ugydlig tilsagnsår, altså at tilsagn blir forsøkt opprettet for langt frem i tid.
    - Krever endringer hos Oebs før feil evt. kan løses.
- **DUPLICATE INVOICE NUMBER (INVOICE):** Returneres når duplikat har blitt sendt/mottatt hos Oebs
    - Kan oppstå ved f.eks. nettverksproblemer og at http-requesten timer ut etter at faktura-melding har blitt
      skrevet/levert til grensesnittet. Siden tjenesten ikke med sikkerhet kan vite om en melding blir levert ved slike
      http-feil vil den forsøke på nytt, noe som kan resultere i duplikater.
    - Denne type feil burde løse seg selv etter at Oebs har håndtert/utbetalt den første av duplikatene.
