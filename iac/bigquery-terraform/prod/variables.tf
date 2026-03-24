variable "gcp_project" {
  description = "GCP project and region defaults."
  type        = map(string)
  default = {
    region  = "europe-north1",
    zone    = "europe-north1-a",
    project = "team-mulighetsrommet-prod-5492"
  }
}

variable "mr_api_datastream_secret" {
  description = "Name of the GCP secret that provides the mr-api datastream credentials."
  type        = string
  default     = "mr-api-datastream-credentials"
}

variable "access_roles" {
  description = "Access roles for the datastream dataset."
  type        = list(map(string))
  default = [
    {
      role          = "OWNER"
      special_group = "projectOwners"
    },
    {
      role          = "READER"
      special_group = "projectReaders"
    },
    {
      role          = "WRITER"
      special_group = "projectWriters"
    },
    {
      role           = "roles/bigquery.metadataViewer"
      group_by_email = "all-users@nav.no"
    },
    {
      role          = "roles/bigquery.metadataViewer"
      user_by_email = "nada-metabase@nada-prod-6977.iam.gserviceaccount.com"
    },
    {
      role          = "roles/bigquery.metadataViewer"
      user_by_email = "effekt-j6bp@knada-gcp.iam.gserviceaccount.com"
    },
    {
      role          = "roles/bigquery.metadataViewer"
      user_by_email = "bigqueryloader@teamoppfolging-prod-47fb.iam.gserviceaccount.com"
    },
  ]
}

locals {
  application_name   = "mulighetsrommet-api"
  grafana_id         = "${local.application_name}-grafana"
  grafana_dataset_id = replace(local.grafana_id, "-", "_")
}
