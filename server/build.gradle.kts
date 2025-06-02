plugins {
    id("org.springframework.boot")
}

// Group and version are inherited from root project

dependencies {
    implementation(project(":pushpin-api"))
    implementation(project(":pushpin-client"))
    implementation(project(":pushpin-discovery"))
    implementation(project(":pushpin-discovery-aws"))
    implementation(project(":pushpin-discovery-kubernetes"))
    implementation(project(":pushpin-metrics-core"))
    implementation(project(":pushpin-security-core"))
    implementation(project(":pushpin-security-remote"))
    implementation(project(":pushpin-security-audit"))
    implementation(project(":pushpin-security-encryption"))
    implementation(project(":pushpin-security-hmac"))
    implementation(project(":pushpin-security-jwt"))
    implementation(project(":pushpin-security-ratelimit"))
    implementation(project(":pushpin-transport-http"))
    implementation(project(":pushpin-transport-zmq"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // OAuth2 Resource Server and JWT
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")

    // For symmetric key token generation in the AuthController (development/testing only)
    implementation("io.jsonwebtoken:jjwt-api")
    runtimeOnly("io.jsonwebtoken:jjwt-impl")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson")

    // JsonPath for JWT claim extraction
    implementation("com.jayway.jsonpath:json-path")

    // Caffeine for caching
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Micrometer for metrics
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.apache.commons:commons-compress")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("javax.servlet:javax.servlet-api")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation(kotlin("test"))
    testImplementation(project(":pushpin-testcontainers"))
}
