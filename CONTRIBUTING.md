# Contributing to Pushpin Missing Toolbox

First off, thank you for considering contributing to Pushpin Missing Toolbox! It's people like you that make this project such a great tool.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Process](#development-process)
- [Style Guidelines](#style-guidelines)
- [Testing](#testing)
- [Pull Request Process](#pull-request-process)
- [Project Structure](#project-structure)

## Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

### Our Standards

- Be respectful and inclusive
- Welcome newcomers and help them get started
- Focus on what is best for the community
- Show empathy towards other community members

## Getting Started

### Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- Git
- Your favorite IDE (IntelliJ IDEA recommended for Kotlin development)

### Setting Up Your Development Environment

1. **Fork the repository**
   ```bash
   # Click the 'Fork' button on GitHub
   ```

2. **Clone your fork**
   ```bash
   git clone https://github.com/YOUR_USERNAME/pushpin-missing-toolbox.git
   cd pushpin-missing-toolbox
   ```

3. **Add the upstream repository**
   ```bash
   git remote add upstream https://github.com/mpecan/pushpin-missing-toolbox.git
   ```

4. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

5. **Start the development environment**
   ```bash
   docker-compose up -d
   ./gradlew build
   ```

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates. When you create a bug report, include as many details as possible:

- **Use a clear and descriptive title**
- **Describe the exact steps to reproduce the problem**
- **Provide specific examples**
- **Describe the behavior you observed and what you expected**
- **Include logs and stack traces**
- **Note your environment** (OS, Java version, Docker version)

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion:

- **Use a clear and descriptive title**
- **Provide a detailed description of the proposed functionality**
- **Explain why this enhancement would be useful**
- **List any alternatives you've considered**

### Your First Code Contribution

Unsure where to begin? Look for issues labeled:

- `good first issue` - Good for newcomers
- `help wanted` - Extra attention needed
- `documentation` - Documentation improvements

### Pull Requests

1. **Follow the style guidelines**
2. **Write tests for new functionality**
3. **Update documentation as needed**
4. **Ensure all tests pass**
5. **Write a clear commit message**

## Development Process

### Building the Project

```bash
# Build all modules
./gradlew build

# Build a specific module
./gradlew :server:build

# Run tests
./gradlew test

# Run integration tests
./gradlew integrationTest
```

### Running Locally

```bash
# Start Pushpin servers
docker-compose up -d

# Run the application
./gradlew bootRun

# Or run with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Project Modules

- `server/` - Main Spring Boot application
- `pushpin-api/` - GRIP protocol implementation
- `pushpin-client/` - Client library for publishing messages
- `pushpin-testcontainers/` - Testcontainers implementation
- `discovery-*` - Service discovery modules
- `pushpin-security-*` - Security modules
- `pushpin-transport-*` - Transport modules

## Style Guidelines

### Kotlin Style

We follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with some additions:

```kotlin
// Classes: PascalCase
class PushpinServer

// Functions: camelCase
fun getBaseUrl(): String

// Properties: camelCase
val controlPort: Int

// Constants: UPPER_SNAKE_CASE
const val DEFAULT_TIMEOUT = 5000

// Test methods: use backticks for descriptive names
@Test
fun `should publish message successfully when server is healthy`() {
    // test implementation
}
```

### Code Organization

```kotlin
// Order of class members
class ExampleService {
    // 1. Companion object
    companion object {
        private val logger = LoggerFactory.getLogger(ExampleService::class.java)
    }
    
    // 2. Properties
    private val timeout = Duration.ofSeconds(30)
    
    // 3. Initialization block
    init {
        // initialization code
    }
    
    // 4. Constructors
    constructor(param: String) : this()
    
    // 5. Public methods
    fun publicMethod() { }
    
    // 6. Private methods
    private fun helperMethod() { }
}
```

### Documentation

- All public APIs must have KDoc comments
- Include examples in documentation when helpful
- Keep comments concise and meaningful

```kotlin
/**
 * Publishes a message to all healthy Pushpin servers.
 * 
 * @param message The message to publish
 * @return Mono<Boolean> indicating success
 * @throws PushpinException if no healthy servers are available
 * 
 * @sample
 * ```
 * val message = Message.simple("channel", mapOf("data" to "hello"))
 * pushpinService.publishMessage(message).block()
 * ```
 */
fun publishMessage(message: Message): Mono<Boolean>
```

## Testing

### Test Requirements

- All new features must have tests
- Maintain or improve code coverage
- Write both unit and integration tests when applicable

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :server:test

# Run a specific test class
./gradlew test --tests "*.PushpinServiceTest"

# Run with coverage
./gradlew test jacocoTestReport
```

### Writing Tests

```kotlin
@SpringBootTest
@Testcontainers
class PushpinIntegrationTest {
    
    @Container
    val pushpinContainer = PushpinContainerBuilder()
        .withPreset(PushpinPresets.minimal())
        .build()
    
    @Test
    fun `should handle message publishing`() {
        // Given
        val message = TestDataBuilder.message()
        
        // When
        val result = pushpinService.publishMessage(message).block()
        
        // Then
        assertThat(result).isTrue()
    }
}
```

### Integration Tests

See the [Testing Guide](docs/Testing.md) for detailed information about:
- Using the pushpin-testcontainers module
- Writing integration tests
- Testing patterns and best practices

## Pull Request Process

1. **Update your fork**
   ```bash
   git fetch upstream
   git checkout main
   git merge upstream/main
   ```

2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature
   ```

3. **Make your changes**
   - Write code following style guidelines
   - Add tests
   - Update documentation

4. **Commit your changes**
   ```bash
   git add .
   git commit -m "feat: add new feature

   - Detailed description of what changed
   - Why the change was made
   - Any breaking changes"
   ```

   Follow [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat:` - New feature
   - `fix:` - Bug fix
   - `docs:` - Documentation changes
   - `style:` - Code style changes (formatting, etc)
   - `refactor:` - Code refactoring
   - `perf:` - Performance improvements
   - `test:` - Test additions or corrections
   - `build:` - Build system changes
   - `ci:` - CI/CD changes
   - `chore:` - Other changes

5. **Push to your fork**
   ```bash
   git push origin feature/your-feature
   ```

6. **Create a Pull Request**
   - Go to the original repository
   - Click "New Pull Request"
   - Select your fork and branch
   - Fill in the PR template
   - Link any related issues

### PR Requirements

- [ ] Tests pass (`./gradlew test`)
- [ ] Code follows style guidelines
- [ ] Documentation is updated
- [ ] Commit messages follow conventions
- [ ] PR description explains the changes

### Review Process

1. Automated checks will run
2. Maintainers will review your code
3. Address any feedback
4. Once approved, your PR will be merged

## Project Structure

```
pushpin-missing-toolbox/
├── server/                     # Main application
│   ├── src/main/kotlin/       # Application code
│   ├── src/test/kotlin/       # Tests
│   └── build.gradle.kts       # Build configuration
├── pushpin-api/               # GRIP protocol library
├── pushpin-client/            # Client library
├── pushpin-testcontainers/    # Testing utilities
├── discovery-*/               # Discovery modules
├── pushpin-security-*/        # Security modules
├── pushpin-transport-*/       # Transport modules
├── docs/                      # Documentation
├── docker-compose.yml         # Development environment
└── gradlew                    # Build script
```

## Release Process

Releases are managed by maintainers:

1. Update version in `gradle.properties`
2. Update CHANGELOG.md
3. Create a release tag
4. GitHub Actions will build and publish

## Getting Help

- **Discord**: Join our community server
- **GitHub Issues**: For bugs and features
- **Documentation**: Check the [docs](docs/) folder
- **Examples**: See [Examples.md](docs/Examples.md)

## Recognition

Contributors are recognized in:
- The project README
- Release notes
- Our contributors page

Thank you for contributing to Pushpin Missing Toolbox! 🎉