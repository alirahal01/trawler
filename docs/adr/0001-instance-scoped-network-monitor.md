# ADR-0001: NetworkMonitor is instance-scoped, not a singleton

## Status

Accepted.

## Context

The original sketch (`NetworkMonitor.install(extensions = ...)`) implied a
process-wide singleton, matching how KtorMonitor and similar tools typically
work.

## Decision

`NetworkMonitor` is constructed explicitly —
`NetworkMonitor(extensions = ..., redaction = ..., store = ...)` — and the
same instance is passed to both the Ktor plugin installation
(`install(TrawlerMonitor) { monitor = myMonitor }`) and the viewer composable
(`NetworkMonitorUi(monitor = myMonitor)`). There is no global registry and no
static accessor.

`NetworkMonitor` itself lives in `monitor-extensions-api`, not `monitor-core`
or a dedicated module — its constructor takes `MonitorExtension`, which needs
Compose for `standalonePanel`, and `monitor-extensions-api` is the only
module that already depends on `monitor-core` (for `CallStore`/`CapturedCall`)
without creating a cycle. This wasn't decided up front; the module layout
originally left `NetworkMonitor` unassigned, and this is where the actual
dependency graph forced it once `monitor-ktor` needed to call `.capture()`.

## Consequences

This lets one app run multiple independent `HttpClient`s/monitors, makes
tests trivial (no global state to reset — every test in this repo constructs
its own `NetworkMonitor`), and matches the project's no-DI-container /
explicit-constructor-injection constraint. The cost is a slightly more
verbose call site than a one-line global install — acceptable since it's
paid once, at wiring time.
