plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

// Configure publishing for this module
configurePushpinPublishing(
    moduleName = "pushpin-testcontainers",
    moduleDescription =
        "Testcontainers implementation for Pushpin " +
            "- enables integration testing with containerized Pushpin instances",
)

dependencies {
    implementation("org.testcontainers:testcontainers")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.slf4j:slf4j-api")

    // For testing the testcontainers module itself
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation("ch.qos.logback:logback-classic")
}
