plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    `maven-publish`
}

dependencies {
    api(project(":pushpin-api"))
    api(project(":pushpin-client"))
    api(project(":pushpin-discovery"))
    api(project(":pushpin-transport-core"))

    implementation("org.springframework:spring-webflux")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${project.findProperty("mockitoKotlinVersion")}")
    testImplementation("io.projectreactor:reactor-test")
}
