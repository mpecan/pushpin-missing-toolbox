plugins {
    id("org.springframework.boot")
}

// Group and version are inherited from root project

dependencies {
    implementation(project(":pushpin-api"))
    implementation(project(":pushpin-client"))
    implementation(project(":discovery"))
    implementation(project(":discovery-aws"))
    implementation(project(":discovery-kubernetes"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.apache.commons:commons-compress")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("javax.servlet:javax.servlet-api")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("test"))
}
