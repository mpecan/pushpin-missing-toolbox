// Get versions from gradle.properties
val kotlinVersion: String by project
val springBootVersion: String by project
val springDependencyManagementVersion: String by project

plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.4" apply false
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
    id("jacoco-report-aggregation")
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

    // Get all versions from gradle.properties
    val jacocoVersion: String by project
    val testcontainersVersion: String by project
    val mockitoKotlinVersion: String by project
    val awsSdkVersion: String by project
    val commonsCompressVersion: String by project
    val servletApiVersion: String by project
    val kubernetesClientVersion: String by project

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

