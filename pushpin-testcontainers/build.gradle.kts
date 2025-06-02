// All configuration is inherited from root project

description = "Testcontainers support for Pushpin server"

dependencies {
    implementation("org.testcontainers:testcontainers")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.slf4j:slf4j-api")

    // For testing the testcontainers module itself
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation("ch.qos.logback:logback-classic")
}
