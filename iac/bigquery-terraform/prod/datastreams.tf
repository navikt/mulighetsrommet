module "mr_api_datastream_setup" {
  source                   = "../modules/mr-api-datastream"
  gcp_project              = var.gcp_project
  application_name         = local.application_name
  mr_api_datastream_secret = var.mr_api_datastream_secret
  grafana_dataset_id       = local.grafana_dataset_id
  access_roles             = var.access_roles
}
