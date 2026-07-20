# Minimum Requirements

These are draft requirements and will be refined as the emulator matures. They are intentionally strict about one thing: **Vulkan support is mandatory, with no fallback.**

## Hardware / driver requirements

- A GPU and driver supporting **Vulkan 1.2 or newer**. There is no OpenGL fallback and none is planned, ever.
- A 64-bit CPU. Exact minimum (clock speed, core count) will be published once performance benchmarking begins in Phase 2 of the roadmap.
- Enough free storage for the emulator, save data, and any user-provided game images (varies by game/format; no fixed number yet).

## Operating systems (initial targets)

- Windows 10/11 (x86_64)
- A modern Linux distribution with Vulkan driver support (x86_64)
- macOS (x86_64), with Apple Silicon/arm64 + Metal support planned for later
- Android (version requirements to be defined alongside the Android packaging work)

## Java runtime

End users are not expected to install Java separately — releases are intended to bundle a compatible runtime (Java 21+). Contributors building from source need a Java 21+ JDK and Gradle.

## Invalid reports

**Reports of problems caused by hardware or drivers that do not meet the requirements above are invalid and will be closed.** The most common example: a GPU/driver without Vulkan support. Since Vulkan is a hard requirement with no fallback path, "it doesn't work because my GPU has no Vulkan support" is a known limitation, not a bug.

If you're unsure whether your hardware qualifies, check your GPU vendor's Vulkan driver support before filing an issue.
