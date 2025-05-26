# GitHub Actions CI Pipeline

This directory contains GitHub Actions workflow configurations for the Pushpin Missing Toolbox project.

## CI Workflow (`ci.yml`)

The continuous integration workflow does the following:

### Build Job
- Runs on a matrix of operating systems: Ubuntu, Windows, and macOS
- Sets up JDK 17
- Validates the Gradle wrapper
- Builds the project
- Runs all tests
- Uploads test reports as artifacts

### Lint Job
- Runs Ktlint checks to ensure code style consistency
- Applied to all Kotlin files in the project

### Dependency Check Job
- Checks for outdated dependencies
- Generates a dependency update report
- Uploads the report as an artifact

## Workflow Triggers

The workflow triggers on:
- Pull requests to the `main` branch
- Pushes to the `main` branch

## Local Development

To run the same checks locally:

```bash
# Build and test
./gradlew build

# Run just the tests
./gradlew test

# Run linting
./gradlew ktlintCheck

# Check for dependency updates
./gradlew dependencyUpdates
```

## Artifacts

The CI pipeline generates several artifacts:
- Test reports (HTML format in `build/reports/tests/`)
- Test results (XML format in `build/test-results/`)
- Dependency updates report (JSON format in `build/reports/dependencyUpdates/`)

These artifacts help diagnose issues when tests fail or identify outdated dependencies that could be updated.