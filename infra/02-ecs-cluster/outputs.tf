output "alb_dns_name" {
  description = "Nombre DNS público del Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "public_endpoint_url" {
  description = "URL pública del endpoint de prueba del microservicio"
  value       = "http://${aws_lb.main.dns_name}/api/v1/hello"
}
