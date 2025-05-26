plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    `maven-publish`
}

apply(plugin = "io.spring.dependency-management")

dependencies {
    // Core dependencies only - minimal footprint
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Spring Context for annotations (optional dependency)
    compileOnly("org.springframework:spring-context")

    // Spring Boot AutoConfiguration support
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    // Jakarta Servlet API for request handling (optional)
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    // Spring Security OAuth2 for JWT support (optional)
    compileOnly("org.springframework.security:spring-security-oauth2-jose")

    // Logging
    implementation("org.slf4j:slf4j-api")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${property("mockitoKotlinVersion")}")
    testImplementation("org.springframework:spring-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("Pushpin Security Core")
                description.set("Core security interfaces and models for Pushpin Missing Toolbox")
                url.set("https://github.com/mpecan/pushpin-missing-toolbox")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
