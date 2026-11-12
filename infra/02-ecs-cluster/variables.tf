variable "aws_region" {
  description = "Región de AWS donde se desplegará el clúster ECS"
  type        = string
  default     = "us-east-1"
}

variable "image_uri" {
  description = "URI completa de la imagen Docker en ECR (incluyendo tag)"
  type        = string
}

variable "app_port" {
  description = "Puerto expuesto por el contenedor de la aplicación"
  type        = number
  default     = 8080
}
