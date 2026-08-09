# ADR-0002: Extensions only ever see redacted calls

## Status

Accepted.

## Context

`MonitorExtension.onCapture()` is the hook where an extension can transform
a `CapturedCall` before it's stored. Redaction (`RedactionConfig`, replacing
values for headers like `Authorization`/`Cookie`/`Set-Cookie` with the
literal string `"[REDACTED]"`) also has to happen somewhere in that same
path.

## Decision

`NetworkMonitor.capture()` applies `RedactionConfig` first, then folds the
result through every registered extension's `onCapture()`:

```kotlin
suspend fun capture(call: CapturedCall) {
    if (redaction.isIgnored(call.url)) return
    val redacted = redaction.apply(call)
    val transformed = extensions.fold(redacted) { acc, extension -> extension.onCapture(acc) }
    store.record(transformed)
}
```

Extensions — including first-party ones like `curl-export` — can never see a
raw `Authorization` header, cookie, or other redacted value. Only the
`[REDACTED]` marker, already in place by the time `onCapture()` runs.

## Consequences

This closes off an obvious leak vector: a careless or malicious extension
can't exfiltrate secrets through curl-export, a custom panel, or a log — the
generated curl command literally contains the string `[REDACTED]`, not a
token.

The direct cost lands on `replay`: it cannot auto-restore original auth
headers when refiring a call. Its edit view loads redacted fields as an
editable `[REDACTED]` placeholder (`ReplayDraft.from()` carries the literal
string straight through), and an unedited replay goes out with that literal
value and fails obviously (401/403) rather than silently sending a broken or
missing header. We accepted this as the correct default: a debugging tool
that can silently leak credentials is a worse outcome than a replay that
requires re-entering a token by hand.
