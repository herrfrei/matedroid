.PHONY: build install run clean test test-docker help

# Default target
help:
	@echo "Available targets:"
	@echo "  build       - Build debug APK"
	@echo "  install     - Build and install debug APK on connected device"
	@echo "  run         - Build, install, and launch the app"
	@echo "  clean       - Clean build artifacts"
	@echo "  test        - Run unit tests"
	@echo "  test-docker - Run unit tests in Docker container"

# Build debug APK
build:
	./gradlew assembleDebug

# Build and install debug APK on connected device
install: build
	adb install -r app/build/outputs/apk/debug/app-debug.apk

# Build, install, and launch the app
run: install
	adb shell am start -n com.matedroid/.MainActivity

# Clean build artifacts
clean:
	./gradlew clean

# Run unit tests
test:
	./gradlew testDebugUnitTest

# Run unit tests in Docker container
test-docker:
	docker build -f Dockerfile.test -t matedroid-test .
	docker run --rm matedroid-test
