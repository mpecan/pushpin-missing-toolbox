plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

// Configure publishing for this module
configurePushpinPublishing(
    moduleName = "pushpin-transport-http",
    moduleDescription =
        "HTTP transport for Pushpin - provides HTTP-based message publishing and health " +
            "checking for Pushpin servers",
)

dependencies {
    api(project(":pushpin-api"))
    api(project(":pushpin-client"))
    api(project(":pushpin-discovery"))
    api(project(":pushpin-transport-core"))

    implementation("org.springframework:spring-webflux")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation("io.projectreactor:reactor-test")
}
