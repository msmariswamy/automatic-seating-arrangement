# 🚀 Gradle Setup & Run Guide

## Quick Start (No Gradle Installation Required!)

This project includes **Gradle Wrapper**, so you don't need to install Gradle separately!

---

## Step 1: Verify Prerequisites

```bash
# Check Java version (must be 17+)
java -version

# Check PostgreSQL
psql --version
```

---

## Step 2: Setup Database

```bash
# Navigate to project directory
cd /Users/mariswamypillai/Documents/Github/personal/asr

# Run database initialization script
psql -U postgres -f database/init.sql
```

---

## Step 3: Build the Project

### Linux / macOS:
```bash
./gradlew clean build
```

### Windows:
```bash
gradlew.bat clean build
```

**First build will download dependencies (may take 2-3 minutes)**

---

## Step 4: Run the Application

### Linux / macOS:
```bash
./gradlew bootRun
```

### Windows:
```bash
gradlew.bat bootRun
```

**Wait for:** `Started SeatingArrangementApplication`

---

## Step 5: Access Application

1. Open browser: http://localhost:8080
2. Login with:
   - **Username:** `admin`
   - **Password:** `admin123`

---

## 📋 Common Gradle Commands

### Build Commands:
```bash
# Clean build
./gradlew clean build

# Build without tests
./gradlew clean build -x test

# Refresh dependencies
./gradlew build --refresh-dependencies
```

### Run Commands:
```bash
# Run application
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=prod'

# Run in debug mode
./gradlew bootRun --debug-jvm
```

### Test Commands:
```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests StudentServiceTest
```

### Other Commands:
```bash
# View project dependencies
./gradlew dependencies

# View available tasks
./gradlew tasks

# Check for dependency updates
./gradlew dependencyUpdates
```

---

## 🔧 Gradle Wrapper Explained

The Gradle Wrapper consists of:

- **gradlew** - Unix/Linux/macOS script
- **gradlew.bat** - Windows script
- **gradle/wrapper/gradle-wrapper.properties** - Wrapper configuration
- **gradle/wrapper/gradle-wrapper.jar** - Wrapper executable

### Benefits:
✅ No Gradle installation required
✅ Same Gradle version for all developers
✅ Guaranteed compatibility
✅ Easy CI/CD integration

### Updating Wrapper:
```bash
# Upgrade to latest Gradle version
./gradlew wrapper --gradle-version 8.5
```

---

## 🏗️ Project Structure with Gradle

```
automatic-seating-arrangement/
├── build.gradle                 # Build configuration
├── settings.gradle              # Project settings
├── gradle/
│   └── wrapper/                 # Gradle wrapper files
├── gradlew                      # Unix wrapper script
├── gradlew.bat                  # Windows wrapper script
├── build/                       # Build output (generated)
│   ├── classes/                 # Compiled classes
│   ├── libs/                    # JAR files
│   └── resources/               # Processed resources
├── src/
│   ├── main/
│   └── test/
└── database/
```

---

## 🐛 Troubleshooting Gradle Issues

### Issue: Permission denied (Linux/Mac)

```bash
# Make gradlew executable
chmod +x gradlew
```

### Issue: Gradle daemon issues

```bash
# Stop all Gradle daemons
./gradlew --stop

# Rebuild
./gradlew clean build
```

### Issue: Dependency resolution failure

```bash
# Clear Gradle cache
rm -rf ~/.gradle/caches

# Rebuild with fresh dependencies
./gradlew clean build --refresh-dependencies
```

### Issue: Out of memory

```bash
# Increase Gradle memory in gradle.properties
echo "org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m" > gradle.properties
```

### Issue: Build is slow

```bash
# Enable Gradle daemon and parallel builds
echo "org.gradle.daemon=true" >> gradle.properties
echo "org.gradle.parallel=true" >> gradle.properties
echo "org.gradle.caching=true" >> gradle.properties
```

---

## 📦 Building for Production

### Create executable JAR:
```bash
# Build production JAR
./gradlew clean build -x test

# Find JAR at:
build/libs/automatic-seating-arrangement-1.0.0.jar
```

### Run production JAR:
```bash
java -jar build/libs/automatic-seating-arrangement-1.0.0.jar
```

### With custom configuration:
```bash
java -jar build/libs/automatic-seating-arrangement-1.0.0.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

---

## 🐳 Docker Build with Gradle

### Option 1: Build then Docker
```bash
# Build JAR
./gradlew clean build -x test

# Create Dockerfile
cat > Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
EOF

# Build and run
docker build -t seating-app .
docker run -p 8080:8080 seating-app
```

### Option 2: Multi-stage Docker Build
```dockerfile
# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# Run stage
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

---

## 🔄 Gradle vs Maven Comparison

| Feature | Gradle | Maven |
|---------|--------|-------|
| Build file | `build.gradle` | `pom.xml` |
| Build command | `./gradlew build` | `mvn install` |
| Run command | `./gradlew bootRun` | `mvn spring-boot:run` |
| Clean | `./gradlew clean` | `mvn clean` |
| Test | `./gradlew test` | `mvn test` |
| Output dir | `build/` | `target/` |
| Wrapper | Built-in | Separate plugin |
| Build speed | Faster (incremental) | Slower |
| Flexibility | High (Groovy/Kotlin DSL) | Medium (XML) |

---

## 🎯 Gradle Best Practices

### 1. Always use the wrapper
```bash
# Good
./gradlew build

# Avoid (unless testing specific version)
gradle build
```

### 2. Leverage build cache
```bash
# Enable in gradle.properties
org.gradle.caching=true
```

### 3. Use parallel builds
```bash
# Enable in gradle.properties
org.gradle.parallel=true
```

### 4. Keep Gradle updated
```bash
./gradlew wrapper --gradle-version 8.5
```

### 5. Clean build when switching branches
```bash
git checkout feature-branch
./gradlew clean build
```

---

## 📚 Additional Resources

- [Gradle Official Documentation](https://docs.gradle.org/)
- [Spring Boot Gradle Plugin](https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/htmlsingle/)
- [Gradle Build Scans](https://scans.gradle.com/)

---

## ✅ Quick Reference Card

```bash
# Build
./gradlew clean build          # Full build with tests
./gradlew build -x test        # Build without tests
./gradlew assemble             # Build without running tests

# Run
./gradlew bootRun              # Run application
./gradlew bootRun --args='--spring.profiles.active=dev'

# Test
./gradlew test                 # Run all tests
./gradlew test --tests ClassName

# Clean
./gradlew clean                # Remove build directory

# Info
./gradlew tasks                # List all available tasks
./gradlew dependencies         # Show dependency tree
./gradlew properties           # Show project properties

# Troubleshooting
./gradlew --stop               # Stop Gradle daemon
./gradlew build --refresh-dependencies
chmod +x gradlew               # Make executable (Linux/Mac)
```

---

**Ready to build! Start with: `./gradlew clean build` 🚀**
