variable "gcp_project" {
  description = "GCP project and region defaults."
  type        = map(string)
  default = {
    region  = "europe-north1",
    zone    = "europe-north1-a",
    project = "team-mulighetsrommet-dev-a2d7"
  }
}

variable "mr-api_datastream_secret" {
  description = "Name of the GCP secret that provides the mr-api datastream credentials."
  type        = string
  default     = "mr-api-datastream-credentials"
}
