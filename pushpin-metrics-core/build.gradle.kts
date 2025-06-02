plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-library")
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot for autoconfiguration
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    // Micrometer is optional - autoconfiguration will detect it
    compileOnly("io.micrometer:micrometer-core")

    // Logging
    implementation("org.slf4j:slf4j-api")

    // Annotation processor for configuration properties
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.micrometer:micrometer-core")
    testImplementation("io.micrometer:micrometer-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
