resource "google_bigquery_table" "view" {
  dataset_id          = var.dataset_id
  table_id            = var.view_id
  schema              = var.view_schema
  deletion_protection = var.deletion_protection

  view {
    use_legacy_sql = false
    query          = var.view_query
  }
}
