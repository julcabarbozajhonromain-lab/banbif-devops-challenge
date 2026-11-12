output "repository_url" {
  description = "URL del repositorio ECR para hacer push de la imagen Docker"
  value       = aws_ecr_repository.app.repository_url
}
