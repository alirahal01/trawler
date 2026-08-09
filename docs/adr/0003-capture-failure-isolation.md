# ADR-0003: Capture failures never propagate to the real request

## Status

Accepted.

## Context

`monitor-ktor` sits inside the request/response pipeline of the host app's
real `HttpClient`. Any exception it throws is, by construction, an exception
the host app's own request now has to deal with.

## Decision

Every side effect `TrawlerMonitor` performs — reading a body, generating an
id, calling `NetworkMonitor.capture()` — runs inside
`isolatingCaptureFailures`, which rethrows `CancellationException` (a
legitimate signal, not a capture failure) and swallows everything else:

```kotlin
private suspend inline fun isolatingCaptureFailures(block: suspend () -> Unit) {
    try {
        block()
    } catch (c: CancellationException) {
        throw c
    } catch (t: Throwable) {
        // Intentionally swallowed.
    }
}
```

This is a deliberate one-way door: a debugging/inspection tool must be
strictly additive. The alternative — letting capture errors propagate —
would surface Trawler's own bugs immediately during its development, but it
means a tool meant only to observe traffic has the power to break real
production requests, which is unacceptable for something that could ship in
a release build by accident. We chose safety over self-diagnosability.

## A concrete case this caught

Manual testing on a real Android emulator (build order step 7) found that
requests which fail *before* any `HttpResponse` is received — a connection
reset, a DNS failure, a timeout — used to disappear from Trawler entirely.
`ResponseObserver` only fires for responses that were actually received, and
nothing else in the plugin populated `CapturedCall.error`, despite the field
existing since the model was designed.

The fix moved id/timing bookkeeping into Ktor's `Send` hook, which wraps the
entire send-and-receive cycle, and wrapped its `proceed()` call the same way:
catch, capture an error `CapturedCall` via `isolatingCaptureFailures`, then
always rethrow. The real failure still reaches the host app exactly as
before — Trawler only ever gained visibility into it, never influence over
it. This is the isolation policy this ADR describes, just applied one layer
earlier than the original skeleton had it.
