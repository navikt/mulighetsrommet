module "datamarkedsplassen_views" {
  source                       = "../modules/datamarkedsplassen-views"
  gcp_project                  = var.gcp_project
  mr_api_datastream_dataset_id = module.mr_api_datastream_setup.dataset_id
}

resource "google_bigquery_table_iam_member" "mr_api_views_all_users_data_viewer" {
  for_each = module.datamarkedsplassen_views.view_ids

  project    = var.gcp_project["project"]
  dataset_id = module.mr_api_datastream_setup.dataset_id
  table_id   = each.value
  role       = "roles/bigquery.dataViewer"
  member     = "group:all-users@nav.no"
}

module "grafana_views" {
  source                       = "../modules/grafana-views"
  gcp_project                  = var.gcp_project
  mr_api_datastream_dataset_id = module.mr_api_datastream_setup.dataset_id
  grafana_dataset_id           = local.grafana_dataset_id

  depends_on = [module.mr_api_datastream_setup]
}
