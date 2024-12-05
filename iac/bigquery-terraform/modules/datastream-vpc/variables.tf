variable "gcp_project" {
  description = "GCP project and region defaults."
  type = map(string)
}

variable "cloud_sql_port" {
  description = "The port exposed by the Cloud SQL instance."
  type        = string
  default     = "5432"
}
