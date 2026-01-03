group "default" {
  targets = []
}

variable "TAG" {
  default = "latest"
}

target "app-base" {
  dockerfile = "Dockerfile"
  context = "."
}