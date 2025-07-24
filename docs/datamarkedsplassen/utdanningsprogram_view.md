# Utdanningsprogram - kodeverk

[Link til datasettet på Datamarkedsplassen](https://data.ansatt.nav.no/dataproduct/48c6dab9-d236-4088-bb48-0a59007148c9/Arbeidsmarkedstiltak%20%28Valp%29/71edccf5-ad70-4b16-9f0f-77825e469423)

View: `utdanningsprogram_view`

## Om datasettet

Kodeverket er basert på Nav-Eksport fra [api.utdanning.no](https://api.utdanning.no/) og knyttes
til [gjennomføringer](gjennomforing_view.md) via
[tiltakstypen `GRUPPE_FAG_OG_YRKESOPPLAERING`](tiltakstype_view.md).

Dataene blir oppdatert nattlig, derav daglig endring av `oppdatert_tidspunkt`.

| Feltnavn                 | Type          | Beskrivelse                                                                                                 |
|--------------------------|---------------|-------------------------------------------------------------------------------------------------------------|
| `id`                     | STRING (UUID) | ID for utdanningsprogrammet                                                                                 |
| `navn`                   | STRING        | Navn på utdanningsprogram                                                                                   |
| `nus_koder`              | JSON          | [Norsk standard for utdanningsgruppering koder](https://www.ssb.no/klass/klassifikasjoner/36)               |
| `programomradekode`      | STRING        | [Programområdets alfanumeriske kode](https://regbok.udir.no/35004/3344/35042-1021038.html) på 10 posisjoner |
| `utdanningsprogram_type` | STRING        | Overordnet type utdanningsprogram                                                                           |
| `opprettet_tidspunkt`    | TIMESTAMP     | Når (i UTC) utdanningsprogrammet ble opprettet (i databasen)                                                |
| `oppdatert_tidspunkt`    | TIMESTAMP     | Når (i UTC) utdanningsprogrammet ble sist oppdatert (i databasen)                                           |
