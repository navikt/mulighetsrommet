variable "gcp_project" {
  description = "GCP project and region defaults."
  type        = map(string)
}

variable "application_name" {
  description = "Application name used to derive Datastream resources."
  type        = string
}

variable "mr_api_datastream_secret" {
  description = "Name of the GCP secret that provides the mr-api datastream credentials."
  type        = string
}

variable "grafana_dataset_id" {
  description = "Dataset ID for the Grafana views dataset."
  type        = string
}

variable "access_roles" {
  description = "Access roles for the Datastream dataset."
  type        = list(map(string))
}
