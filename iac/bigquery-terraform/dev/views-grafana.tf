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


