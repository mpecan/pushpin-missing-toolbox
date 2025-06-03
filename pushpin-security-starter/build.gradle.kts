plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

// Configure publishing for this module
configurePushpinPublishing(
    moduleName = "pushpin-security-starter",
    moduleDescription =
        "Spring Boot starter for Pushpin security - bundles all security modules with " +
            "auto-configuration for a complete security solution",
)

dependencies {
    // Include all security modules
    api(project(":pushpin-security-core"))
    api(project(":pushpin-security-remote"))
    api(project(":pushpin-security-audit"))
    api(project(":pushpin-security-encryption"))
    api(project(":pushpin-security-hmac"))
    api(project(":pushpin-security-jwt"))
    api(project(":pushpin-security-ratelimit"))

    // Spring Boot starter dependencies
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Optional dependencies that security modules might need
    implementation("com.jayway.jsonpath:json-path")
    implementation("com.github.ben-manes.caffeine:caffeine")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
