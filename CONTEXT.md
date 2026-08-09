# Trawler

A Kotlin Multiplatform library that captures Ktor HTTP client traffic,
exposes it through a Compose Multiplatform viewer, and lets host apps plug
in custom behavior via extensions — a clean-room, leaner alternative to
KtorMonitor and Wormholy.

## Language

**CapturedCall**:
The record of one request/response pair: method, url, headers, bodies,
status, timing, and error. Stored *after* redaction has already run —
nothing that reads a `CapturedCall` (an extension, the viewer) ever sees a
raw, unredacted value.
_Avoid_: Call, Request, Transaction

**CallStore**:
The interface owning the fixed-capacity ring buffer of `CapturedCall`s,
exposing `record()`, `observe(): StateFlow<List<CapturedCall>>`, and
`clear()`. Capacity is fixed at construction. The default implementation
(`InMemoryCallStore`) is `Mutex`-guarded, since writes can land on any
thread the Ktor engine uses while the UI reads on the main thread.
_Avoid_: Repository, Cache, Log

**NetworkMonitor**:
The instance-scoped facade a host app constructs explicitly and passes to
both the Ktor plugin installation and the viewer composable — wiring one
`CallStore`, one `RedactionConfig`, and a list of `MonitorExtension`s. Never
a process-wide singleton; an app may construct more than one, one per
`HttpClient` it wants to inspect independently (ADR-0001). Owns `capture()`,
which applies redaction and then folds the result through every extension's
`onCapture()` before storing it (ADR-0002).
_Avoid_: Inspector, Sniffer

**TrawlerMonitor**:
The Ktor `ClientPlugin` (`monitor-ktor`) a host app installs on its real
`HttpClient` — `install(TrawlerMonitor) { monitor = myNetworkMonitor }`. Not
to be confused with `NetworkMonitor`: `TrawlerMonitor` is the thing that
observes traffic on the wire; `NetworkMonitor` is the thing that stores and
serves what was observed. Every side effect it performs is wrapped so a bug
here can never fail, delay, or alter the host app's real HTTP call
(ADR-0003).
_Avoid_: Plugin (too generic — always say "the Ktor plugin" or
`TrawlerMonitor`), Interceptor

**RedactionConfig**:
The process of replacing sensitive header or body values (`Authorization`,
`Cookie`, `Set-Cookie` by default) with the literal marker `"[REDACTED]"`
before a `CapturedCall` is stored, plus body truncation and per-host
skipping (`ignoredHosts`, suffix-matched). Runs once, inside
`NetworkMonitor.capture()` — nothing downstream (a `MonitorExtension`, the
viewer) ever sees the raw value.
_Avoid_: Masking, Scrubbing, Filtering

**MonitorExtension**:
The extension contract host apps implement to add behavior (`curl-export`,
`replay`, `preset-playground`, or a custom one) without forking Trawler:
`onCapture()` to transform a captured call, `actions()` to attach buttons,
and an optional `standalonePanel` for a feature that isn't tied to any one
call.
_Avoid_: Plugin (reserved for the Ktor `ClientPlugin`/`TrawlerMonitor`),
Addon

**CallAction**:
A single action a `MonitorExtension` attaches to a `CapturedCall`: a label
plus a suspend callback the viewer invokes and forgets about. Carries no
structured return value — the extension is responsible for any resulting UI
effect itself (its own `standalonePanel`, a host-supplied callback, etc.).
_Avoid_: Command, Handler

**Endpoint / EndpointPreset**:
`preset-playground`'s domain model: an `Endpoint` (method, url, label) with
zero or more named `EndpointPreset`s (headers, body), declared as plain
Kotlin by the host app — type-safe and refactor-safe, no JSON/YAML file or
parser involved. Fireable independent of whether that endpoint was ever
captured live; not tied to a `CapturedCall` the way `replay`/`curl-export`
are.
_Avoid_: Fixture, Mock, Template, Config, Schema

**ReplayDraft**:
`replay`'s editable snapshot of a call — method, url, headers flattened to
one editable row per value, and the request body decoded to text. Redacted
fields carry the literal `"[REDACTED]"` straight through from the
`CapturedCall` they were built from; if the user doesn't overwrite it before
sending, the refire goes out with that literal value (ADR-0002).
_Avoid_: Request builder, Snapshot
