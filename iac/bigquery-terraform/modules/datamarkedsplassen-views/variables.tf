variable "gcp_project" {
  description = "GCP project and region defaults."
  type        = map(string)
}

variable "mr_api_datastream_dataset_id" {
  description = "Dataset ID created by the mr-api datastream module."
  type        = string
}
