# Release Setup Documentation

## Overview

This document describes the release automation setup for Pushpin Missing Toolbox using release-please, GitHub Packages, and Maven Central.

## Components

### 1. Release Please Configuration

- **`.release-please-config.json`**: Configures release-please to manage versions
- **`.release-please-manifest.json`**: Tracks the current version
- **`.github/workflows/release-please.yml`**: Workflow that creates release PRs

Release-please will:
- Monitor commits to `main` branch
- Create/update a release PR with changelog
- Update version in `gradle.properties` when PR is merged
- Create GitHub releases and tags

### 2. Publishing Configuration

All library modules (excluding `server`) are configured to publish to:
- **GitHub Packages**: For all releases
- **Maven Central**: For stable releases (non-pre-release)

Configuration is in root `build.gradle.kts`:
- Maven publication with POM details
- GPG signing for Maven Central
- Nexus publishing plugin for staging

### 3. Release Workflow

**`.github/workflows/release.yml`** triggers when a GitHub release is published:
1. Publishes all libraries to GitHub Packages
2. Publishes to Maven Central (for non-prerelease releases)
3. Builds Docker image using the JAR from build stage
4. Pushes Docker image to GitHub Container Registry

### 4. Version Management

- Version is maintained in `gradle.properties`
- Release-please updates it automatically
- All modules use the same version number
- Format: `major.minor.patch` (e.g., `0.0.1`)

## Required Secrets

Configure these in GitHub repository settings under Settings → Secrets and variables → Actions:

### 1. Maven Central (Sonatype OSSRH) Credentials
- `OSSRH_USERNAME`: Your Sonatype OSSRH username
- `OSSRH_PASSWORD`: Your Sonatype OSSRH password

To get these:
1. Create an account at https://issues.sonatype.org
2. Create a ticket to claim your namespace (e.g., `io.github.mpecan`)
3. Use your JIRA credentials as OSSRH credentials

### 2. GPG Signing Key Setup

#### Generate a GPG Key (if you don't have one):
```bash
# Generate a new GPG key
gpg --full-generate-key

# Choose:
# - RSA and RSA (default)
# - 4096 bits
# - Key does not expire (or set expiration)
# - Your real name and email

# List your keys to find the key ID
gpg --list-secret-keys --keyid-format=long

# Example output:
# sec   rsa4096/1234567890ABCDEF 2024-01-01 [SC]
#       Key fingerprint = XXXX XXXX XXXX XXXX XXXX  XXXX XXXX XXXX XXXX XXXX
# uid                 [ultimate] Your Name <your.email@example.com>
```

#### Export and Encode the Key:
```bash
# Export your private key (replace with your key ID)
gpg --export-secret-keys -a 1234567890ABCDEF > private-key.asc

# Base64 encode the key for GitHub
base64 -i private-key.asc | pbcopy  # macOS (copies to clipboard)
# OR
base64 private-key.asc > private-key-base64.txt  # Linux/other

# IMPORTANT: Delete the private key file after copying!
rm private-key.asc
```

#### Publish Your Public Key:
```bash
# Send to key servers (required for Maven Central)
gpg --keyserver keyserver.ubuntu.com --send-keys 1234567890ABCDEF
gpg --keyserver keys.openpgp.org --send-keys 1234567890ABCDEF
gpg --keyserver pgp.mit.edu --send-keys 1234567890ABCDEF
```

#### Configure GitHub Secrets:
- `SIGNING_KEY`: The base64-encoded private key (from the export step above)
- `SIGNING_PASSWORD`: The passphrase you used when creating the GPG key

## Release Process

### Snapshot Releases (Continuous)

Every push to `main` branch:
1. Checks if the current version ends with `-SNAPSHOT`
2. If it's a SNAPSHOT version, publishes all libraries to:
   - GitHub Packages (snapshot versions)
   - Sonatype Snapshots Repository (Maven Central snapshots)
3. If it's NOT a SNAPSHOT version, skips publishing (release versions are handled by the release workflow)

This allows users to test the latest changes before official releases. After a release, you should manually update the version in `gradle.properties` to the next SNAPSHOT version (e.g., `0.0.2-SNAPSHOT`).

### Official Releases

1. **Development**: Merge PRs to `main` using conventional commits:
   - `feat:` for new features (bumps minor)
   - `fix:` for bug fixes (bumps patch)
   - `feat!:` or `fix!:` for breaking changes (bumps major)

2. **Release PR**: Release-please automatically creates/updates a PR with:
   - Updated version in `gradle.properties`
   - Generated `CHANGELOG.md`
   - Release notes

3. **Release**: When the release PR is merged:
   - GitHub release and tag are created
   - Libraries are published to GitHub Packages
   - Libraries are published to Maven Central (if stable)
   - Docker image is built and pushed

## Docker Image

The Docker image:
- Is built using the server JAR from the Gradle build stage
- Is tagged with semantic versions (e.g., `1.0.0`, `1.0`, `1`, `latest`)
- Is pushed to `ghcr.io/mpecan/pushpin-missing-toolbox/pushpin-missing-toolbox`

## Using Snapshot Versions

To use snapshot versions in your project:

### From GitHub Packages:
```gradle
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/mpecan/pushpin-missing-toolbox")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}

dependencies {
    implementation("io.github.mpecan:pushpin-client:0.0.1-SNAPSHOT")
}
```

### From Sonatype Snapshots:
```gradle
repositories {
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencies {
    implementation("io.github.mpecan:pushpin-client:0.0.1-SNAPSHOT")
}
```

## Testing the Setup

1. Make a commit with conventional format
2. Check that release-please creates a PR
3. Merge the PR and verify:
   - GitHub release is created
   - Packages appear in GitHub Packages
   - Docker image is available in GHCR
4. For snapshots, push to main and verify:
   - CI publishes to both repositories
   - Artifacts are available with -SNAPSHOT suffix

## Troubleshooting

- If signing fails: Ensure GPG key is properly base64 encoded
- If Maven Central fails: Check OSSRH credentials and namespace permissions
- If Docker build fails: Verify the JAR pattern matches the actual output