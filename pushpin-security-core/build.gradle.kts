plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

// Configure publishing for this module
configurePushpinPublishing(
    moduleName = "pushpin-security-core",
    moduleDescription =
        "Core security interfaces and models for Pushpin applications" +
            " - provides foundational security abstractions",
)

dependencies {
    // Core dependencies only - minimal footprint
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Spring Context for annotations (optional dependency)
    compileOnly("org.springframework:spring-context")

    // Spring Boot AutoConfiguration support
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    // Jakarta Servlet API for request handling (optional)
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    // Spring Security OAuth2 for JWT support (optional)
    compileOnly("org.springframework.security:spring-security-oauth2-jose")

    // Logging
    implementation("org.slf4j:slf4j-api")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation("org.springframework:spring-test")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
