plugins {
    id("org.springframework.boot")
    id("jacoco")
}

group = "io.github.mpecan"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":pushpin-api"))
    implementation(project(":discovery"))

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

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

jacoco {
    toolVersion = property("jacocoVersion") as String
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    // Exclude KubernetesDiscoveryAutoConfiguration from coverage since it's just configuration
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude("**/config/**")
            }
        })
    )

    dependsOn(tasks.test)
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}