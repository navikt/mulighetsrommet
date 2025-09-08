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

module "grafana_tilsagn_type_antall_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "tilsagn_type_antall_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode(
    [
      {
        mode        = "NULLABLE"
        name        = "type"
        type        = "STRING"
        description = "Type av tilsagn"
      },
      {
        mode        = "NULLABLE"
        name        = "antall"
        type        = "INTEGER"
        description = "Antall av tilsagns typen"
      },
    ]
  )
  view_query = <<EOF
SELECT
  type,
  COUNT(*) AS antall
FROM
  `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_tilsagn`
GROUP BY
  type
ORDER BY
  antall desc
EOF
}

module "grafana_tilsagn_status_antall_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "tilsagn_status_antall_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode(
    [
      {
        mode        = "NULLABLE"
        name        = "status"
        type        = "STRING"
        description = "Status av tilsagn"
      },
      {
        mode        = "NULLABLE"
        name        = "antall_type_tilsagn"
        type        = "INTEGER"
        description = "Antall av typen TILSAGN"
      },
      {
        mode        = "NULLABLE"
        name        = "antall_type_ekatratilsagn"
        type        = "INTEGER"
        description = "Antall av typen EKSTRATILSAGN"
      },
      {
        mode        = "NULLABLE"
        name        = "antall_type_investering"
        type        = "INTEGER"
        description = "Antall av typen INVESTERING"
      },
    ]
  )
  view_query = <<EOF
SELECT
  status,
  COUNT(CASE
      WHEN type = 'TILSAGN' THEN 1
      ELSE NULL
  END
    ) AS antall_type_tilsagn,
  COUNT(CASE
      WHEN type = 'EKSTRATILSAGN' THEN 1
      ELSE NULL
  END
    ) AS antall_type_ekatratilsagn,
  COUNT(CASE
      WHEN type = 'INVESTERING' THEN 1
      ELSE NULL
  END
    ) AS antall_type_investering,
FROM
  `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_tilsagn`
GROUP BY
  status
EOF
}

module "grafana_tilsagn_nye_siste_maaned_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "tilsagn_nye_siste_maaned_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode(
    [
      {
        mode        = "NULLABLE"
        name        = "created_date"
        type        = "DATE"
        description = "Dato for opprettelse"
      },
      {
        mode        = "NULLABLE"
        name        = "antall_type_tilsagn"
        type        = "INTEGER"
        description = "Antall av typen TILSAGN"
      },
      {
        mode        = "NULLABLE"
        name        = "antall_type_ekatratilsagn"
        type        = "INTEGER"
        description = "Antall av typen EKSTRATILSAGN"
      },
      {
        mode        = "NULLABLE"
        name        = "antall_type_investering"
        type        = "INTEGER"
        description = "Antall av typen INVESTERING"
      },
    ]
  )
  view_query = <<EOF
WITH
  date_range AS (
  SELECT
    CURRENT_DATE() AS today_date,
    DATE_SUB(CURRENT_DATE(), INTERVAL 1 MONTH) AS start_date ),
  dates_past_month AS (
  SELECT
    DATE_ADD(start_date, INTERVAL day_offset DAY) AS generated_date
  FROM
    date_range,
    UNNEST(GENERATE_ARRAY(0, DATE_DIFF(today_date, start_date, DAY))) AS day_offset
  ORDER BY
    generated_date DESC)
SELECT
  d.generated_date AS created_date,
  COUNT(CASE
      WHEN type = 'TILSAGN' THEN 1
      ELSE NULL
  END
    ) AS antall_type_tilsagn,
  COUNT(CASE
      WHEN type = 'EKSTRATILSAGN' THEN 1
      ELSE NULL
  END
    ) AS antall_type_ekatratilsagn,
  COUNT(CASE
      WHEN type = 'INVESTERING' THEN 1
      ELSE NULL
  END
    ) AS antall_type_investering,
FROM
  dates_past_month d
FULL JOIN
  `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_tilsagn` t
ON
  DATE(t.created_at) = d.generated_date
  AND DATE(t.created_at) >= d.generated_date
WHERE
  d.generated_date IS NOT NULL
GROUP BY
  created_date
ORDER BY
  created_date
EOF
}

module "grafana_tilsagn_feilet_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "tilsagn_feilet_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode(
    [
      {
        mode = "NULLABLE"
        name = "bestillingsnummer"
        type = "STRING"
      },
      {
        mode = "NULLABLE"
        name = "type"
        type = "STRING"
      },
      {
        mode = "NULLABLE"
        name = "status"
        type = "STRING"
      },
      {
        mode = "NULLABLE"
        name = "bestilling_status"
        type = "STRING"
      },
    ]
  )
  view_query = <<EOF
SELECT
  bestillingsnummer,
  type,
  status,
  bestilling_status
FROM
  `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_tilsagn`
WHERE
  bestilling_status = 'FEILET'
EOF
}

module "grafana_utbetaling_arrangor_innsending_stats_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "utbetaling_arrangor_innsending_stats_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode(
    [
      {
        mode = "NULLABLE"
        name = "tilskuddstype"
        type = "STRING"
      },
      {
        mode = "NULLABLE"
        name = "total_rows"
        type = "INTEGER"
      },
      {
        mode = "NULLABLE"
        name = "avg_days"
        type = "FLOAT"
      },
      {
        mode = "NULLABLE"
        name = "min_days"
        type = "INTEGER"
      },
      {
        mode = "NULLABLE"
        name = "max_days"
        type = "INTEGER"
      },
      {
        mode = "NULLABLE"
        name = "stddev_days"
        type = "FLOAT"
      },
      {
        mode = "NULLABLE"
        name = "median_days"
        type = "INTEGER"
      },
    ]
  )
  view_query = <<EOF
 WITH
  arrangor_innsendt_utbetalinger as (select
      tilskuddstype,
      DATE(created_at) as date_created,
      DATE(godkjent_av_arrangor_tidspunkt) as date_arrangor_godkjent,
      DATE_DIFF(godkjent_av_arrangor_tidspunkt, created_at, DAY) as antall_dager_mellom
    FROM
      `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_utbetaling`
    WHERE
      innsender = "Arrangor" and godkjent_av_arrangor_tidspunkt is not null
  )
SELECT
  tilskuddstype,
  COUNT(*) AS total_rows,
  AVG(antall_dager_mellom) AS avg_days,
  MIN(antall_dager_mellom) AS min_days,
  MAX(antall_dager_mellom) AS max_days,
  STDDEV(antall_dager_mellom) AS stddev_days,
  APPROX_QUANTILES(antall_dager_mellom, 2)[OFFSET(1)] AS median_days
FROM
  arrangor_innsendt_utbetalinger
group by
  tilskuddstype
EOF
}

module "grafana_utbetaling_arrangor_utestaende_innsendinger_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "utbetaling_arrangor_utestaende_innsendinger_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode(
    [
      {
        mode = "NULLABLE"
        name = "dag"
        type = "DATE"
      },
      {
        mode = "NULLABLE"
        name = "antall_utestaende_innsendinger"
        type = "INTEGER"
      },
    ]
  )
  view_query = <<EOF
WITH dager AS (
  SELECT
    dag
  FROM UNNEST(
    GENERATE_DATE_ARRAY(
      DATE_SUB(CURRENT_DATE(), INTERVAL 2 MONTH),
      CURRENT_DATE(),
      INTERVAL 1 DAY
    )
  ) AS dag
),
arrangor_utbetalinger as (
  SELECT
    u.id,
    DATE(u.created_at) as created_at,
    DATE(u.godkjent_av_arrangor_tidspunkt) as godkjent_av_arrangor_tidspunkt
  FROM `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_utbetaling` u
  WHERE
    (u.innsender IS NULL OR u.innsender = 'Arrangor')
)
SELECT
  d.dag,
  COUNT(u.id) AS antall_utestaende_innsendinger
FROM
  dager d
LEFT JOIN arrangor_utbetalinger u
  ON (u.created_at <= d.dag)
     AND (u.godkjent_av_arrangor_tidspunkt IS NULL OR u.godkjent_av_arrangor_tidspunkt > d.dag)
GROUP BY 1
ORDER BY 1 DESC
EOF
}

module "grafana_utbetaling_antall_godkjente_per_prosess_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "utbetaling_antall_godkjente_per_prosess_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode(
    [
      {
        mode = "NULLABLE"
        name = "dag"
        type = "DATE"
      },
      {
        mode = "NULLABLE"
        name = "prosess"
        type = "STRING"
      },
      {
        mode = "NULLABLE"
        name = "totalt_antall_godkjente"
        type = "INTEGER"
      },
    ]
  )
  view_query = <<EOF
WITH
  dager AS (
  SELECT
    dag
  FROM
    UNNEST( GENERATE_DATE_ARRAY( DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH), CURRENT_DATE(), INTERVAL 1 DAY ) ) AS dag ),
  godkjent_utbetaling_av AS (
  SELECT
    DISTINCT du.utbetaling_id,
    CASE
      WHEN t.besluttet_av = 'Tiltaksadministrasjon' THEN 'Automatisk'
      ELSE 'Manuell'
  END
    AS prosess,
    MAX(DATE(t.besluttet_tidspunkt)) AS besluttet_dag
  FROM
    `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_delutbetaling` du
  INNER JOIN
    `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_totrinnskontroll` t
  ON
    du.id = t.entity_id
  GROUP BY
    du.utbetaling_id,
    t.besluttet_av
  HAVING
    COUNT(*) > 0
    AND COUNT(*) = COUNTIF(t.besluttelse = 'GODKJENT') )
SELECT
  d.dag,
  gu.prosess,
  COUNT(u.id) AS totalt_antall_godkjente
FROM
  dager d
inner join
  `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_utbetaling` u on d.dag >= DATE(u.created_at)
INNER JOIN
  godkjent_utbetaling_av gu
ON
  gu.utbetaling_id = u.id and d.dag >= gu.besluttet_dag
GROUP BY
  1,2
order by 1 desc
EOF
}

module "grafana_utbetaling_feilet_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "utbetaling_feilet_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode(
    [
      {
        mode = "NULLABLE"
        name = "fakturanummer"
        type = "STRING"
      },
      {
        mode = "NULLABLE"
        name = "sendt_til_okonomi_tidspunkt"
        type = "DATETIME"
      },
      {
        mode = "NULLABLE"
        name = "faktura_status"
        type = "STRING"
      },
    ]
  )
  view_query = <<EOF
SELECT
  du.fakturanummer,
  DATETIME(du.sendt_til_okonomi_tidspunkt) as sendt_til_okonomi_tidspunkt,
  du.faktura_status
FROM
  `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_delutbetaling` du
WHERE
  du.sendt_til_okonomi_tidspunkt IS NOT NULL
ORDER BY
  2
EOF
}

module "grafana_utbetaling_admin_korreksjoner_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "utbetaling_admin_korreksjoner_view"
  depends_on          = [module.mr_api_datastream.dataset_id]
  view_schema = jsonencode(
    [
      {
        mode = "NULLABLE"
        name = "created_at"
        type = "DATETIME"
      },
      {
        mode = "NULLABLE"
        name = "beregning_type"
        type = "STRING"
      },
      {
        mode = "NULLABLE"
        name = "status"
        type = "STRING"
      },
    ]
  )
  view_query = <<EOF
SELECT
  DATETIME(created_at) as created_at,
  beregning_type,
  status
FROM
  `${var.gcp_project["project"]}.${module.mr_api_datastream.dataset_id}.public_utbetaling`
WHERE
  innsender IS NOT NULL
  AND innsender != 'Arrangor'
  AND tilskuddstype = 'TILTAK_DRIFTSTILSKUDD'
order by 1 desc
EOF
}

