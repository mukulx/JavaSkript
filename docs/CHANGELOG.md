# Changelog

All notable changes to JavaSkript are documented in GitHub Releases.

## [2.0.0] - 2026-08-31

### Features
- **Addon Architecture & Public API:** Added `JavaSkript` static entry point, `AddonRegistry`, custom `FieldInjector` support, and Bukkit lifecycle events (`ScriptLoadEvent`, `ScriptUnloadEvent`, `ScriptReloadEvent`, `ScriptPreCompileEvent`).
- **Inter-Script Shared State & Events:** Thread-safe global `Variables` cache with atomic math and persistent disk storage; custom `ScriptEventBus` for inter-script pub/sub events.
- **Built-in Async HTTP & Discord Webhooks:** Built-in `HttpHelper` with zero external dependencies, supporting async REST requests and Discord embed webhooks.
- **Clean Script Diagnostics:** Smart stack trace formatter pinpointing the exact script file and line number while filtering internal reflection noise, with a config toggle (`errors.clean-stack-traces`).
- **Display Holograms:** Added `HologramHelper` using modern 1.21 `TextDisplay`, `ItemDisplay`, and `BlockDisplay` entities.
- **Commands & Menus:** Fluent command builder with subcommands and typed arguments; component-based inventory GUI builder.
- **Performance Profiler & Benchmarks:** Real-time nanosecond CPU execution tracking (`/js profile`) and synthetic throughput benchmarks (`/js benchmark`).
- **PDC & ItemBuilder:** Fluent persistent data container helpers and Adventure-native item construction.

## [1.1.0]

- Initial release with in-memory ECJ compilation, dynamic Maven dependencies, SQLite database helper, and Folia scheduler support.
