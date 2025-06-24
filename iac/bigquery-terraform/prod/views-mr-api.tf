module "mr_api_tiltakstype_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = module.mr_api_datastream.dataset_id
  view_id             = "tiltakstype_view"
  view_schema = jsonencode(
    [
      {
        mode        = "NULLABLE"
        name        = "id"
        type        = "STRING"
        description = "Unik ID for tiltakstypen."
      },
      {
        mode        = "NULLABLE"
        name        = "navn"
        type        = "STRING"
        description = "Navn til tiltakstypen."
      },
      {
        mode        = "NULLABLE"
        name        = "tiltakskode"
        type        = "STRING"
        description = "Tiltakskode hos Valp."
      },
      {
        mode        = "NULLABLE"
        name        = "arena_tiltakskode"
        type        = "STRING"
        description = "Tiltakskode fra Arena."
      },
    ]
  )
  view_query = <<EOF
SELECT
  id,
  navn,
  tiltakskode,
  arena_kode as arena_tiltakskode
FROM `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_tiltakstype`
WHERE tiltakskode IS NOT NULL
EOF
}

module "mr_api_avtale_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = module.mr_api_datastream.dataset_id
  view_id             = "avtale_view"
  view_schema = jsonencode(
    [
      {
        mode        = "NULLABLE"
        name        = "id"
        type        = "STRING"
        description = "ID til avtalen."
      },
      {
        mode        = "NULLABLE"
        name        = "tiltakstype_id"
        type        = "STRING"
        description = "ID til tiltakstypen."
      },
      {
        mode        = "NULLABLE"
        name        = "start_dato"
        type        = "DATE"
        description = "Start-datoen til avtalen."
      },
      {
        mode        = "NULLABLE"
        name        = "slutt_dato"
        type        = "DATE"
        description = "Slutt-datoen til avtalen. Denne kan være åpen (null), ellers indikerer den siste dagen som avtalen er pågående."
      },
      {
        mode        = "NULLABLE"
        name        = "avtaletype"
        type        = "STRING"
        description = <<EOF
Indikerer hvilken type avtale som har blitt inngått.

RAMMEAVTALE: Offentlig anskaffet rammeavtale.
FORHANDSGODKJENT: Avtaler med forhåndsgodkjente tiltaksleverandører.
OFFENTLIG_OFFENTLIG: Om dette er en avtale med offentlig-offentlig samarbeid.
AVTALE: Enkelte avtaler som ikke faller under resten av kategoriene.
EOF
      },
      {
        mode        = "NULLABLE"
        name        = "opprettet_tidspunkt"
        type        = "TIMESTAMP"
        description = "Tidspunktet som avtalen ble opprettet (i databasen). Merk at dette tidspunktet ofte ikke samsvarer med når avtalen initielt ble opprettet (gjelder bl.a. for alle avtaler som har blitt overført fra Arena)."
      },
      {
        mode        = "NULLABLE"
        name        = "oppdatert_tidspunkt"
        type        = "TIMESTAMP"
        description = "Tidspunktet som avtalen sist ble oppdatert (i databasen)."
      },
      {
        mode        = "NULLABLE"
        name        = "status"
        type        = "STRING"
        description = "Status til avtalen. Dette kan være en av følgende verdier: 'UTKAST', 'AKTIV', 'AVBRUTT', 'AVSLUTTET'."
      },
      {
        mode        = "NULLABLE"
        name        = "avbrutt_tidspunkt"
        type        = "TIMESTAMP"
        description = "Indikerer om avtalen har blitt avbrutt eller ikke."
      },
    ]
  )
  view_query = <<EOF
SELECT
  id,
  tiltakstype_id,
  start_dato,
  slutt_dato,
  avtaletype,
  created_at as opprettet_tidspunkt,
  updated_at as oppdatert_tidspunkt,
  avbrutt_tidspunkt
FROM `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_avtale`
EOF
}

module "mr_api_gjennomforing_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = module.mr_api_datastream.dataset_id
  view_id             = "gjennomforing_view"
  view_schema = jsonencode(
    [
      {
        mode        = "NULLABLE"
        name        = "id"
        type        = "STRING"
        description = "ID til gjennomføringen."
      },
      {
        mode        = "NULLABLE"
        name        = "tiltakstype_id"
        type        = "STRING"
        description = "ID til tiltakstypen."
      },
      {
        mode        = "NULLABLE"
        name        = "avtale_id"
        type        = "STRING"
        description = "ID til avtalen."
      },
      {
        mode        = "NULLABLE"
        name        = "tiltaksnummer"
        type        = "STRING"
        description = "Tiltaksnummer fra saken i Arena. Format: `<år>#<løpenummer>`."
      },
      {
        mode        = "NULLABLE"
        name        = "start_dato"
        type        = "DATE"
        description = "Start-datoen til gjennomføringen."
      },
      {
        mode        = "NULLABLE"
        name        = "slutt_dato"
        type        = "DATE"
        description = "Slutt-datoen til gjennomføringen. Denne kan være åpen (null), ellers indikerer den siste dagen som gjennomføringen er pågående."
      },
      {
        mode        = "NULLABLE"
        name        = "opprettet_tidspunkt"
        type        = "TIMESTAMP"
        description = "Tidspunktet som gjennomføringen ble opprettet (i databasen). Merk at dette tidspunktet ofte ikke samsvarer med når gjennomføringen initielt ble opprettet (gjelder bl.a. for alle gjennomføringer som har blitt overført fra Arena)."
      },
      {
        mode        = "NULLABLE"
        name        = "oppdatert_tidspunkt"
        type        = "TIMESTAMP"
        description = "Tidspunktet som gjennomføringen sist ble oppdatert (i databasen)."
      },
      {
        mode        = "NULLABLE"
        name        = "status"
        type        = "STRING"
        description = "Status til gjennomføringen. Dette kan være en av følgende verdier: 'GJENNOMFORES', 'AVLYST', 'AVBRUTT', 'AVSLUTTET'."
      },
      {
        mode        = "NULLABLE"
        name        = "avsluttet_tidspunkt"
        type        = "TIMESTAMP"
        description = "Tidspunktet som gjennomføringen ble avsluttet, null om den fortsatt er aktiv."
      },
    ]
  )
  view_query = <<EOF
SELECT
  id,
  tiltakstype_id,
  avtale_id,
  tiltaksnummer,
  start_dato,
  slutt_dato,
  created_at as opprettet_tidspunkt,
  updated_at as oppdatert_tidspunkt,
  avsluttet_tidspunkt
FROM `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_gjennomforing`
EOF
}

module "mr_api_gjennomforing_opphav_antall_opprettet_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = module.mr_api_datastream.dataset_id
  view_id             = "gjennomforing_opphav_antall_opprettet_view"
  view_schema = jsonencode(
    [
      {
        mode        = "NULLABLE"
        name        = "navn"
        type        = "STRING"
        description = "Navn på tiltakstype."
      },
      {
        mode        = "NULLABLE"
        name        = "opphav"
        type        = "STRING"
        description = "Hvilket system som gjennomføringen ble opprettet i."
      },
      {
        mode        = "NULLABLE"
        name        = "antall_opprettet"
        type        = "INTEGER"
        description = "Antall gjennomføringer per opphav."
      },
    ]
  )
  view_query = <<EOF
select
    tiltakstype.navn,
    gjennomforing.opphav,
    count(*) as antall_opprettet
from `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_gjennomforing` gjennomforing
         join `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_tiltakstype` tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
group by tiltakstype.navn, gjennomforing.opphav
order by tiltakstype.navn
EOF
}

module "mr_api_gjennomforing_nav_enhet_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = module.mr_api_datastream.dataset_id
  view_id             = "gjennomforing_nav_enhet_view"
  view_schema = jsonencode(
    [
      {
        mode        = "NULLABLE"
        name        = "gjennomforing_id"
        type        = "STRING"
        description = "ID til gjennomføringen."
      },
      {
        mode        = "NULLABLE"
        name        = "enhetsnummer"
        type        = "STRING"
        description = "Enhetsnummeret til gjennomføringen."
      },
    ]
  )
  view_query = <<EOF
SELECT
  gjennomforing_id,
  enhetsnummer,
FROM `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_gjennomforing_nav_enhet`
EOF
}

module "mr_api_avtale_nav_enhet_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = module.mr_api_datastream.dataset_id
  view_id             = "avtale_nav_enhet_view"
  view_schema = jsonencode(
    [
      {
        mode        = "NULLABLE"
        name        = "avtale_id"
        type        = "STRING"
        description = "ID til avtalen."
      },
      {
        mode        = "NULLABLE"
        name        = "enhetsnummer"
        type        = "STRING"
        description = "Enhetsnummeret til avtalen."
      },
    ]
  )
  view_query = <<EOF
SELECT
  avtale_id,
  enhetsnummer,
FROM `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_avtale_nav_enhet`
EOF
}

module "mr_api_del_med_bruker_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = module.mr_api_datastream.dataset_id
  view_id             = "del_med_bruker_view"
  view_schema = jsonencode(
    [
      {
        mode        = "NULLABLE"
        name        = "id"
        type        = "INTEGER"
        description = "ID til Del med bruker-raden."
      },
      {
        mode        = "NULLABLE"
        name        = "tiltakstype_navn"
        type        = "STRING"
        description = "Lesbart navn for tiltakstypen koblet til tiltaket som er delt med bruker"
      },
      {
        mode        = "NULLABLE"
        name        = "delt_fra_fylke"
        type        = "STRING"
        description = "Enhetsnummer for fylket til veileder som har delt med bruker"
      },
      {
        mode        = "NULLABLE"
        name        = "delt_fra_enhet"
        type        = "STRING"
        description = "Enhetsnummet for nav-kontoret til veileder som har delt med bruker"
      },
      {
        mode        = "NULLABLE"
        name        = "created_at"
        type        = "TIMESTAMP"
        description = "Når tiltaket ble delt med bruker"
      },
    ]
  )
  view_query = <<EOF
SELECT
  id,
  tiltakstype_navn,
  delt_fra_fylke,
  delt_fra_enhet,
  created_at
FROM `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_del_med_bruker`
WHERE delt_fra_fylke IS NOT NULL
EOF
}
