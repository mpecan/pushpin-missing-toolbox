plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "pushpin-missing-toolbox"
include(":server", ":pushpin-api", ":discovery", ":discovery-aws", ":discovery-kubernetes", ":pushpin-client", ":pushpin-security-core", ":pushpin-security-remote", ":pushpin-security-audit", ":pushpin-security-encryption", ":pushpin-security-hmac", ":pushpin-security-jwt")
