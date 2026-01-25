group "default" {
  targets = [
    "transfer",
    "gateway",
    "account",
    "cash",
    "notification",
    "front-ui"
  ]
}

target "base" {
  context    = "."
  platforms  = ["linux/amd64"]
  pull       = true
}

target "transfer" {
  inherits   = ["base"]
  dockerfile = "transfer/Dockerfile"
  tags       = ["my-bank/transfer:latest"]
}

target "gateway" {
  inherits   = ["base"]
  dockerfile = "gateway/Dockerfile"
  tags       = ["my-bank/gateway:latest"]
}

target "account" {
  inherits   = ["base"]
  dockerfile = "account/Dockerfile"
  tags       = ["my-bank/account:latest"]
}

target "cash" {
  inherits   = ["base"]
  dockerfile = "cash/Dockerfile"
  tags       = ["my-bank/cash:latest"]
}

target "notification" {
  inherits   = ["base"]
  dockerfile = "notification/Dockerfile"
  tags       = ["my-bank/notification:latest"]
}

target "front-ui" {
  inherits   = ["base"]
  dockerfile = "front-ui/Dockerfile"
  tags       = ["my-bank/front-ui:latest"]
}