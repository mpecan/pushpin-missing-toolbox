plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

// Configure publishing for this module
configurePushpinPublishing(
    moduleName = "pushpin-discovery-aws",
    moduleDescription =
        "AWS-based service discovery for Pushpin servers - discovers EC2 instances by tags " +
            "and Auto Scaling Groups",
)

dependencies {
    implementation(project(":pushpin-api"))
    implementation(project(":pushpin-discovery"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // AWS SDK dependencies
    implementation("software.amazon.awssdk:ec2")
    implementation("software.amazon.awssdk:autoscaling")
    implementation("software.amazon.awssdk:sts")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation(kotlin("test"))

    // AWS testing dependencies
    testImplementation("org.testcontainers:localstack")
}
