// All configuration is inherited from root project

dependencies {
    api(project(":pushpin-api"))
    api(project(":pushpin-client"))
    api(project(":pushpin-discovery"))
    implementation("org.springframework:spring-webflux")
}
