locals {
  datastream_vpc_resources = {
    vpc_name                       = google_compute_network.mr_datastream_private_vpc.name
    private_connection_id          = google_datastream_private_connection.mr_datastream_private_connection.id
    bigquery_connection_profile_id = google_datastream_connection_profile.datastream_bigquery_connection_profile.id
  }
}

module "mr-api_datastream" {
  source                                       = "git::https://github.com/navikt/terraform-google-bigquery-datastream.git?ref=v1.0.1"
  gcp_project                                  = var.gcp_project
  application_name                             = "mulighetsrommet-api"
  cloud_sql_instance_name                      = "mulighetsrommet-api"
  cloud_sql_instance_db_name                   = "mulighetsrommet-api-db"
  cloud_sql_instance_db_credentials            = local.mr-api_datastream_credentials
  cloud_sql_instance_publication_name          = "ds_publication"
  cloud_sql_instance_replication_name          = "ds_replication"
  datastream_vpc_resources                     = local.datastream_vpc_resources
  big_query_dataset_delete_contents_on_destroy = true
  postgresql_include_schemas = [
    {
      schema = "public",
      tables = [
        { table = "tiltakstype" },
        { table = "tiltaksgjennomforing" },
      ]
    }
  ]
}
