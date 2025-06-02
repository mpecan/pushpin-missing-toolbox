plugins {
    id("org.springframework.boot")
}

// Group and version are inherited from root project

dependencies {
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.jsonwebtoken:jjwt-api")
    runtimeOnly("io.jsonwebtoken:jjwt-impl")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson")

    // Spring framework for ResponseEntity
    compileOnly("org.springframework:spring-web")
}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}
