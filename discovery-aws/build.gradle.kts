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

    // AWS SDK dependencies
    implementation("software.amazon.awssdk:ec2")
    implementation("software.amazon.awssdk:autoscaling")
    implementation("software.amazon.awssdk:sts")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation(kotlin("test"))

    // AWS testing dependencies
    testImplementation("org.testcontainers:localstack")
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

    // Exclude auto-configuration classes from coverage
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