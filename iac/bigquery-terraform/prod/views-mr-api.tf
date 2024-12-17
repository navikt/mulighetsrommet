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

module "mr_api_tiltaksgjennomforing_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = module.mr_api_datastream.dataset_id
  view_id             = "tiltaksgjennomforing_view"
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
  tiltaksnummer,
  avsluttet_tidspunkt,
  start_dato,
  slutt_dato
FROM `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_tiltaksgjennomforing`
WHERE slutt_dato is null or slutt_dato >= DATE '2018-01-01'
EOF
}

module "mr_api_tiltaksgjennomforing_opphav_antall_opprettet_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = module.mr_api_datastream.dataset_id
  view_id             = "tiltaksgjennomforing_opphav_antall_opprettet_view"
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
from `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_tiltaksgjennomforing` gjennomforing
         join `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_tiltakstype` tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
group by tiltakstype.navn, gjennomforing.opphav
order by tiltakstype.navn
EOF
}
