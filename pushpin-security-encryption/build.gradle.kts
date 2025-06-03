plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

// Configure publishing for this module
configurePushpinPublishing(
    moduleName = "pushpin-security-encryption",
    moduleDescription =
        "Encryption support for Pushpin - provides AES/GCM authenticated encryption for " +
            "securing sensitive channel data",
)

dependencies {
    implementation(project(":pushpin-security-core"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Optional dependencies
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
