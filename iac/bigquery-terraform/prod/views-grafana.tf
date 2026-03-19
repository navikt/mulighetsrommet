module "grafana_tilsagn_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "tilsagn_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode(
    [
      { name = "id", type = "STRING", mode = "NULLABLE" },
      { name = "gjennomforing_id", type = "STRING", mode = "NULLABLE" },
      { name = "created_at", type = "TIMESTAMP", mode = "NULLABLE" },
      { name = "updated_at", type = "TIMESTAMP", mode = "NULLABLE" },
      { name = "kostnadssted", type = "STRING", mode = "NULLABLE" },
      { name = "lopenummer", type = "INTEGER", mode = "NULLABLE" },
      { name = "tilsagn_type", type = "STRING", mode = "NULLABLE" },
      { name = "status", type = "STRING", mode = "NULLABLE" },
      { name = "bestillingsnummer", type = "STRING", mode = "NULLABLE" },
      { name = "bestilling_status", type = "STRING", mode = "NULLABLE" },
      { name = "datastream_periode_start", type = "DATE", mode = "NULLABLE" },
      { name = "datastream_periode_slutt", type = "DATE", mode = "NULLABLE" },
      { name = "beregning_type", type = "STRING", mode = "NULLABLE" },
      { name = "beregning_antall_timer_oppfolging_per_deltaker", type = "INTEGER", mode = "NULLABLE" },
      { name = "beregning_antall_plasser", type = "INTEGER", mode = "NULLABLE" },
      { name = "beregning_sats", type = "INTEGER", mode = "NULLABLE" },
      { name = "belop_beregnet", type = "INTEGER", mode = "NULLABLE" },
      { name = "belop_brukt", type = "INTEGER", mode = "NULLABLE" },
      { name = "valuta", type = "STRING", mode = "NULLABLE" },
      { name = "tiltakstype_navn", type = "STRING", mode = "NULLABLE" },
    ]
  )
  view_query = <<EOF
SELECT
  tilsagn.id,
  tilsagn.gjennomforing_id,
  tilsagn.created_at,
  tilsagn.updated_at,
  tilsagn.kostnadssted,
  tilsagn.lopenummer,
  tilsagn.tilsagn_type,
  tilsagn.status,
  tilsagn.bestillingsnummer,
  tilsagn.bestilling_status,
  tilsagn.datastream_periode_start,
  tilsagn.datastream_periode_slutt,
  tilsagn.beregning_type,
  tilsagn.beregning_antall_timer_oppfolging_per_deltaker,
  tilsagn.beregning_antall_plasser,
  tilsagn.beregning_sats,
  tilsagn.belop_beregnet,
  tilsagn.belop_brukt,
  tilsagn.valuta,
  tiltakstype.navn as tiltakstype_navn
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
    { name = "id", type = "STRING", mode = "NULLABLE" },
    { name = "gjennomforing_id", type = "STRING", mode = "NULLABLE" },
    { name = "created_at", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "updated_at", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "tilskuddstype", type = "STRING", mode = "NULLABLE" },
    { name = "datastream_periode_start", type = "DATE", mode = "NULLABLE" },
    { name = "datastream_periode_slutt", type = "DATE", mode = "NULLABLE" },
    { name = "status", type = "STRING", mode = "NULLABLE" },
    { name = "avbrutt_tidspunkt", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "innsendt_av_arrangor_tidspunkt", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "utbetales_tidligst_tidspunkt", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "beregning_type", type = "STRING", mode = "NULLABLE" },
    { name = "belop_beregnet", type = "INTEGER", mode = "NULLABLE" },
    { name = "valuta", type = "STRING", mode = "NULLABLE" },
    { name = "tiltakstype_navn", type = "STRING", mode = "NULLABLE" },
  ])
  view_query = <<EOF
SELECT
  utbetaling.id,
  utbetaling.gjennomforing_id,
  utbetaling.created_at,
  utbetaling.updated_at,
  utbetaling.tilskuddstype,
  utbetaling.datastream_periode_start,
  utbetaling.datastream_periode_slutt,
  utbetaling.status,
  utbetaling.avbrutt_tidspunkt,
  utbetaling.innsendt_av_arrangor_tidspunkt,
  utbetaling.utbetales_tidligst_tidspunkt,
  utbetaling.beregning_type,
  utbetaling.belop_beregnet,
  utbetaling.valuta,
  tiltakstype.navn as tiltakstype_navn
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
    { name = "id", type = "STRING", mode = "NULLABLE" },
    { name = "utbetaling_id", type = "STRING", mode = "NULLABLE" },
    { name = "tilsagn_id", type = "STRING", mode = "NULLABLE" },
    { name = "created_at", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "updated_at", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "lopenummer", type = "INTEGER", mode = "NULLABLE" },
    { name = "gjor_opp_tilsagn", type = "BOOLEAN", mode = "NULLABLE" },
    { name = "status", type = "STRING", mode = "NULLABLE" },
    { name = "fakturanummer", type = "STRING", mode = "NULLABLE" },
    { name = "faktura_sendt_tidspunkt", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "faktura_status_endret_tidspunkt", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "faktura_status", type = "STRING", mode = "NULLABLE" },
    { name = "datastream_periode_start", type = "DATE", mode = "NULLABLE" },
    { name = "datastream_periode_slutt", type = "DATE", mode = "NULLABLE" },
    { name = "belop", type = "INTEGER", mode = "NULLABLE" },
    { name = "valuta", type = "STRING", mode = "NULLABLE" },
    { name = "besluttet_av", type = "STRING", mode = "NULLABLE" },
  ])
  view_query = <<EOF
SELECT
  delutbetaling.id,
  delutbetaling.utbetaling_id,
  delutbetaling.tilsagn_id,
  delutbetaling.created_at,
  delutbetaling.updated_at,
  delutbetaling.lopenummer,
  delutbetaling.gjor_opp_tilsagn,
  delutbetaling.status,
  delutbetaling.fakturanummer,
  delutbetaling.faktura_sendt_tidspunkt,
  delutbetaling.faktura_status_endret_tidspunkt,
  delutbetaling.faktura_status,
  delutbetaling.datastream_periode_start,
  delutbetaling.datastream_periode_slutt,
  delutbetaling.belop,
  delutbetaling.valuta,
  totrinnskontroll.besluttet_av
FROM `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_delutbetaling` delutbetaling
  INNER JOIN `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_totrinnskontroll` totrinnskontroll
  ON (totrinnskontroll.entity_id = delutbetaling.id and totrinnskontroll.besluttelse = 'GODKJENT')
EOF
}

module "grafana_avtale_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "avtale_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode([
    { name = "id", type = "STRING", mode = "NULLABLE" },
    { name = "tiltakstype_id", type = "STRING", mode = "NULLABLE" },
    { name = "avtalenummer", type = "STRING", mode = "NULLABLE" },
    { name = "start_dato", type = "DATE", mode = "NULLABLE" },
    { name = "slutt_dato", type = "DATE", mode = "NULLABLE" },
    { name = "status", type = "STRING", mode = "NULLABLE" },
    { name = "avtaletype", type = "STRING", mode = "NULLABLE" },
    { name = "created_at", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "updated_at", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "arrangor_hovedenhet_id", type = "STRING", mode = "NULLABLE" },
    { name = "lopenummer", type = "STRING", mode = "NULLABLE" },
    { name = "personvern_bekreftet", type = "BOOLEAN", mode = "NULLABLE" },
    { name = "sakarkiv_nummer", type = "STRING", mode = "NULLABLE" },
    { name = "avbrutt_tidspunkt", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "avbrutt_aarsaker", type = "JSON", mode = "NULLABLE" },
    { name = "opsjon_maks_varighet", type = "DATE", mode = "NULLABLE" },
    { name = "opsjonsmodell", type = "STRING", mode = "NULLABLE" },
    { name = "opsjon_custom_opsjonsmodell_navn", type = "STRING", mode = "NULLABLE" },
    { name = "tiltakstype_navn", type = "STRING", mode = "NULLABLE" },
  ])
  view_query = <<EOF
SELECT
  avtale.id,
  avtale.tiltakstype_id,
  avtale.avtalenummer,
  avtale.start_dato,
  avtale.slutt_dato,
  avtale.status,
  avtale.avtaletype,
  avtale.created_at,
  avtale.updated_at,
  avtale.arrangor_hovedenhet_id,
  avtale.lopenummer,
  avtale.personvern_bekreftet,
  avtale.sakarkiv_nummer,
  avtale.avbrutt_tidspunkt,
  avtale.avbrutt_aarsaker,
  avtale.opsjon_maks_varighet,
  avtale.opsjonsmodell,
  avtale.opsjon_custom_opsjonsmodell_navn,
  tiltakstype.navn as tiltakstype_navn
FROM `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_avtale` avtale
  INNER JOIN `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_tiltakstype` tiltakstype
    ON tiltakstype.id = avtale.tiltakstype_id
EOF
}

module "grafana_gjennomforing_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "gjennomforing_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode([
    { name = "id", type = "STRING", mode = "NULLABLE" },
    { name = "tiltakstype_id", type = "STRING", mode = "NULLABLE" },
    { name = "avtale_id", type = "STRING", mode = "NULLABLE" },
    { name = "arrangor_id", type = "STRING", mode = "NULLABLE" },
    { name = "arena_tiltaksnummer", type = "STRING", mode = "NULLABLE" },
    { name = "lopenummer", type = "STRING", mode = "NULLABLE" },
    { name = "created_at", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "updated_at", type = "TIMESTAMP", mode = "NULLABLE" },
    { name = "start_dato", type = "DATE", mode = "NULLABLE" },
    { name = "slutt_dato", type = "DATE", mode = "NULLABLE" },
    { name = "status", type = "STRING", mode = "NULLABLE" },
    { name = "gjennomforing_type", type = "STRING", mode = "NULLABLE" },
    { name = "antall_plasser", type = "INTEGER", mode = "NULLABLE" },
    { name = "oppstart", type = "STRING", mode = "NULLABLE" },
    { name = "pamelding_type", type = "STRING", mode = "NULLABLE" },
    { name = "publisert", type = "BOOLEAN", mode = "NULLABLE" },
    { name = "sted_for_gjennomforing", type = "STRING", mode = "NULLABLE" },
    { name = "nav_region", type = "STRING", mode = "NULLABLE" },
    { name = "apent_for_pamelding", type = "BOOLEAN", mode = "NULLABLE" },
    { name = "deltidsprosent", type = "NUMERIC", mode = "NULLABLE" },
    { name = "estimert_ventetid_verdi", type = "INTEGER", mode = "NULLABLE" },
    { name = "estimert_ventetid_enhet", type = "STRING", mode = "NULLABLE" },
    { name = "avbrutt_aarsaker", type = "JSON", mode = "NULLABLE" },
    { name = "tilgjengelig_for_arrangor_dato", type = "DATE", mode = "NULLABLE" },
    { name = "prismodell_type", type = "STRING", mode = "NULLABLE" },
    { name = "tiltakstype_navn", type = "STRING", mode = "NULLABLE" },
  ])
  view_query = <<EOF
SELECT
  gjennomforing.id,
  gjennomforing.tiltakstype_id,
  gjennomforing.avtale_id,
  gjennomforing.arrangor_id,
  gjennomforing.arena_tiltaksnummer,
  gjennomforing.lopenummer,
  gjennomforing.created_at,
  gjennomforing.updated_at,
  gjennomforing.start_dato,
  gjennomforing.slutt_dato,
  gjennomforing.status,
  gjennomforing.gjennomforing_type,
  gjennomforing.antall_plasser,
  gjennomforing.oppstart,
  gjennomforing.pamelding_type,
  gjennomforing.publisert,
  gjennomforing.sted_for_gjennomforing,
  gjennomforing.nav_region,
  gjennomforing.apent_for_pamelding,
  gjennomforing.deltidsprosent,
  gjennomforing.estimert_ventetid_verdi,
  gjennomforing.estimert_ventetid_enhet,
  gjennomforing.avbrutt_aarsaker,
  gjennomforing.tilgjengelig_for_arrangor_dato,
  prismodell.prismodell_type,
  tiltakstype.navn as tiltakstype_navn
FROM `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_gjennomforing` gjennomforing
  INNER JOIN `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_tiltakstype` tiltakstype
    ON tiltakstype.id = gjennomforing.tiltakstype_id
  INNER JOIN `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_prismodell` prismodell
    ON prismodell.id = gjennomforing.prismodell_id
EOF
}
