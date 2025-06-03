plugins {
    id("com.vanniktech.maven.publish")
}
// All configuration is inherited from root project

dependencies {
    implementation(project(":pushpin-security-core"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("jakarta.servlet:jakarta.servlet-api")

    // Optional dependencies
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
