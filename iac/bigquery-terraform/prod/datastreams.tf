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
  cloud_sql_instance_name    = "mulighetsrommet-api-v1"
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
        { table = "gjennomforing" },
        { table = "gjennomforing_amo_kategorisering" },
        { table = "gjennomforing_amo_kategorisering_sertifisering" },
        { table = "gjennomforing_utdanningsprogram" },
        { table = "arrangor" },
        { table = "utdanningsprogram" },
        { table = "utdanning" },
      ]
    }
  ]

  access_roles = [
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
      user_by_email = "effekt-j6bp@knada-gcp.iam.gserviceaccount.com"
    }
  ]

  authorized_views = [
    {
      view = {
        dataset_id = "mulighetsrommet_api_datastream"
        project_id = var.gcp_project["project"]
        table_id   = "tiltakstype_view"
      }
    },
    {
      view = {
        dataset_id = "mulighetsrommet_api_datastream"
        project_id = var.gcp_project["project"]
        table_id   = "avtale_view"
      }
    },
    {
      view = {
        dataset_id = "mulighetsrommet_api_datastream"
        project_id = var.gcp_project["project"]
        table_id   = "gjennomforing_view"
      }
    },
  ]
}
