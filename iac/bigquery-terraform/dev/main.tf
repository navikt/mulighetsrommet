terraform {
  required_providers {
    google = {
      source = "hashicorp/google"
      # version = "4.84.0"
      version = "6.12.0"
    }
  }

  backend "gcs" {
    bucket = "mr-terraform-state-dev"
  }
}

provider "google" {
  project = var.gcp_project["project"]
  region  = var.gcp_project["region"]
}

data "google_project" "project" {}
