# Changelog

All notable changes to JavaSkript are documented in GitHub Releases.

## [Unreleased]

### Features
- **Resource Pack API:** Scripts register resource packs from folders, zips, URLs or code (`ResourcePackHelper`, `ResourcePacks`). JavaSkript hosts them on a built-in HTTP server (`modules.resource-packs`) and sends every pack to joining players. Includes 2D and 3D custom item models (1.21.4+ `item_model`), textures, sounds, priorities, required packs and load callbacks. Unchanged packs are not downloaded again after a reload.
- **Custom Economies:** `Economy.setProvider` lets a script provide the main economy for `Economy`, team banks and Vault. Script-registered providers and currencies are released when the script unloads.
- **Teams:** `rename` and `transferLeadership` on `TeamHelper`, `save` refreshes tab names, team metadata is persisted, and team banks can use a registered currency (`modules.teams.bank.currency`).

### Bug Fixes
- Economy no longer overwrites a balance with the starting balance when the database is briefly unavailable, and failed saves are reported instead of succeeding.
- Team messages now use the `teams.*` keys from `messages.yml`; none of them could be changed before.
- Queued team saves are written on shutdown instead of being dropped after a 3 second stall.
- A corrupt `variables.json` is kept as a backup instead of being overwritten.
- A reload whose `onEnable` fails restores the previous version of the script.
- Holograms and mannequins teleport and update safely on Folia.
- HTTP failures fail the returned future and are logged; callbacks no longer run after their script unloads.
- Open menus of an unloading script are closed so their handlers never run old code.
- Command names only lose a trailing `Command`/`Cmd`, and command errors no longer show exception details to players.

## [2.0.1] - 2026-09-03

### Bug Fixes & Improvements
- **Asynchronous Lifecycle Events:** Made `ScriptEvent` async state dynamic to prevent Bukkit `IllegalStateException` when triggered from background file watcher threads.
- **Config Overload Ambiguity:** Added `setFileHeader` and `addFileDefault` in `ScriptConfig` to eliminate compiler ambiguity with vararg overloads.
- **Command Builder & Context:** Added fluent `.playerOnly()` and `.consoleOnly()` methods to `CommandBuilder` / `SubcommandBuilder`, and added `.args()` and `.arg(int index)` helpers to `CommandContext`.
- **Compiler Classpath Enhancements:** Added Google Gson and Guava directly to `ScriptCompiler` base classpath and added `Players.online()` alias.

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
