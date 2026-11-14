# Copilot instructions for kbeigl/bm

## Project
Multi-module Maven repository (Java 21) for GPS tracking with Apache Camel and Spring Boot.
Root modules: `bm-parent` (dependency/plugin management, Camel & Spring Boot versions),
`bm-tracker` (OsmAnd tracker/player apps), `bm-traccar` (Traccar API clients and services), `bm-docs`.

## Build and test
- Always use the Maven wrapper from the repo root: `./mvnw`.
- CI build (mirror it locally):
  `./mvnw -B -ntp install -Dintegration.tests.skip=true -Ddocker.skip.tests=true -Dopenapi.skip=false -Dasciidoc.process.skip=true`
- Integration and Docker tests need a Traccar server; skip them unless asked.
- Build a single module with `-pl <module> -am`.

## Conventions
- Manage dependency and plugin versions in `bm-parent/pom.xml`, not in child modules.
- Target Java 21 (`jdk.version` in the root `pom.xml`).
- Generated code (e.g. the OpenAPI client in `bm-traccar/traccar-openapitools-client`) must not be edited by hand.
- Keep changes minimal and match the style of surrounding code; add or update tests for behavior changes.

## Documentation
- Docs are AsciiDoc (`.adoc`) next to the code, in each module's `asciidoc/` folder, and published via GitHub Pages.
- Edit `.adoc` sources only; `.html` files are generated (`./mvnw clean generate-resources`) and checked in at release.
- Update the relevant `.adoc` and `readme.adoc` when changing behavior or architecture; keep cross-links valid.
- Use `-Dasciidoc.process.skip=true` when you are not working on documentation.
