plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.4" apply false
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("com.github.ben-manes.versions") version "0.51.0"
}

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

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    
    repositories {
        mavenCentral()
    }
    
    // Configure KtLint
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        verbose.set(true)
        android.set(false)
        outputToConsole.set(true)
        outputColorName.set("RED")
        ignoreFailures.set(true) // Set to true for CI to continue on lint errors
        enableExperimentalRules.set(true)
        
        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
        }
    }
}

tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates") {
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/reports/dependencyUpdates"
    reportfileName = "report"
}

