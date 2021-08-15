# kmm-frozen-inspection

Test plugin for Android Studio that includes two code inspections that should help working with concurrency in Kotlin Multiplatform:
1. Highlights variables usage in frozen types: objects, companion objects and enums.
1. Highlights lambda of Worker, that captures outer scope

## Usage

1. Download repository
2. Open with Android Studio
3. Select the runIde task in Gradle, it will launch a Development Instance of the IDE with the plugin enabled

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
