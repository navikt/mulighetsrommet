# Del med bruker

[Link til datasettet på Datamarkedsplassen](https://data.ansatt.nav.no/dataproduct/48c6dab9-d236-4088-bb48-0a59007148c9/Arbeidsmarkedstiltak%20%28Valp%29/db0c2c8e-51bf-42d9-acda-fdc52ec2497a)

View: `del_med_bruker_view`

## Beskrivelse

Datasettet inneholder data om hvilke tiltakstype som er delt med bruker via Arbeidsmarkedstiltak i Modia. I tillegg er
det registrert fylke som delte og lokalkontoret (enheten) som delte tiltaket.

| Feltnavn           | Type      | Beskrivelse                                                              |
|--------------------|-----------|--------------------------------------------------------------------------|
| `id`               | INTEGER   | ID til Del med bruker-raden                                              |
| `tiltakstype_navn` | STRING    | Lesbart navn for tiltakstypen koblet til tiltaket som er delt med bruker |
| `delt_fra_fylke`   | STRING    | Enhetsnummer for fylket til veileder som har delt med bruker             |
| `delt_fra_enhet`   | STRING    | Enhetsnummet for nav-kontoret til veileder som har delt med bruker       |
| `created_at`       | TIMESTAMP | Når tiltaket ble delt med bruker                                         |
