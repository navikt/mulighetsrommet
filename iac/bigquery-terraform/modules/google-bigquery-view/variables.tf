variable "dataset_id" {
  description = "The dataset the module resources belongs to."
  type        = string
}

variable "deletion_protection" {
  description = "If the table or view can be deleted even when data is present."
  type        = bool
  default     = true
}

variable "view_id" {
  description = "The name of the view to create."
  type        = string
}

variable "view_schema" {
  description = "The view schema."
  type        = string
}

variable "view_query" {
  description = "The SQL used to populate the view."
  type        = string
}
