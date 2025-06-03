plugins {
    id("com.vanniktech.maven.publish")
}
// All configuration is inherited from root project

dependencies {
    implementation(project(":pushpin-security-core"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("jakarta.servlet:jakarta.servlet-api")

    // Logging
    implementation("org.slf4j:slf4j-api")

    // Optional dependencies
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
