module "mr_datastream_vpc" {
  source      = "../modules/datastream-vpc"
  gcp_project = var.gcp_project
}

data "google_secret_manager_secret_version" "mr_api_datastream_secret" {
  secret = var.mr_api_datastream_secret
}

module "mr_api_datastream" {
  source                     = "git::https://github.com/navikt/terraform-google-bigquery-datastream.git?ref=v1.0.1"
  gcp_project                = var.gcp_project
  application_name           = "mulighetsrommet-api"
  cloud_sql_instance_name    = "mulighetsrommet-api"
  cloud_sql_instance_db_name = "mulighetsrommet-api-db"
  cloud_sql_instance_db_credentials = jsondecode(
    data.google_secret_manager_secret_version.mr_api_datastream_secret.secret_data
  )
  cloud_sql_instance_publication_name = "ds_publication"
  cloud_sql_instance_replication_name = "ds_replication"
  datastream_vpc_resources = {
    vpc_name                       = module.mr_datastream_vpc.vpc_name
    private_connection_id          = module.mr_datastream_vpc.private_connection_id
    bigquery_connection_profile_id = module.mr_datastream_vpc.bigquery_connection_profile_id
  }
  big_query_dataset_delete_contents_on_destroy = true
  postgresql_include_schemas = [
    {
      schema = "public",
      tables = [
        { table = "tiltakstype" },
        { table = "avtale" },
        { table = "tiltaksgjennomforing" },
        { table = "tiltaksgjennomforing_amo_kategorisering" },
        { table = "tiltaksgjennomforing_amo_kategorisering_sertifisering" },
        { table = "tiltaksgjennomforing_utdanningsprogram" },
        { table = "arrangor" },
        { table = "utdanningsprogram" },
        { table = "utdanning" },
        { table = "utdanning_nus_kode" },
        { table = "utdanning_nus_kode_innhold" },
      ]
    }
  ]
}
