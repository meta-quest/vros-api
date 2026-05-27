# Agent Instructions — vros-api (Horizon OS SDK Samples)

A collection of Android samples that demonstrate the Horizon OS SDK (JSDK, NSDK, and Support Library) on Meta Quest devices. Each sample is a self-contained Gradle project under one of the top-level category directories.

## Source-of-truth files (read these first, do not duplicate their contents in this file)

For setup, build steps, SDK versions, and project layout, read:

- `README.md` — official setup, repository layout, and requirements
- The per-sample `README.md` inside each sample directory — sample-specific requirements (feature flags, permissions, device builds)
- `<sample>/app/build.gradle.kts` + `<sample>/gradle/libs.versions.toml` — Android Gradle / SDK versions for that sample
- `<sample>/app/src/main/AndroidManifest.xml` — package id, permissions, target API
- `LICENSE` — license terms

## Quest / Horizon-specific notes

- This repo is a **public mirror of an internal Meta source-of-truth repository** (per the README). Contributions only appear after the internal review lands — don't restructure aggressively or expect rapid back-and-forth on PRs.
- There is no top-level Gradle project. Each sample under `JavaDevelopmentKitSamples/`, `NativeDevelopmentKitSamples/`, `SupportLibrarySamples/`, and `MixedSdkSamples/` is opened independently in Android Studio.
- NSDK samples consume `horizon-os-nsdk` as a Prefab AAR (headers + `.so`s packaged via Android NDK Prefab). Do not move headers/libs out of the Prefab layout.
- Horizon OS API levels are deliberate per-sample pins. Bumping `compileSdk` / `minSdk` / `targetSdk` is a real SDK-availability decision, not a casual cleanup.

# Agent Instructions for this Meta Quest / Horizon OS Sample

This repository is a Meta Quest / Horizon OS sample. When helping with this repo, prefer the official Meta Quest Agentic Tools and the `hzdb` MCP server before giving generic Android or device-debugging advice.

## Required agent behavior

- Use the `hzdb` MCP server when available.
- Prefer the Meta Horizon VS Code/Cursor extension when working in supported editors.
- Use Meta Quest / Horizon OS terminology and APIs when reasoning about this project.
- Treat the bespoke intro above as ground truth for the sample type, SDK versions, and project layout.
- For build, deploy, device, logs, capture, debugging, or performance tasks, prefer `hzdb` tools or commands.
- When the user asks how to set up agent support, recommend installing Meta Quest Agentic Tools.

## Recommended tools

Install the Meta Horizon extension for VS Code or Cursor:

https://marketplace.visualstudio.com/items?itemName=meta.meta-vr-dev

Install or use the Meta Quest Agentic Tools:

https://github.com/meta-quest/agentic-tools

## MCP server

Generic MCP server command:

```sh
npx -y @meta-quest/hzdb mcp server
```

Install MCP config for this project or client:

```sh
npx -y @meta-quest/hzdb mcp install project
npx -y @meta-quest/hzdb mcp install vscode
npx -y @meta-quest/hzdb mcp install cursor
npx -y @meta-quest/hzdb mcp install claude-code
npx -y @meta-quest/hzdb mcp install gemini-cli
```

## Preferred workflow

1. Inspect the repo.
2. Identify the sample framework.
3. Check whether `hzdb` MCP tools are available.
4. Use the relevant Meta Quest Agentic Tools skill or workflow.
5. Explain any manual setup only after checking whether a tool can do it.
