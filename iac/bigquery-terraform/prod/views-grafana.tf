module "grafana_tilsagn_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "tilsagn_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode(
    [
      {
        name        = "id"
        type        = "STRING"
        mode        = "NULLABLE"
        description = "Unik id"
      },
      {
        name        = "gjennomforing_id"
        type        = "STRING"
        mode        = "NULLABLE"
        description = "Gjennomføring id"
      },
      {
        name        = "created_at"
        type        = "TIMESTAMP"
        mode        = "NULLABLE"
        description = "Tidspunkt for opprettelse"
      },
      {
        name        = "kostnadssted"
        type        = "STRING"
        mode        = "NULLABLE"
        description = "Kostnadssted"
      },
      {
        name        = "lopenummer"
        type        = "INTEGER"
        mode        = "NULLABLE"
        description = "Løpenummer"
      },
      {
        name        = "type"
        type        = "STRING"
        mode        = "NULLABLE"
        description = "Type"
      },
      {
        name        = "bestillingsnummer"
        type        = "STRING"
        mode        = "NULLABLE"
        description = "Bestillingsnummer"
      },
      {
        name        = "status"
        type        = "STRING"
        mode        = "NULLABLE"
        description = "Status"
      },
      {
        name        = "belop_gjenstaende"
        type        = "INTEGER"
        mode        = "NULLABLE"
        description = "Beløp gjenstående"
      },
      {
        name        = "belop_beregnet"
        type        = "INTEGER"
        mode        = "NULLABLE"
        description = "Beløp beregnet"
      },
      {
        name        = "prismodell"
        type        = "STRING"
        mode        = "NULLABLE"
        description = "Prismodell"
      },
      {
        name        = "bestilling_status"
        type        = "STRING"
        mode        = "NULLABLE"
        description = "Bestilling status"
      },
      {
        name        = "datastream_periode_start"
        type        = "DATE"
        mode        = "NULLABLE"
        description = "Datastream periode start"
      },
      {
        name        = "datastream_periode_slutt"
        type        = "DATE"
        mode        = "NULLABLE"
        description = "Datastream periode slutt"
      },
      {
        name        = "belop_brukt"
        type        = "INTEGER"
        mode        = "NULLABLE"
        description = "Beløp brukt"
      },
      {
        name        = "beregning_type"
        type        = "STRING"
        mode        = "NULLABLE"
        description = "Beregning type"
      },
      {
        name        = "kommentar"
        type        = "STRING"
        mode        = "NULLABLE"
        description = "Kommentar"
      },
      {
        name        = "antall_timer_oppfolging_per_deltaker"
        type        = "INTEGER"
        mode        = "NULLABLE"
        description = "Antall timer oppfølging per deltaker"
      },
      {
        name        = "beregning_antall_plasser"
        type        = "INTEGER"
        mode        = "NULLABLE"
        description = "Beregning antall plasser"
      },
      {
        name        = "tiltakstypeNavn"
        type        = "STRING"
        mode        = "NULLABLE"
        description = "Tiltakstypenavn"
      }
    ]
  )
  view_query = <<EOF
SELECT
  tilsagn.id,
  tilsagn.gjennomforing_id,
  tilsagn.created_at,
  tilsagn.kostnadssted,
  tilsagn.lopenummer,
  tilsagn.type,
  tilsagn.bestillingsnummer,
  tilsagn.status,
  tilsagn.belop_gjenstaende,
  tilsagn.belop_beregnet,
  tilsagn.prismodell,
  tilsagn.bestilling_status,
  tilsagn.datastream_periode_start,
  tilsagn.datastream_periode_slutt,
  tilsagn.belop_brukt,
  tilsagn.beregning_type,
  tilsagn.kommentar,
  tilsagn.antall_timer_oppfolging_per_deltaker,
  tilsagn.beregning_antall_plasser,
  tiltakstype.navn as tiltakstypeNavn
FROM `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_tilsagn` tilsagn
  INNER JOIN `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_gjennomforing` gjennomforing
    ON gjennomforing.id = tilsagn.gjennomforing_id
  INNER JOIN `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_tiltakstype` tiltakstype
    ON tiltakstype.id = gjennomforing.tiltakstype_id
EOF
}

module "grafana_utbetaling_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "utbetaling_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode([
    { name = "id",                             type = "STRING",    mode = "NULLABLE" },
    { name = "gjennomforing_id",               type = "STRING",    mode = "NULLABLE" },
    { name = "created_at",                     type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "godkjent_av_arrangor_tidspunkt", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "kontonummer",                    type = "STRING",    mode = "NULLABLE" },
    { name = "kid",                            type = "STRING",    mode = "NULLABLE" },
    { name = "journalpost_id",                 type = "STRING",    mode = "NULLABLE" },
    { name = "innsender",                      type = "STRING",    mode = "NULLABLE" },
    { name = "beskrivelse",                    type = "STRING",    mode = "NULLABLE" },
    { name = "tilskuddstype",                  type = "STRING",    mode = "NULLABLE" },
    { name = "begrunnelse_mindre_betalt",      type = "STRING",    mode = "NULLABLE" },
    { name = "belop_beregnet",                 type = "INTEGER",   mode = "NULLABLE" },
    { name = "beregning_type",                 type = "STRING",    mode = "NULLABLE" },
    { name = "datastream_periode_start",       type = "DATE",      mode = "NULLABLE" },
    { name = "datastream_periode_slutt",       type = "DATE",      mode = "NULLABLE" },
    { name = "status",                         type = "STRING",    mode = "NULLABLE" },
    { name = "avbrutt_aarsaker",               type = "JSON",      mode = "NULLABLE" },
    { name = "avbrutt_forklaring",             type = "STRING",    mode = "NULLABLE" },
    { name = "avbrutt_tidspunkt",              type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "tiltakstypeNavn",                type = "STRING",    mode = "NULLABLE" },
  ])
  view_query = <<EOF
SELECT
  utbetaling.id,
  utbetaling.gjennomforing_id,
  utbetaling.created_at,
  utbetaling.godkjent_av_arrangor_tidspunkt,
  utbetaling.kontonummer,
  utbetaling.kid,
  utbetaling.journalpost_id,
  utbetaling.innsender,
  utbetaling.beskrivelse,
  utbetaling.tilskuddstype,
  utbetaling.begrunnelse_mindre_betalt,
  utbetaling.belop_beregnet,
  utbetaling.beregning_type,
  utbetaling.datastream_periode_start,
  utbetaling.datastream_periode_slutt,
  utbetaling.status,
  utbetaling.avbrutt_aarsaker,
  utbetaling.avbrutt_forklaring,
  utbetaling.avbrutt_tidspunkt,
  tiltakstype.navn as tiltakstypeNavn
FROM `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_utbetaling` utbetaling
  INNER JOIN `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_gjennomforing` gjennomforing
    ON gjennomforing.id = utbetaling.gjennomforing_id
  INNER JOIN `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_tiltakstype` tiltakstype
    ON tiltakstype.id = gjennomforing.tiltakstype_id
EOF
}

module "grafana_delutbetaling_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "delutbetaling_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode([
    { name = "utbetaling_id",                   type = "STRING",    mode = "NULLABLE" },
    { name = "tilsagn_id",                      type = "STRING",    mode = "NULLABLE" },
    { name = "belop",                           type = "INTEGER",   mode = "NULLABLE" },
    { name = "created_at",                      type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "lopenummer",                      type = "INTEGER",   mode = "NULLABLE" },
    { name = "fakturanummer",                   type = "STRING",    mode = "NULLABLE" },
    { name = "sendt_til_okonomi_tidspunkt",     type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "id",                              type = "STRING",    mode = "NULLABLE" },
    { name = "gjor_opp_tilsagn",                type = "BOOLEAN",   mode = "NULLABLE" },
    { name = "status",                          type = "STRING",    mode = "NULLABLE" },
    { name = "faktura_status",                  type = "STRING",    mode = "NULLABLE" },
    { name = "faktura_status_sist_oppdatert",   type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "datastream_periode_start",        type = "DATE",      mode = "NULLABLE" },
    { name = "datastream_periode_slutt",        type = "DATE",      mode = "NULLABLE" },
    { name = "besluttet_av",                   type = "STRING",    mode = "NULLABLE" },
  ])
  view_query = <<EOF
SELECT
  delutbetaling.utbetaling_id,
  delutbetaling.tilsagn_id,
  delutbetaling.belop,
  delutbetaling.created_at,
  delutbetaling.lopenummer,
  delutbetaling.fakturanummer,
  delutbetaling.sendt_til_okonomi_tidspunkt,
  delutbetaling.id,
  delutbetaling.gjor_opp_tilsagn,
  delutbetaling.status,
  delutbetaling.faktura_status,
  delutbetaling.faktura_status_sist_oppdatert,
  delutbetaling.datastream_periode_start,
  delutbetaling.datastream_periode_slutt,
  totrinnskontroll.besluttet_av
FROM `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_delutbetaling` delutbetaling
  INNER JOIN `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_totrinnskontroll` totrinnskontroll
  ON (totrinnskontroll.entity_id = delutbetaling.id and totrinnskontroll.besluttelse = 'GODKJENT')
EOF
}
