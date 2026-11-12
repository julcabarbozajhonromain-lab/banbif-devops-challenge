variable "aws_region" {
  description = "Región de AWS donde se desplegarán los recursos"
  type        = string
  default     = "us-east-1"
}

variable "repo_name" {
  description = "Nombre del repositorio ECR para la aplicación Java"
  type        = string
  default     = "banbif-java-app"
}
