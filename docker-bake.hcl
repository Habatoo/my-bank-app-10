variable "TAG" {
  default = "latest"
}

group "default" {
  targets = ["gateway", "account", "cash", "transfer", "notification", "front-ui"]
}

target "_java_base" {
  context = "."
  args = {
    TAG = "${TAG}"
  }
}

# Gateway
target "gateway" {
  inherits = ["_java_base"]
  dockerfile = "gateway/Dockerfile"
  tags = ["my-bank/gateway:${TAG}"]
}

# Account
target "account" {
  inherits = ["_java_base"]
  dockerfile = "account/Dockerfile"
  tags = ["my-bank/account:${TAG}"]
}

# Cash
target "cash" {
  inherits = ["_java_base"]
  dockerfile = "cash/Dockerfile"
  tags = ["my-bank/cash:${TAG}"]
}

# Transfer
target "transfer" {
  inherits = ["_java_base"]
  dockerfile = "transfer/Dockerfile"
  tags = ["my-bank/transfer:${TAG}"]
}

# Notification
target "notification" {
  inherits = ["_java_base"]
  dockerfile = "notification/Dockerfile"
  tags = ["my-bank/notification:${TAG}"]
}

# Front UI
target "front-ui" {
  inherits = ["_java_base"]
  dockerfile = "front-ui/Dockerfile"
  tags = ["my-bank/front-ui:${TAG}"]
}