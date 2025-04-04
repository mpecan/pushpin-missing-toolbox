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

## Testing
- JUnit 5 with Spring Boot Test
- Integration tests use Testcontainers
- Test classes: *Test for unit tests, *IntegrationTest for integration
- HTTP errors use standard Spring status codes