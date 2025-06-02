plugins {
    id("org.springframework.boot")
    id("maven-publish")
    id("java-library")
}

description = "Testcontainers support for Pushpin server"

dependencies {
    implementation("org.testcontainers:testcontainers:${property("testcontainersVersion")}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.slf4j:slf4j-api")

    // For testing the testcontainers module itself
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${property("mockitoKotlinVersion")}")
    testImplementation("ch.qos.logback:logback-classic")
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

java {
    withJavadocJar()
    withSourcesJar()
}
