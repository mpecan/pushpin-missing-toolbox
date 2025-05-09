plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.4" apply false
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
    id("jacoco-report-aggregation")
}

// Import JaCoCo tasks and Kotlin extensions
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

group = "io.github.mpecan"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

// Centralized dependency versions
extra["kotlinVersion"] = "1.9.25"
extra["springBootVersion"] = "3.4.4"
extra["junitVersion"] = "5.10.2"
extra["springDependencyManagementVersion"] = "1.1.7"
extra["jacocoVersion"] = "0.8.11"
extra["testcontainersVersion"] = "1.19.8"
extra["mockitoKotlinVersion"] = "5.2.1"
extra["awsSdkVersion"] = "2.25.13"
extra["commonsCompressVersion"] = "1.26.0"
extra["servletApiVersion"] = "4.0.1"
extra["kubernetesClientVersion"] = "20.0.0"

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
    }

    // Apply dependency management
    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
            mavenBom("software.amazon.awssdk:bom:${property("awsSdkVersion")}")
            mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
        }

        dependencies {
            // Kotlin dependencies
            dependency("org.jetbrains.kotlin:kotlin-reflect:${property("kotlinVersion")}")
            dependency("org.jetbrains.kotlin:kotlin-stdlib:${property("kotlinVersion")}")
            dependency("org.jetbrains.kotlin:kotlin-test:${property("kotlinVersion")}")
            dependency("org.jetbrains.kotlin:kotlin-test-junit5:${property("kotlinVersion")}")

            // Test dependencies
            dependency("org.mockito.kotlin:mockito-kotlin:${property("mockitoKotlinVersion")}")
            dependency("org.apache.commons:commons-compress:${property("commonsCompressVersion")}")
            dependency("javax.servlet:javax.servlet-api:${property("servletApiVersion")}")

            // AWS SDK dependencies
            dependency("software.amazon.awssdk:ec2:${property("awsSdkVersion")}")
            dependency("software.amazon.awssdk:autoscaling:${property("awsSdkVersion")}")
            dependency("software.amazon.awssdk:sts:${property("awsSdkVersion")}")

            // Kubernetes client dependencies
            dependency("io.kubernetes:client-java:${property("kubernetesClientVersion")}")
            dependency("io.kubernetes:client-java-api:${property("kubernetesClientVersion")}")
            dependency("io.kubernetes:client-java-spring-integration:${property("kubernetesClientVersion")}")
        }
    }

    // Common configurations for all subprojects
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    kotlin {
        jvmToolchain(17)
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

