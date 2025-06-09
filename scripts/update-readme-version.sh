#!/bin/bash

# Script to update version numbers in README files
# Usage: ./scripts/update-readme-version.sh <version>

set -e

if [ $# -eq 0 ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 1.0.0"
    exit 1
fi

VERSION=$1
echo "Updating README files to version: $VERSION"

# Function to update version in a file
update_file() {
    local file=$1
    echo "Updating $file..."
    
    # Update version header
    sed -i.bak "s/### Latest Version: .*/### Latest Version: $VERSION/" "$file"
    
    # Update Gradle Kotlin DSL dependencies
    sed -i.bak "s/implementation(\"io\.github\.mpecan:\(pushpin-[^:]*\):[^\"]*\")/implementation(\"io.github.mpecan:\1:$VERSION\")/g" "$file"
    
    # Update Gradle Groovy dependencies  
    sed -i.bak "s/implementation 'io\.github\.mpecan:\(pushpin-[^:]*\):[^']*'/implementation 'io.github.mpecan:\1:$VERSION'/g" "$file"
    
    # Update Maven dependencies with io.github.mpecan groupId
    # This handles multi-line Maven dependency blocks
    perl -i.bak -0pe 's|(<groupId>io\.github\.mpecan</groupId>\s*\n\s*<artifactId>pushpin-[^<]+</artifactId>\s*\n\s*<version>)[^<]+(</version>)|${1}'"$VERSION"'${2}|gs' "$file"
    
    # Remove backup files
    rm -f "${file}.bak"
}

# Update both README files
update_file "README.md"
update_file "README-LIBRARIES.md"

echo "Version update complete!"
echo "Files updated:"
echo "  - README.md"
echo "  - README-LIBRARIES.md"