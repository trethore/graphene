# API Code Documentation Plan

## Goal

Document the public Java API in code so that consumers can understand the purpose and important contracts of each
type without repeating information that is already clear from names, signatures, and straightforward implementation.

This is an API documentation pass, not an attempt to comment every declaration or explain every implementation
detail.

## Scope

Review all Java sources under:

- `packages/common/src/main/java/io/github/trethore/graphene/api/`
- `packages/fabric-1.21.11/src/main/java/io/github/trethore/graphene/fabric/api/`

The pass covers:

- every public top-level class, interface, enum, and record;
- every public nested API type;
- public and protected members whose contract is not clear from the declaration;
- unusual implementation logic inside public API classes when the reason for it is difficult to infer.

Private implementation types do not require API Javadoc. Overridden methods inherit documentation unless Graphene
adds behavior or constraints that the inherited contract does not describe.

## Required Type Documentation

Every public API type must have class-level Javadoc, including small records, listener interfaces, marker-like types,
and self-explanatory enums.

For simple types, use one short sentence:

```java
/** Identifies whether a physical key was pressed or released. */
public enum BrowserKeyAction {
```

Use additional paragraphs only when consumers need to understand lifecycle, ownership, threading, security, units,
or other behavior that cannot fit clearly into the summary.

Do not add documentation to every enum constant. Document an individual constant only when its meaning differs from
the name or has an important fallback or compatibility meaning.

## Member Documentation Rules

Add member Javadoc when it communicates at least one of the following:

- ownership or lifecycle behavior, including what `close()` releases;
- valid states and state-dependent behavior;
- callback thread, blocking restrictions, or asynchronous completion behavior;
- security decisions or failure behavior for policies and handlers;
- coordinate spaces, dimensions, units, encodings, or data formats;
- defaults, fallbacks, normalization, snapshots, or deduplication;
- cancellation behavior and whether repeated operations are idempotent;
- important exceptions that are part of normal API usage;
- a distinction between similarly named methods that is not obvious from their signatures.

Do not add Javadoc that only restates a declaration. Examples that normally remain undocumented include:

- ordinary getters such as `width()` or `state()`;
- forwarding methods such as `reload()` or `goBack()`;
- builder setters whose argument and effect are clear from their names;
- `equals`, `hashCode`, and straightforward record accessors;
- validation already made obvious by a method name and exception message.

Use `@param` and `@return` only when they add information beyond the parameter or method name. Use `@throws` for
meaningful public failure conditions rather than listing every possible null or validation exception.

## Writing Style

- Start with a direct summary of what the type or member represents or does.
- Prefer one sentence for simple types and one to three sentences for most documented members.
- Use present tense and active wording.
- Describe public Graphene behavior rather than JCEF implementation details.
- Use `{@link ...}` for related API types and `{@code ...}` for values, states, and short expressions.
- Keep the first sentence useful in generated Javadoc summaries.
- Avoid introductory filler such as "This class is used to" or "Provides methods for".
- Avoid examples unless the API is difficult to use correctly without one.
- Avoid promises about implementation details that are not part of the public contract.

Existing Javadocs are part of the review. Shorten broad or repetitive comments, correct unclear contracts, and keep
useful details rather than only adding new comments around them.

## Inline Implementation Comments

Inline comments are not required merely because code is inside an API package. Add one only when it explains why an
unusual implementation is necessary or protects against a non-obvious platform behavior.

Good candidates include:

- AltGr modifier correction;
- synthetic text generation and duplicate suppression;
- Linux clipboard workarounds;
- special handling for extra mouse buttons;
- browser frame or resolution behavior that depends on Minecraft scaling.

Do not narrate conditionals, validation, delegation, or simple data conversion. Prefer a short comment immediately
above the relevant logic instead of a long method-level explanation.

## Documentation Work

### Common entry points and context

Review `Graphene`, `GrapheneContext`, and `GrapheneSubscription`.

- Explain registration and context lookup at type level.
- Distinguish consumer configuration from effective process-global configuration.
- State that runtime lifecycle is controlled by Graphene and the platform.
- Preserve the idempotent subscription cancellation contract.

### Bridge

Review all types in `api/bridge`.

- Give each listener, handler, JSON helper, and exception a concise type summary.
- Document bridge readiness, reserved channels, request timeout behavior, and asynchronous failures where relevant.
- Document callback or handler constraints only where the implementation provides a stable guarantee.
- Replace the current broad `GrapheneBridge` introduction with a shorter contract-focused description.

### Browser core

Review the direct types in `api/browser`.

- Explain the role and lifecycle of `BrowserSession` and `BrowserSessions`.
- Document browser frame ownership, pixel format, sequence, dirty regions, and callback coalescing.
- Document non-obvious URL, title, zoom, download, and listener behavior.
- Give all event records, listener interfaces, cursors, severities, and state enums a short type summary.
- Do not document ordinary navigation forwarding methods individually.

### Browser options and policies

Review `BrowserOptions` and the bridge, navigation, download, dialog, and menu subpackages.

- Document default policies and whether they allow, deny, cancel, or delegate behavior.
- State threading and non-blocking requirements for synchronous policy callbacks.
- Explain how policy exceptions and `null` results are handled when this is a stable contract.
- Explain asynchronous presenter completion and how cancellation is represented.
- Document important option formats and limits, including frame rate, background color, and transparency behavior.
- Give nested request, result, decision, and context types short type documentation.

### Browser input

Review all types in `api/browser/input`.

- Distinguish physical key input from committed text input.
- Document coordinate spaces, click counts, scroll deltas, modifier sets, and raw platform metadata where needed.
- Give each input record and enum a concise type summary.
- Do not comment obvious key or modifier constants individually.

### Configuration

Review all types in `api/config`.

- Distinguish container-scoped and process-global settings.
- Document defaults, disabled states, path normalization, port selection, HTTP file roots, and SPA fallback behavior.
- Explain how global configuration conflicts are represented.
- Keep builder method documentation selective; group shared behavior in the enclosing type or builder documentation when
  that avoids repetition.

### Runtime and DevTools

Review all types in `api/runtime` and `api/devtools`.

- Document runtime state and availability expectations.
- Preserve the detailed initialization-stage contract.
- Explain HTTP server and remote-debugging availability.
- Document DevTools discovery results and expected exceptional completion without duplicating the same full exception
  list unnecessarily.

### URLs and assets

Review all types in `api/url`.

- Explain asset namespace and path requirements.
- Distinguish app, classpath, and HTTP URL factories where exposed through context.
- Document classpath URL construction and normalization.
- State that `normalizeResourcePath` returns an empty string for unsupported or invalid URLs.

### Fabric screens, surfaces, and widgets

Review all types in `fabric/api`.

- Document `BrowserSurface` ownership, closure, logical size, browser resolution, automatic resolution, rendering, and
  coordinate conversion.
- Document the coordinate and GLFW input expectations of `BrowserSurfaceInputAdapter` without repeating every
  parameter on every method.
- Add short inline reasoning comments around platform workarounds where the code alone does not explain them.
- Document `GrapheneWebViewWidget` surface ownership, screen integration requirement, resizing, input forwarding, and
  closure.
- Document what the `GrapheneScreens` web-view auto-close setting controls and its screen integration requirement.
- Leave straightforward widget forwarding and inherited rendering/input methods undocumented unless Graphene changes
  their contract.

## Review Order

Perform the work in coherent groups so terminology stays consistent:

1. Entry points, context, configuration, runtime, and URLs.
2. Browser core, frames, load events, and listeners.
3. Bridge, navigation, downloads, dialogs, and context menus.
4. Browser input.
5. Fabric surfaces, input adapters, screens, and widgets.
6. Final cross-package consistency and redundancy review.

## Completion Criteria

- Every public top-level and nested API type has class-level Javadoc.
- Member Javadocs describe only non-obvious public contracts.
- Lifecycle, ownership, threading, security, units, formats, defaults, and failure behavior are documented where
  relevant.
- Existing overly broad or redundant comments have been tightened.
- Inline comments explain reasoning rather than restating code.
- Terminology is consistent between common and Fabric APIs.
- Javadoc links resolve and the code remains free of formatting and compiler warnings.

After completing the documentation pass, run:

```shell
./gradlew spotlessApply
./gradlew check
```
