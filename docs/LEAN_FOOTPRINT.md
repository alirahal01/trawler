# Why Trawler's dependency footprint is smaller than KtorMonitor's

This is a deliberate contrast, not an oversight. Trawler exists to be
dogfooded in a proprietary KMP app maintained at work, where every
dependency it drags in is scrutiny the whole app has to justify — so v1
carries only what it cannot do without:

- Kotlin Multiplatform tooling
- `io.ktor:ktor-client-core` — unavoidable; this is the framework being
  instrumented
- `kotlinx-coroutines-core`
- Compose Multiplatform
- `kotlinx-serialization-json` — `CapturedCall` is `@Serializable`

Checked directly against KtorMonitor's own root `build.gradle.kts`
(`CosminMihuMDC/KtorMonitor`): it applies both the `sqldelight` and
`koin.compiler` Gradle plugins, with real usage throughout the repo (not
just declared and unused). Trawler has neither, by design:

## No persistence layer (no SQLDelight)

`CallStore` is an interface with one shipped implementation,
`InMemoryCallStore(capacity: Int = 200)` — a `Mutex`-guarded, fixed-capacity
ring buffer. Nothing is written to disk. Capacity is fixed at construction,
not resizable at runtime.

The trade-off is real: captured calls don't survive a process restart, and
there's no query surface beyond "the last N calls." We accepted this
because Trawler's job is live debugging of the current session, not a
persistent traffic log — and because a persistence layer is exactly the
kind of thing that's easy to add later as an opt-in module (a
`CallStore` implementation backed by SQLDelight, say) without touching
`monitor-core`'s public shape, but expensive to bolt on by default and then
try to make optional.

## No DI container (no Koin)

Every type in this repo takes its dependencies as constructor parameters —
`NetworkMonitor(extensions = ..., redaction = ..., store = ...)`,
`ReplayExtension(client)`, `CurlExportExtension(onCurlGenerated)`. There is
no service locator, no `startKoin {}`, no annotation processor resolving a
dependency graph at runtime.

For a library meant to be embedded inside a host app that likely has its
own DI setup already, this matters beyond style: pulling in a second DI
framework alongside whatever the host app already uses is exactly the kind
of friction that makes a debugging tool annoying to adopt. Explicit
constructors compose with anything.

## What this buys, concretely

Every module in this repo can be understood by reading its constructor
signature. There is no framework magic to learn before contributing, and no
version of Koin or SQLDelight to keep in sync with whatever the host app is
already using. The cost — no persistence, more verbose wiring at the call
site — is paid once, by the integrator, at setup time. The benefit is paid
continuously, by everyone who has to reason about what a piece of code
depends on.
