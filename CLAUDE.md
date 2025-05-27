# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands
- Build: `./gradlew build`
- Run tests: `./gradlew test`
- Run single test: `./gradlew test --tests io.github.mpecan.pmt.controller.PushpinControllerTest`
- Run application: `./gradlew bootRun`

## Code Style
- Kotlin with Spring Boot
- Java 17 toolchain
- Package structure: io.github.mpecan.pmt.*
- Classes: PascalCase (PushpinServer)
- Functions: camelCase (getBaseUrl())
- Properties: camelCase (controlPort)
- Test methods: use backticks with descriptive names

## Dependency Management
- All dependency versions must be defined in the root gradle.properties file
- Submodules should inherit versions from the root module's dependency management configuration
- Never hardcode version numbers directly in submodule build.gradle.kts files
- Prefer constructor injection over field injection for better testability
- Use primary constructors with default values for dependencies where appropriate
- For external services (like AWS), inject clients or factories to allow mocking in tests
- For testing, use mockito-kotlin library with constructor injection instead of subclassing

## Testing
- JUnit 5 with Spring Boot Test
- Integration tests use Testcontainers
- Test classes: *Test for unit tests, *IntegrationTest for integration
- HTTP errors use standard Spring status codes
- Use mockito-kotlin for cleaner mocking syntax (e.g., use `whenever` instead of `when`)
- Prefer constructor injection to make tests more readable and maintainable
- Tests must be run and pass before any PR or commit
- Always verify API compatibility in external libraries by writing tests
- Use explicit versions in build.gradle.kts for all dependencies
- When working with external API clients (AWS, Kubernetes, etc.), use mocks in tests to avoid reliance on live services