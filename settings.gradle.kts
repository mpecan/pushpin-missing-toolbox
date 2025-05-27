plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "pushpin-missing-toolbox"
include(
    ":server",
    ":pushpin-api",
    ":pushpin-client",
    ":pushpin-discovery",
    ":pushpin-discovery-aws",
    ":pushpin-discovery-kubernetes",
    ":pushpin-metrics-core",
    ":pushpin-security-core",
    ":pushpin-security-remote",
    ":pushpin-security-audit",
    ":pushpin-security-encryption",
    ":pushpin-security-hmac",
    ":pushpin-security-jwt",
    ":pushpin-security-starter",
    ":pushpin-transport-core",
    ":pushpin-transport-http",
    ":pushpin-transport-zmq",
    ":pushpin-testcontainers",
)
