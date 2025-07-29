# Gjennomføringer

[Link til datasettet på Datamarkedsplassen](https://data.ansatt.nav.no/dataproduct/48c6dab9-d236-4088-bb48-0a59007148c9/Arbeidsmarkedstiltak%20%28Valp%29/e21c664d-8890-4d64-988d-bd43343e7eb1)

View: `gjennomforing_view`

| Feltnavn              | Type          | Beskrivelse                                                                                                                                                                                                                               |
|-----------------------|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `id`                  | STRING (UUID) | ID til gjennomføringen                                                                                                                                                                                                                    |
| `tiltakstype_id`      | STRING (UUID) | [ID til tiltakstypen](tiltakstype_view.md)                                                                                                                                                                                                |
| `avtale_id`           | STRING (UUID) | [ID til avtalen](avtale_view.md)                                                                                                                                                                                                          |
| `tiltaksnummer`       | STRING        | Tiltaksnummer fra saken i Arena. Format: `<år>#<løpenummer>`                                                                                                                                                                              |
| `start_dato`          | DATE          | Start-datoen til gjennomføringen.                                                                                                                                                                                                         |
| `slutt_dato`          | DATE          | Slutt-datoen til gjennomføringen. Denne kan være åpen (null), ellers indikerer den siste dagen som gjennomføringen er pågående                                                                                                            |
| `status`              | STRING        | [Status til gjennomføringen](#gjennomføring-status)                                                                                                                                                                                       |
| `opprettet_tidspunkt` | TIMESTAMP     | Tidspunktet (UTC) som gjennomføringen ble opprettet (i databasen). Merk at dette tidspunktet ofte ikke samsvarer med når gjennomføringen initielt ble opprettet (gjelder bl.a. for alle gjennomføringer som har blitt overført fra Arena) |
| `oppdatert_tidspunkt` | TIMESTAMP     | Tidspunktet (UTC) som gjennomføringen sist ble oppdatert (i databasen)                                                                                                                                                                    |
| `avsluttet_tidspunkt` | TIMESTAMP     | Tidspunktet (UTC) som gjennomføringen ble avsluttet, null om den fortsatt er aktiv                                                                                                                                                        |

## Gjennomføring status

| Status         | Beskrivelse                                                                                         |
|----------------|-----------------------------------------------------------------------------------------------------|
| `GJENNOMFORES` | Løpende status fram til `slutt_dato` eller med mindre tiltaksadministrator avbryter gjennomføringen |
| `AVLYST`       | Tiltaksadministrator har avbrutt en gjennomføring før gjennomføringens `start dato`                 |
| `AVBRUTT`      | Tiltaksadministrator har avbrutt en gjennomføring etter gjennomføringens `start_dato`               |
| `AVSLUTTET`    | Gjennomføringen er avsluttet - dagens dato er gått over sin slutt dato                              |
