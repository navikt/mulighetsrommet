module "datamarkedsplassen_views" {
  source                       = "../modules/datamarkedsplassen-views"
  gcp_project                  = var.gcp_project
  mr_api_datastream_dataset_id = module.mr_api_datastream_setup.dataset_id
}

module "grafana_views" {
  source                       = "../modules/grafana-views"
  gcp_project                  = var.gcp_project
  mr_api_datastream_dataset_id = module.mr_api_datastream_setup.dataset_id
  grafana_dataset_id           = local.grafana_dataset_id

  depends_on = [module.mr_api_datastream_setup]
}
