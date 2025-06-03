plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

// Configure publishing for this module
configurePushpinPublishing(
    moduleName = "pushpin-discovery-kubernetes",
    moduleDescription =
        "Kubernetes-based service discovery for Pushpin servers - discovers pods by labels and namespaces",
)

dependencies {
    implementation(project(":pushpin-api"))
    implementation(project(":pushpin-discovery"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Kubernetes client dependencies
    implementation("io.kubernetes:client-java")
    implementation("io.kubernetes:client-java-api")
    implementation("io.kubernetes:client-java-spring-integration")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation(kotlin("test"))

    // Kubernetes testing dependencies
    testImplementation("org.testcontainers:k3s")
}
