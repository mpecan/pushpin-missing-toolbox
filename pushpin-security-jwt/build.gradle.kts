plugins {
    id("com.vanniktech.maven.publish")
}
// All configuration is inherited from root project

dependencies {
    api(project(":pushpin-security-core"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // JsonPath for JWT claim extraction
    implementation("com.jayway.jsonpath:json-path")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
