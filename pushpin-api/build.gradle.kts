plugins {
    id("org.springframework.boot")
}

// Group and version are inherited from root project

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.jsonwebtoken:jjwt-api:${property("jjwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${property("jjwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${property("jjwtVersion")}")

    // Spring framework for ResponseEntity
    compileOnly("org.springframework:spring-web")
}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}
