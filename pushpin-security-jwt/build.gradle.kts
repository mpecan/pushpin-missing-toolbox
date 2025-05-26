plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot") apply false
}

dependencies {
    api(project(":pushpin-security-core"))
    
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // JsonPath for JWT claim extraction
    implementation("com.jayway.jsonpath:json-path:2.8.0")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${property("mockitoKotlinVersion")}")
    testImplementation(kotlin("test"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}