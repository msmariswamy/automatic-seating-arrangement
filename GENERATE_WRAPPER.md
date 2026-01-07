# Generating Gradle Wrapper

Since the Gradle wrapper scripts (`gradlew` and `gradlew.bat`) are binary files and the wrapper JAR, you need to generate them.

## Method 1: Using Gradle (if installed)

If you have Gradle installed:

```bash
cd /Users/mariswamypillai/Documents/Github/personal/asr
gradle wrapper --gradle-version 8.5
```

This will generate:
- `gradlew` (Unix/Linux/macOS script)
- `gradlew.bat` (Windows script)
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties` (already created)

## Method 2: Manual Download

If you don't have Gradle installed, download the wrapper files:

### 1. Download Gradle Distribution

```bash
# Create gradle/wrapper directory if it doesn't exist
mkdir -p gradle/wrapper

# Download Gradle 8.5 wrapper JAR
curl -L https://services.gradle.org/distributions/gradle-8.5-bin.zip -o gradle-8.5.zip
unzip gradle-8.5.zip
./gradle-8.5/bin/gradle wrapper --gradle-version 8.5
rm -rf gradle-8.5 gradle-8.5.zip
```

### 2. Or use this script:

```bash
#!/bin/bash

# Download and setup Gradle wrapper
GRADLE_VERSION=8.5
GRADLE_ZIP="gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/${GRADLE_ZIP}"

echo "Downloading Gradle ${GRADLE_VERSION}..."
curl -L ${GRADLE_URL} -o ${GRADLE_ZIP}

echo "Extracting..."
unzip -q ${GRADLE_ZIP}

echo "Generating wrapper..."
./gradle-${GRADLE_VERSION}/bin/gradle wrapper --gradle-version ${GRADLE_VERSION}

echo "Cleaning up..."
rm -rf gradle-${GRADLE_VERSION} ${GRADLE_ZIP}

echo "✓ Gradle wrapper generated successfully!"
echo "You can now run: ./gradlew clean build"
```

Save this as `setup-gradle-wrapper.sh` and run:
```bash
chmod +x setup-gradle-wrapper.sh
./setup-gradle-wrapper.sh
```

## Method 3: Quick Start (Recommended)

Since you already have `build.gradle` and `gradle/wrapper/gradle-wrapper.properties`:

1. **Install Gradle temporarily** (easiest):

   **macOS:**
   ```bash
   brew install gradle
   cd /Users/mariswamypillai/Documents/Github/personal/asr
   gradle wrapper
   ```

   **Linux:**
   ```bash
   sudo apt install gradle
   cd /Users/mariswamypillai/Documents/Github/personal/asr
   gradle wrapper
   ```

   **Windows (using Chocolatey):**
   ```bash
   choco install gradle
   cd C:\path\to\asr
   gradle wrapper
   ```

2. **Generate wrapper** (one-time):
   ```bash
   gradle wrapper --gradle-version 8.5
   ```

3. **Verify wrapper created**:
   ```bash
   ls -la gradlew gradlew.bat gradle/wrapper/
   ```

4. **Now you can use wrapper** (no Gradle installation needed anymore):
   ```bash
   ./gradlew clean build
   ```

## Verification

After generating, verify with:

```bash
# Check files exist
ls -la gradlew gradlew.bat
ls -la gradle/wrapper/

# Test wrapper
./gradlew --version
```

You should see Gradle 8.5 version info.

## Next Steps

Once wrapper is generated:

1. Build project: `./gradlew clean build`
2. Run application: `./gradlew bootRun`
3. Access: http://localhost:8080

---

**Note:** The wrapper files are committed to version control (except the JAR which is generated), so other developers won't need to repeat this process after cloning the repository.
