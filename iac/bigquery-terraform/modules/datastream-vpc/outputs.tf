output "vpc_name" {
  description = "The name of the VPC"
  value       = google_compute_network.mr_datastream_private_vpc.name
}

output "private_connection_id" {
  description = "The ID of the Datastream private connection"
  value       = google_datastream_private_connection.mr_datastream_private_connection.id
}

output "bigquery_connection_profile_id" {
  description = "The ID of the BigQuery Datastream connection profile"
  value       = google_datastream_connection_profile.datastream_bigquery_connection_profile.id
}
