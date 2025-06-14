plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

// Configure publishing for this module
configurePushpinPublishing(
    moduleName = "pushpin-transport-zmq",
    moduleDescription =
        "ZeroMQ transport for Pushpin - provides high-performance ZMQ-based message publishing " +
            "with connection pooling",
)

dependencies {
    api(project(":pushpin-api"))
    api(project(":pushpin-client"))
    api(project(":pushpin-discovery"))
    api(project(":pushpin-transport-core"))

    implementation("org.zeromq:jeromq")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework:spring-webflux")
    implementation("io.projectreactor:reactor-core")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation("io.projectreactor:reactor-test")
}
