plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("maven-publish")
}

dependencies {
    implementation(project(":pushpin-security-core"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("com.github.ben-manes.caffeine:caffeine:${rootProject.property("caffeineVersion")}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("jakarta.servlet:jakarta.servlet-api")

    // Optional dependencies
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${rootProject.property("mockitoKotlinVersion")}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "io.github.mpecan"
            artifactId = "pushpin-security-remote"
            version = project.version.toString()
        }
    }
}
