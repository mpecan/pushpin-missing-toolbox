import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost
import org.jlleitschuh.gradle.ktlint.KtlintExtension

// Get versions from gradle.properties
val kotlinVersion: String by project
val springBootVersion: String by project

plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.spring") version "2.1.21"
    id("org.springframework.boot") version "3.5.0" apply false
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
    id("jacoco-report-aggregation")
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0-rc.1"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("com.vanniktech.maven.publish") version "0.32.0"
}

// Group and version are defined in gradle.properties

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

// Configure Maven publishing
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates("io.github.mpecan", "pushpin-missing-toolbox", version.toString())

    pom {
        name = "Pushpin Missing Toolbox"
        description = "A comprehensive toolkit for working with Pushpin reverse proxy, " +
            "providing essential features for real-time web applications"
        inceptionYear = "2025"
        url = "https://github.com/mpecan/pushpin-missing-toolbox"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
                distribution = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "mpecan"
                name = "Pushpin Missing Toolbox Team"
                url = "https://github.com/mpecan/pushpin-missing-toolbox"
            }
        }
        scm {
            url = "https://github.com/mpecan/pushpin-missing-toolbox"
            connection = "scm:git:git://github.com/mpecan/pushpin-missing-toolbox.git"
            developerConnection = "scm:git:ssh://git@github.com/mpecan/pushpin-missing-toolbox.git"
        }
    }
}

// Centralized dependency declarations for all subprojects
allprojects {
    repositories {
        mavenCentral()
    }
}

// Apply common configurations and dependencies to all subprojects
subprojects {
    // Apply plugins
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.spring")
        plugin("io.spring.dependency-management")
        plugin("org.jlleitschuh.gradle.ktlint")
        plugin("com.vanniktech.maven.publish")
    }

    // Get all versions from gradle.properties
    val testcontainersVersion: String by project
    val mockitoKotlinVersion: String by project
    val awsSdkVersion: String by project
    val commonsCompressVersion: String by project
    val servletApiVersion: String by project
    val kubernetesClientVersion: String by project
    val jjwtVersion: String by project
    val bucket4jVersion: String by project
    val jsonPathVersion: String by project
    val jeromqVersion: String by project
    val caffeineVersion: String by project
    val micrometerVersion: String by project
    val kotlinxCoroutinesVersion: String by project
    val reactorKotlinExtensionsVersion: String by project
    val logbackVersion: String by project
    val httpClientVersion: String by project
    val artemisVersion: String by project
    val tomcatVersion: String by project
    val okioVersion: String by project

    // Apply dependency management
    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
            mavenBom("software.amazon.awssdk:bom:$awsSdkVersion")
            mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
        }

        dependencies {
            // Kotlin dependencies
            dependency("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
            dependency("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
            dependency("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
            dependency("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")

            // Kotlin Coroutines
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxCoroutinesVersion")
            dependency("io.projectreactor.kotlin:reactor-kotlin-extensions:$reactorKotlinExtensionsVersion")

            // Test dependencies
            dependency("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
            dependency("org.apache.commons:commons-compress:$commonsCompressVersion")
            dependency("javax.servlet:javax.servlet-api:$servletApiVersion")

            // AWS SDK dependencies
            dependency("software.amazon.awssdk:ec2:$awsSdkVersion")
            dependency("software.amazon.awssdk:autoscaling:$awsSdkVersion")
            dependency("software.amazon.awssdk:sts:$awsSdkVersion")

            // Kubernetes client dependencies
            dependency("io.kubernetes:client-java:$kubernetesClientVersion")
            dependency("io.kubernetes:client-java-api:$kubernetesClientVersion")
            dependency("io.kubernetes:client-java-spring-integration:$kubernetesClientVersion")

            // Security dependencies
            dependency("io.jsonwebtoken:jjwt-api:$jjwtVersion")
            dependency("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
            dependency("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
            dependency("com.github.vladimir-bukhtoyarov:bucket4j-core:$bucket4jVersion")
            dependency("com.jayway.jsonpath:json-path:$jsonPathVersion")

            // Infrastructure dependencies
            dependency("org.zeromq:jeromq:$jeromqVersion")
            dependency("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

            // Monitoring dependencies
            dependency("io.micrometer:micrometer-core:$micrometerVersion")
            dependency("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

            // Override vulnerable versions
            dependency("ch.qos.logback:logback-classic:$logbackVersion")
            dependency("ch.qos.logback:logback-core:$logbackVersion")
            dependency("org.apache.httpcomponents.client5:httpclient5:$httpClientVersion")
            dependency("org.apache.activemq:artemis-project:$artemisVersion")
            dependency("org.apache.tomcat.embed:tomcat-embed-core:$tomcatVersion")
            dependency("org.apache.tomcat.embed:tomcat-embed-el:$tomcatVersion")
            dependency("org.apache.tomcat.embed:tomcat-embed-websocket:$tomcatVersion")
            dependency("com.squareup.okio:okio:$okioVersion")
            dependency("com.squareup.okio:okio-jvm:$okioVersion")
        }
    }

    // Common configurations for all subprojects
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.set(listOf("-Xjsr305=strict"))
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    kotlin {
        jvmToolchain(17)
    }

    configure<KtlintExtension> {
        verbose.set(true)
        android.set(false)
        outputToConsole.set(true)
        outputColorName.set("RED")
        ignoreFailures.set(false) // Set to false to fail on ktlint errors
        enableExperimentalRules.set(true)

        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
        }
    }

    // Configure library modules (all except server)
    if (project.name != "server") {
        // Apply Spring Boot plugin for dependency management
        apply(plugin = "org.springframework.boot")
        // Disable bootJar and enable regular jar for libraries
        tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
            enabled = false
        }
        tasks.named<Jar>("jar") {
            enabled = true
        }
    }

    // Configure publishing for all modules that start with "pushpin-" or are in the discovery modules
    if (project.name.startsWith("pushpin-")) {
        apply(plugin = "com.vanniktech.maven.publish")
        mavenPublishing {
            configure(
                KotlinJvm(
                    javadocJar = JavadocJar.Javadoc(),
                    sourcesJar = true,
                ),
            )
            coordinates("io.github.mpecan", project.name, version.toString())

            pom {
                name = project.name
                description = "Pushpin Missing Toolbox - ${project.name}"
                url = "https://github.com/mpecan/pushpin-missing-toolbox"
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                        distribution = "https://opensource.org/licenses/MIT"
                    }
                }
                developers {
                    developer {
                        id = "mpecan"
                        name = "Pushpin Missing Toolbox Team"
                        url = "https://github.com/mpecan/pushpin-missing-toolbox"
                    }
                }
                scm {
                    url = "https://github.com/mpecan/pushpin-missing-toolbox"
                    connection = "scm:git:git://github.com/mpecan/pushpin-missing-toolbox.git"
                    developerConnection = "scm:git:ssh://git@github.com/mpecan/pushpin-missing-toolbox.git"
                }
            }
        }
    }
}

// Configure all subprojects to use JaCoCo
subprojects {
    apply(plugin = "jacoco")

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }

    // Create a jacoco task that will run after tests
    tasks.withType<Test> {
        val testTask = this
        tasks.withType<JacocoReport> {
            executionData(testTask.extensions.getByType<JacocoTaskExtension>().destinationFile!!)
            dependsOn(testTask)
        }
    }
}

// Aggregate JaCoCo reports from all subprojects
val jacocoAggregatedReport by tasks.registering(JacocoReport::class) {
    group = "verification"
    description = "Generates aggregated JaCoCo coverage report from all subprojects"

    val allExecutionData = project.objects.fileCollection()
    val allSourceSets = project.objects.fileCollection()
    val allClassDirs = project.objects.fileCollection()

    // Collect all the data from subprojects
    subprojects {
        val subproject = this

        // Only include projects that have applied the JaCoCo plugin
        plugins.withId("jacoco") {
            tasks.withType<Test> {
                val testTask = this

                // Include execution data from the test task if it exists
                testTask.extensions.findByType<JacocoTaskExtension>()?.destinationFile?.let { file ->
                    if (file.exists()) {
                        allExecutionData.from(file)
                    }
                }
            }

            // Include all source directories
            subproject.extensions.findByType(SourceSetContainer::class.java)?.let { sourceSets ->
                allSourceSets.from(sourceSets["main"].allSource.srcDirs)
                // Include all class directories
                allClassDirs.from(sourceSets["main"].output.classesDirs)
            }
        }
    }

    // Configure the report
    executionData.from(allExecutionData)
    sourceDirectories.from(allSourceSets)
    classDirectories.from(allClassDirs)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

// Add a task to run all tests and the aggregated report
tasks.register("testWithCoverage") {
    group = "verification"
    description = "Runs all tests and generates an aggregated coverage report"

    // First, run all tests
    val testTasks = subprojects.flatMap { it.tasks.withType<Test>() }
    dependsOn(testTasks)

    // Make the jacocoAggregatedReport task depend on this task
    // This avoids circular dependency issues
    finalizedBy(jacocoAggregatedReport)
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/reports/dependencyUpdates"
    reportfileName = "report"
}

// Install pre-commit hook for ktlint (only for developers, not CI)
val installGitHook by tasks.registering {
    group = "git hooks"
    description = "Install pre-commit git hook for ktlint"

    doLast {
        // Skip if running in CI environment
        val isCI =
            System.getenv("CI") != null ||
                System.getenv("GITHUB_ACTIONS") != null ||
                System.getenv("JENKINS_HOME") != null ||
                System.getenv("GITLAB_CI") != null ||
                System.getenv("CIRCLECI") != null

        if (isCI) {
            logger.lifecycle("Skipping git hook installation in CI environment")
            return@doLast
        }

        val hookFile = file(".git/hooks/pre-commit")
        val hookContent =
            """
            |#!/bin/sh
            |
            |# Run ktlintCheck before commit
            |echo "Running ktlintCheck..."
            |
            |# Run ktlintCheck
            |./gradlew ktlintCheck --console=plain
            |
            |# Capture the exit code
            |RESULT=${'$'}?
            |
            |# If ktlintCheck failed, abort the commit
            |if [ ${'$'}RESULT -ne 0 ]; then
            |    echo ""
            |    echo "❌ Commit aborted due to ktlint violations."
            |    echo "Please fix the issues and try again."
            |    echo "You can run './gradlew ktlintFormat' to auto-fix some violations."
            |    exit 1
            |fi
            |
            |echo "✅ All ktlint checks passed!"
            |exit 0
            """.trimMargin()

        if (!hookFile.parentFile.exists()) {
            logger.warn("Git hooks directory not found. Skipping pre-commit hook installation.")
            return@doLast
        }

        hookFile.writeText(hookContent)
        hookFile.setExecutable(true)
        logger.lifecycle("Pre-commit hook installed successfully at ${hookFile.path}")
    }
}

// Automatically install git hooks after project evaluation
afterEvaluate {
    tasks.named("build") {
        dependsOn(installGitHook)
    }
}
