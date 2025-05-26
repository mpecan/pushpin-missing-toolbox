plugins {
    id("org.springframework.boot")
}

// Group and version are inherited from root project

dependencies {
    implementation(project(":pushpin-api"))
    implementation(project(":pushpin-client"))
    implementation(project(":discovery"))
    implementation(project(":discovery-aws"))
    implementation(project(":discovery-kubernetes"))
    implementation(project(":pushpin-security-core"))
    implementation(project(":pushpin-security-remote"))
    implementation(project(":pushpin-security-audit"))
    implementation(project(":pushpin-security-encryption"))
    implementation(project(":pushpin-security-hmac"))
    implementation(project(":pushpin-security-jwt"))
    implementation(project(":pushpin-transport-http"))
    implementation(project(":pushpin-transport-zmq"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // OAuth2 Resource Server and JWT
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")

    // For symmetric key token generation in the AuthController (development/testing only)
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // Rate limiting
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0")

    // JsonPath for JWT claim extraction
    implementation("com.jayway.jsonpath:json-path:2.8.0")

    // Caffeine for caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.apache.commons:commons-compress")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("javax.servlet:javax.servlet-api")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("test"))
}
