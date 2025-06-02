# Custom dataset with views for grafana, these are usually public
resource "google_bigquery_dataset" "grafana_views" {
  dataset_id  = local.grafana_dataset_id
  description = "Public views for grafana"
  project     = var.gcp_project["project"]
  location    = var.gcp_project["region"]
  depends_on  = [module.mr_api_datastream.dataset_id]

  access {
    role          = "OWNER"
    special_group = "projectOwners"
  }

  access {
    role          = "READER"
    special_group = "projectReaders"
  }

  access {
    role          = "WRITER"
    special_group = "projectWriters"
  }

  access {
    role          = "READER"
    user_by_email = "grafana@nais-management-233d.iam.gserviceaccount.com"
  }
}
