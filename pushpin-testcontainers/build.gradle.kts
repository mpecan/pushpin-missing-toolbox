plugins {
    id("org.springframework.boot")
    id("maven-publish")
    id("java-library")
}

description = "Testcontainers support for Pushpin server"

dependencies {
    implementation("org.testcontainers:testcontainers:${property("testcontainersVersion")}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.slf4j:slf4j-api")

    // For testing the testcontainers module itself
    testImplementation("org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${property("mockitoKotlinVersion")}")
    testImplementation("ch.qos.logback:logback-classic")
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("Pushpin Testcontainers")
                description.set("Testcontainers support for Pushpin server integration testing")
                url.set("https://github.com/mpecan/pushpin-missing-toolbox")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("mpecan")
                        name.set("Matjaž Pečan")
                        email.set("matjaz.pecan@gmail.com")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/mpecan/pushpin-missing-toolbox")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
