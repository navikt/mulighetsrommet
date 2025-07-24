# Avtaler

[Link til datasettet på Datamarkedsplassen](https://data.ansatt.nav.no/dataproduct/48c6dab9-d236-4088-bb48-0a59007148c9/Arbeidsmarkedstiltak%20%28Valp%29/9be96259-b448-4710-891b-b0c386ba01a9)

View: `avtale_view`

| Feltnavn              | Type          | Beskrivelse                                                                                                                                                                                                         |
|-----------------------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `id`                  | STRING (UUID) | ID til avtalen                                                                                                                                                                                                      |
| `tiltakstype_id`      | STRING (UUID) | [ID til tiltakstypen](tiltakstype_view.md)                                                                                                                                                                          |
| `start_dato`          | DATE          | Start-datoen til avtalen                                                                                                                                                                                            |
| `slutt_dato`          | DATE          | Slutt-datoen til avtalen. Denne kan være åpen (null), ellers indikerer den siste dagen som avtalen er pågående                                                                                                      |
| `status`              | STRING        | [Status til avtalen](#avtale-status)                                                                                                                                                                                |
| `avtaletype`          | STRING        | Indikerer hvilken [type avtale](#avtaletype) som har blitt inngått.                                                                                                                                                 |
| `opprettet_tidspunkt` | TIMESTAMP     | Tidspunktet (i UTC) som avtalen ble opprettet (i databasen). Merk at dette tidspunktet ofte ikke samsvarer med når avtalen initielt ble opprettet (gjelder bl.a. for alle avtaler som har blitt overført fra Arena) |
| `oppdatert_tidspunkt` | TIMESTAMP     | Tidspunktet (i UTC) som avtalen sist ble oppdatert (i databasen)                                                                                                                                                    |
| `avbrutt_tidspunkt`   | TIMESTAMP     | Indikerer (i UTC) om avtalen har blitt avbrutt eller ikke                                                                                                                                                           |

## Avtale status

| Navn        | Beskrivelse                                                 |
|-------------|-------------------------------------------------------------|
| `UTKAST`    | Avtalen er delvis utfylt, eller er ikke satt aktiv          |
| `AKTIV`     | Avtalen er aktiv                                            |
| `AVBRUTT`   | Tiltaksadministrator har avsluttet avtalen før `slutt_dato` |
| `AVSLUTTET` | Avtalen har passert `slutt_dato` og automatisk avsluttes    |

## Avtaletype

| Navn                  | Beskrivelse                                                 |
|-----------------------|-------------------------------------------------------------|
| `RAMMEAVTALE`         | Offentlig anskaffet rammeavtale                             |
| `FORHANDSGODKJENT`    | Avtaler med forhåndsgodkjente tiltaksleverandører           |
| `OFFENTLIG_OFFENTLIG` | Om dette er en avtale med offentlig-offentlig samarbeid     |
| `AVTALE`              | Enkelte avtaler som ikke faller under resten av kategoriene |
