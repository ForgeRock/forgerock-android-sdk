# AGENTS.md

This file provides guidance to AI coding agents (Claude Code, Cursor, GitHub Copilot, Gemini, etc.) when working with code in this repository. It follows the open AGENTS.md convention.

> **Note:** CLAUDE.md in this repository is a one-line redirect to AGENTS.md. Edit AGENTS.md only.
>
> **Plugin users:** Developers using the Ping SDK Agents plugin (ping-sdk-agents) receive additional implementation-level guidance via android-architect, android-engineer, android-code-reviewer, and related agents. AGENTS.md is the repo-level foundation that both plugin users and bare-agent users share — it is complete enough to stand alone without the plugin.

## Overview

This is the **legacy ForgeRock SDK v4.x** for Android — a callback-based SDK integrating with ForgeRock/PingAM and PingOne platforms. It targets Android API 23+ (Android 6.0 Marshmallow) and uses a Java + Kotlin mixed codebase.

> **Maintenance mode — READ THIS FIRST.**
> This repository entered maintenance mode on **April 15, 2026**. Only critical bug fixes and security updates are accepted. End-of-support is **April 15, 2028**.
> **New feature work belongs in [ping-android-sdk](https://github.com/ForgeRock/ping-android-sdk).** If you are an agent evaluating whether to add a new capability: do not add it here. Propose the change against `ping-android-sdk` instead and document the migration path.

## Platform & Toolchain

| Item | Version / value |
|---|---|
| JDK | 17 (Temurin; configured via `jvmToolchain(17)` in module build scripts) |
| Android Gradle Plugin (AGP) | 8.11.1 |
| Kotlin | 2.2.0 |
| Gradle wrapper | use `./gradlew`; never install Gradle globally |
| `minSdk` | 23 (Android 6.0 Marshmallow) |
| `compileSdk` | 36 |
| `targetSdk` (test options) | 35 |
| Group ID | `org.forgerock` |
| Current version | 4.8.6 |
| Kotlin code style | `official` (set in `gradle.properties`) |

## Build and Test Commands

```bash
# Full build
./gradlew clean build

# All unit tests with coverage (matches CI gate exactly)
./gradlew --rerun-tasks testDebugUnitTestCoverage --stacktrace --no-daemon
```

Always use `testDebugUnitTestCoverage`, never plain `test` or `testDebugUnitTest` alone — the `Coverage` suffix runs the Jacoco report generation step that CI checks.

```bash
# Per-module unit tests
./gradlew :forgerock-core:testDebugUnitTestCoverage --stacktrace --no-daemon
./gradlew :forgerock-auth:testDebugUnitTestCoverage --stacktrace --no-daemon
./gradlew :forgerock-authenticator:testDebugUnitTestCoverage --stacktrace --no-daemon
./gradlew :ping-protect:testDebugUnitTestCoverage --stacktrace --no-daemon

# Generate API reference documentation
./gradlew clean dokkaHtmlMultiModule          # HTML → build/api-reference/html/
./gradlew clean dokkaJavadocCollector         # JavaDoc → build/api-reference/javadoc/
```

## CI/CD

CI runs on every pull request and every push to `develop` / `master`. The gate is defined in `.github/workflows/ci.yaml` and delegates to:

| Workflow | Purpose |
|---|---|
| `build-and-test.yaml` | JDK 17 setup, `./gradlew --rerun-tasks testDebugUnitTestCoverage --stacktrace --no-daemon`, Codecov upload |
| `mend-sca-scan.yaml` | Software Composition Analysis (dependency vulnerabilities) |
| `mend-sast-scan.yaml` | Static Application Security Testing |
| `browserstack-prepare-artifacts.yaml` | Build and sign APKs for device-farm testing |
| `browserstack-run.yaml` | Execute E2E tests on BrowserStack |
| `browserstack-results.yaml` | Collect and report BrowserStack results |
| `publish-snapshot.yaml` | Publish a `-SNAPSHOT` to Sonatype (only on push to `develop` after all tests pass) |

A PR must pass `build-and-test` and both Mend scans before it can be merged.

## Architecture

This SDK uses a **callback-based orchestration model**, not coroutines or a sealed `Node` hierarchy (those belong to `ping-android-sdk`).

### Authentication flow

- Entry point: `FRAuth` / `FRUser`.
- Tree/Journey traversal is driven by `NodeListener<FRSession>` with three callbacks:
  - `onSuccess(result: FRSession)` — authentication succeeded.
  - `onException(e: AuthenticationException)` — unrecoverable error.
  - `onCallbackReceived(node: FRNode)` — AM returned a node; the caller reads the node's `Callback` list, fills in values, and calls `node.next(context, nodeListener)`.
- Callbacks (one per AM callback type) extend `AbstractValidatedCallback` or `Callback` and live in `org.forgerock.android.auth.callback.*`.

### Networking

- HTTP client: **OkHttp** (version 5.x). Not Ktor, not Retrofit.
- Interceptors are added to `OkHttpClient` via `OkHttpClientProvider`.

### Language mix

- Java and Kotlin coexist. Many core classes (`FRUser`, `FRAuth`, `NodeListener`) are Java; newer callbacks (Device Binding, WebAuthn) are Kotlin.
- Lombok is used in Java classes for boilerplate reduction (`@Getter`, `@Builder`, etc.). The build runs a `delombok` step before Dokka.

### Package root

All public API lives under `org.forgerock.android.auth.*`. Do not create classes under `com.pingidentity.*` in this repo.

### Storage

`SharedPreferences` and `EncryptedSharedPreferences` (via `androidx.security:security-crypto`). Not DataStore, not Room.

### Dependency pins (load-bearing — do not relax)

Root `build.gradle.kts` contains a `resolutionStrategy` block that forces specific versions of transitive dependencies. Each pin exists because of a CVE or a known compatibility issue. Do not remove or downgrade these pins without first re-verifying the original CVE is resolved in the newer version.

| Library | Forced version | Reason |
|---|---|---|
| `com.fasterxml.jackson.module:jackson-module-kotlin` | `2.15.0` | CVE-2022-40152 (via Dokka's transitive Jackson dependency) |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-xml` | `2.15.0` | CVE-2022-40152 (same) |
| `com.fasterxml.jackson.core:jackson-databind` | `2.15.0` | CVE-2022-40152 (same) |
| `junit:junit` | `4.13.2` | Consistent JUnit 4 version across all test configurations |
| `com.google.android.gms:play-services-basement` | `18.1.0` | CVE-2022-2390 (CWE-471 mutable element) |
| `org.mockito:mockito-core` | `3.12.4` | PowerMock incompatibility with Mockito 4.x+ |
| `com.google.code.gson:gson` | `2.13.1` | CVE-2025-53864 (transitive from `androidx.security:security-crypto`) |

## Project Layout

The following modules are declared in `settings.gradle.kts`:

| Module | Project path | Purpose |
|---|---|---|
| `forgerock-auth` | `:forgerock-auth` | Core OIDC client; Journey/AM orchestration; WebAuthn, Social Login, Device Binding, App Integrity callbacks |
| `forgerock-core` | `:forgerock-core` | Shared utilities, logging, storage, networking (OkHttp), and types used by all other modules |
| `forgerock-authenticator` | `:forgerock-authenticator` | MFA: Push notifications and OATH (TOTP/HOTP) mechanisms |
| `forgerock-auth-ui` | `:forgerock-auth-ui` | **DEPRECATED** — Pre-built UI components for rapid Journey prototyping. Do not modify. |
| `ping-protect` | `:ping-protect` | PingOne Protect fraud-signals integration |
| `forgerock-integration-tests` | `:forgerock-integration-tests` | Integration/instrumentation tests (run against a live AM instance) |
| `app` (e2e/app) | `:app` | Sample application for end-to-end BrowserStack testing |
| `authenticator` (samples/authenticator) | `:authenticator` | Sample authenticator app demonstrating Push + OATH |

Tests live in `<module>/src/test/` (unit) and `<module>/src/androidTest/` (instrumented). Integration tests require a live AM/AIC instance and BrowserStack — they are not run in the normal local development loop.

## Contributing

- Branch off `develop`; PRs target `develop`. Never commit directly to `develop` or `master`.
- Commits must be GPG-signed (`git commit -S`). Do not use `--no-gpg-sign`.
- Commit format: `[TYPE] Short description` — valid types: `feat`, `fix`, `docs`, `refactor`, `test`. Bracket notation `[TYPE]` matches recent commit history; colon notation `TYPE:` also appears in `CONTRIBUTING.md` — either is accepted.
- Every source file starts with the MIT copyright header:
  ```
  /*
   * Copyright (c) <year> - <year> Ping Identity Corporation. All rights reserved.
   *
   * This software may be modified and distributed under the terms
   * of the MIT license. See the LICENSE file for details.
   */
  ```
- Do not add new features — this is a maintenance-mode repo. Do not add coroutine-only public APIs (preserve `NodeListener` callback compatibility). Do not modify `forgerock-auth-ui` (deprecated). Do not introduce OkHttp alternatives or alternative storage solutions.
- See [CONTRIBUTING.md](./CONTRIBUTING.md) for the full PR checklist and [sdk-standards-of-practice](https://github.com/ForgeRock/sdk-standards-of-practice/blob/main/code-style/android-coding-standard.md) for the Android coding standard.
