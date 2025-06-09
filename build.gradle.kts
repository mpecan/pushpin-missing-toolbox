import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.springframework.boot.gradle.tasks.bundling.BootJar

// Get versions from gradle.properties
val kotlinVersion: String by project
val springBootVersion: String by project

plugins {
    java
    id("org.springframework.boot") version "3.5.0" apply false
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
    id("jacoco-report-aggregation")
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0-rc.1"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("org.sonarqube") version "6.2.0.5505"
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

// Publishing configuration moved to individual modules using configurePushpinPublishing function

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
        plugin("io.spring.dependency-management")
        plugin("org.jlleitschuh.gradle.ktlint")
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
    if (name != "server") {
        afterEvaluate {
            // Apply Spring Boot plugin for dependency management if not already applied
            if (!plugins.hasPlugin("org.springframework.boot")) {
                apply(plugin = "org.springframework.boot")
            }
            // Disable bootJar and enable regular jar for libraries
            tasks.findByName("bootJar")?.let { bootJarTask ->
                (bootJarTask as BootJar).enabled = false
            }
            tasks.findByName("jar")?.let { jarTask ->
                (jarTask as Jar).enabled = true
            }
        }
    }
}

// Configure all subprojects to use JaCoCo
subprojects {
    apply(plugin = "jacoco")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")

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
            testTask.finalizedBy(this)
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "mpecan_pushpin-missing-toolbox")
        property("sonar.organization", "mpecan")
        property("sonar.host.url", "https://sonarcloud.io")
    }
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
