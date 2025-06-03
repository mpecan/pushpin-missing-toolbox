plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

// Configure publishing for this module
configurePushpinPublishing(
    moduleName = "pushpin-security-ratelimit",
    moduleDescription =
        "Rate limiting for Pushpin - implements token bucket algorithm to protect against " +
            "abuse and ensure fair resource usage",
)

dependencies {
    // Dependencies from other modules
    api(project(":pushpin-security-core"))

    // Spring Boot dependencies
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("jakarta.servlet:jakarta.servlet-api")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Rate limiting library
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
