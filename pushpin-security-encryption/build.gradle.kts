plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("maven-publish")
}

dependencies {
    implementation(project(":pushpin-security-core"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
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
            artifactId = "pushpin-security-encryption"
            version = project.version.toString()
        }
    }
}