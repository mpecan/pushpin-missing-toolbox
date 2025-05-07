plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.mpecan"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":pushpin-api"))
    implementation(project(":discovery"))
    
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    
    // AWS SDK dependencies
    implementation(platform("software.amazon.awssdk:bom:2.25.13")) // Use the BOM for AWS SDK version management
    implementation("software.amazon.awssdk:ec2")                   // EC2 client
    implementation("software.amazon.awssdk:autoscaling")           // Auto Scaling client
    implementation("software.amazon.awssdk:sts")                   // STS for IAM role support

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation(kotlin("test"))
    
    // AWS testing dependencies
    testImplementation("org.testcontainers:localstack:1.19.8")
}

tasks.bootJar{
    enabled = false
}

tasks.jar {
    enabled = true
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}