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

