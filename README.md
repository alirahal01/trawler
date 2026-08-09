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
container, no persistence layer in core — see `docs/adr/`).

## Modules

- `monitor-core` — `CapturedCall` model and the `CallStore` ring buffer. No
  Ktor or Compose dependency.
- `monitor-ktor` — the Ktor `ClientPlugin` that captures requests/responses.
- `monitor-ui-compose` — the Compose Multiplatform viewer UI.
- `monitor-extensions-api` — the `MonitorExtension` contract.
- `extensions/curl-export`, `extensions/replay`, `extensions/preset-playground`
  — first-party extensions built against that contract.
- `sample-app` — Android + iOS + Desktop sample app exercising the library.

## Status

Early scaffold — see `docs/adr/` for the foundational decisions and open
`.issues/` (or the project board) for build progress.

## License

Apache-2.0 — see [LICENSE](LICENSE).
