plugins {
    id("com.vanniktech.maven.publish")
}
// All configuration is inherited from root project

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
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation("org.springframework:spring-test")
}
