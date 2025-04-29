module "grafana_tilsagn_type_antall_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "tilsagn_type_antall_view"
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
  1
ORDER BY
  2 desc
EOF
}

module "grafana_tilsagn_status_antall_view" {
  source              = "../modules/google-bigquery-view"
  deletion_protection = false
  dataset_id          = local.grafana_dataset_id
  view_id             = "tilsagn_status_antall_view"
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
  1
EOF
}
