# Utdanninger

[Link til datasettet på Datamarkedsplassen](https://data.ansatt.nav.no/dataproduct/48c6dab9-d236-4088-bb48-0a59007148c9/Arbeidsmarkedstiltak%20%28Valp%29/8a4a3a4d-2a39-4b3c-a621-7e22178147c2)

View: `utdanningsprogram_view`

## Om datasettet

Kodeverket er basert på Nav-Eksport fra [api.utdanning.no](https://api.utdanning.no/) og knyttes til gjennomføringer via
tiltakstypen `GRUPPE_FAG_OG_YRKESOPPLAERING`.

Dataene blir oppdatert nattlig, derav daglig endring av `oppdatert_tidspunkt`.

| Feltnavn              | Type          | Beskrivelse                                                                                                 |
|-----------------------|---------------|-------------------------------------------------------------------------------------------------------------|
| `id`                  | STRING (UUID) | ID for utdanning                                                                                            |
| `navn`                | STRING        | Navn på utdanning                                                                                           |
| `nus_koder`           | JSON          | [Norsk standard for utdanningsgruppering koder](https://www.ssb.no/klass/klassifikasjoner/36)               |
| `programomradekode`   | STRING        | [Programområdets alfanumeriske kode](https://regbok.udir.no/35004/3344/35042-1021038.html) på 10 posisjoner |
| `sluttkompetanse`     | STRING        | Oppnådd sluttkompetanse ved fullført utdanning                                                              |
| `opprettet_tidspunkt` | TIMESTAMP     | Når (i UTC) utdanningen ble opprettet (i databasen)                                                         |
| `oppdatert_tidspunkt` | TIMESTAMP     | Når (i UTC) utdanningen sist ble oppdatert (i databasen)                                                    |
