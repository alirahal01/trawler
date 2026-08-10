# Trawler

A lean, extensible network request monitor for Ktor clients on Kotlin
Multiplatform (Android, iOS, Desktop/JVM). Capture HTTP traffic in-app,
inspect it in a Compose Multiplatform viewer, and extend it — replay/edit-
resend, preset-driven API firing, custom tagging, custom body renderers —
without forking the library.

Trawler is a clean-room alternative to
[KtorMonitor](https://github.com/CosminMihuMDC/KtorMonitor) and
[Wormholy](https://github.com/pmusolino/wormholy) (iOS-only): same problem
space, no shared code, and a deliberately smaller dependency footprint (no DI
container, no persistence layer in core — see
[docs/LEAN_FOOTPRINT.md](docs/LEAN_FOOTPRINT.md)).

See [CONTEXT.md](CONTEXT.md) for the project's glossary and
[docs/adr/](docs/adr/) for the foundational architectural decisions.

## Installation

Available on JitPack (`v0.1.2`):

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.github.alirahal01.trawler:monitor-core:v0.1.2")
            implementation("com.github.alirahal01.trawler:monitor-ktor:v0.1.2")
            implementation("com.github.alirahal01.trawler:monitor-extensions-api:v0.1.2")
            implementation("com.github.alirahal01.trawler:monitor-ui-compose:v0.1.2")

            // optional first-party extensions
            implementation("com.github.alirahal01.trawler:curl-export:v0.1.2")
            implementation("com.github.alirahal01.trawler:replay:v0.1.2")
            implementation("com.github.alirahal01.trawler:preset-playground:v0.1.2")
        }
    }
}
```

Each module resolves to the right Android/iOS/Desktop variant automatically
through Gradle's normal KMP dependency resolution — verified directly against
the built JitPack artifacts, not just documented from assumption.

## Modules

- `monitor-core` — `CapturedCall` model and the `CallStore` ring buffer. No
  Ktor or Compose dependency.
- `monitor-ktor` — the Ktor `ClientPlugin` that captures requests/responses.
- `monitor-ui-compose` — the Compose Multiplatform viewer UI.
- `monitor-extensions-api` — the `MonitorExtension` contract.
- `extensions/curl-export`, `extensions/replay`, `extensions/preset-playground`
  — first-party extensions built against that contract.
- `sample-app` — Android + iOS + Desktop sample app exercising the library.

## Running the sample app

- **Desktop**: `./gradlew :sample-app:desktopApp:run`
- **Android**: `./gradlew :sample-app:androidApp:installDebug`, or open the repo
  root in Android Studio and run the `androidApp` configuration.
- **iOS**: `sample-app/iosApp/` is an [XcodeGen](https://github.com/yonaskolb/XcodeGen)
  project — `project.yml` is the source of truth, not the generated
  `.xcodeproj`. First time, or after editing `project.yml`:
  ```
  brew install xcodegen
  cd sample-app/iosApp && xcodegen generate
  ```
  Then open `TrawlerSample.xcodeproj` in Xcode and run. The Kotlin framework
  builds automatically via a Run Script build phase
  (`embedAndSignAppleFrameworkForXcode`) — no separate Gradle step needed.

## Status

Core capture pipeline, redaction, the Compose viewer, and all three
first-party extensions (`curl-export`, `replay`, `preset-playground`) are
built and tested. Published to JitPack as `v0.1.2`; not yet on Maven Central.

## License

Apache-2.0 — see [LICENSE](LICENSE).
