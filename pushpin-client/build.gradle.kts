plugins {
    id("org.springframework.boot")
    id("maven-publish")
    id("java-library")
}

group = "io.github.mpecan"
version = "0.0.1-SNAPSHOT"

dependencies {
    api(project(":pushpin-api"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webflux")
    implementation("io.projectreactor:reactor-core")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation(kotlin("test"))
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("Pushpin Client")
                description.set("A client library for interacting with Pushpin servers")
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
