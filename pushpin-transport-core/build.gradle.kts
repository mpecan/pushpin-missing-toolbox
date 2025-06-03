plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

// Configure publishing for this module
configurePushpinPublishing(
    moduleName = "pushpin-transport-core",
    moduleDescription =
        "Core transport interfaces for Pushpin " +
            "- defines transport abstraction and health checking APIs",
)

dependencies {
    api(project(":pushpin-api"))
    api(project(":pushpin-client"))
    api(project(":pushpin-discovery"))
    implementation("org.springframework:spring-webflux")
}
