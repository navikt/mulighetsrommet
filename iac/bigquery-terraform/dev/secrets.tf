data "google_secret_manager_secret_version" "mr-api_datastream_secret" {
  secret = var.mr-api_datastream_secret
}

locals {
  mr-api_datastream_credentials = jsondecode(
    data.google_secret_manager_secret_version.mr-api_datastream_secret.secret_data
  )
}
