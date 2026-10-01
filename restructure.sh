#!/bin/bash
# Runtime: Termux / Linux Shell
# Keterangan: Skrip untuk menyusun ulang struktur proyek menjadi standar Android Studio / Gradle di root repository.

echo "[+] Memulai restrukturisasi direktori proyek yafe-tool..."

# 1. Membuat struktur direktori standar Android Gradle
mkdir -p app/src/main/java/com/redz/bpbot
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/xml
mkdir -p app/src/main/res/layout
mkdir -p build
mkdir -p docs

# 2. Memindahkan atau membuat file Java ke lokasi Android standard (app/src/main/java)
if [ -d "botapp/src/com/redz/8bpbot" ]; then
    cp -r botapp/src/com/redz/8bpbot/* app/src/main/java/com/redz/bpbot/ 2>/dev/null || true
elif [ -d "android/src/com/redz/bpbot" ]; then
    cp -r android/src/com/redz/bpbot/* app/src/main/java/com/redz/bpbot/ 2>/dev/null || true
fi

# 3. Memindahkan file res & AndroidManifest
if [ -f "botapp/AndroidManifest.xml" ]; then
    cp botapp/AndroidManifest.xml app/src/main/AndroidManifest.xml
elif [ -f "android/AndroidManifest.xml" ]; then
    cp android/AndroidManifest.xml app/src/main/AndroidManifest.xml
fi

if [ -d "botapp/res" ]; then
    cp -r botapp/res/* app/src/main/res/ 2>/dev/null || true
elif [ -d "android/res" ]; then
    cp -r android/res/* app/src/main/res/ 2>/dev/null || true
fi

# 4. Membuat file root settings.gradle
cat << 'EOF' > settings.gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "yafe-tool"
include ':app'
EOF

# 5. Membuat file root build.gradle
cat << 'EOF' > build.gradle
plugins {
    id 'com.android.application' version '8.1.0' apply false
}
EOF

# 6. Membuat file app/build.gradle
cat << 'EOF' > app/build.gradle
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.redz.bpbot'
    compileSdk 34

    defaultConfig {
        applicationId "com.redz.bpbot"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
}
EOF

# 7. Membuat file helper di folder build/ dan docs/
cat << 'EOF' > build/build.sh
#!/bin/bash
./gradlew assembleDebug
EOF
chmod +x build/build.sh

cat << 'EOF' > build/sign.sh
#!/bin/bash
echo "[+] Script for signing APK"
EOF
chmod +x build/sign.sh

cat << 'EOF' > docs/SETUP.md
# Setup Guide for yafe-tool
1. Clone repository
2. Run `./gradlew assembleDebug` or use GitHub Actions to build APK.
EOF

# 8. Membuat GitHub Actions Workflow (.github/workflows/android.yml)
mkdir -p .github/workflows
cat << 'EOF' > .github/workflows/android.yml
name: Build Android APK

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v5
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle Wrapper
        run: |
          gradle wrapper --gradle-version 8.5
          chmod +x gradlew

      - name: Build APK with Gradle
        run: |
          ./gradlew assembleDebug

      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: yafe-tool-apk
          path: app/build/outputs/apk/debug/app-debug.apk
EOF

echo "[+] Selesai! Structure yafe-tool disesuaikan dengan standar Android Gradle."

