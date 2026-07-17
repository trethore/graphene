# V1 Remaining Tasks

## 1. DevTools target discovery

- [x] Discover remote DevTools page targets through Graphene-owned APIs.
- [x] Expose target metadata and inspector URIs without leaking JCEF types.
- [x] Handle disabled remote debugging, missing targets, and ambiguous session matches explicitly.

## 2. Context-menu policy or presenter

- [x] Define per-browser context-menu configuration.
- [x] Expose Graphene-owned request metadata and common browser actions.
- [x] Determine whether CEF's native menu works with windowless Minecraft surfaces or a Fabric presenter is required.

Context menus are disabled by default and can be enabled with Graphene's standard action policy. Graphene always uses a
custom context-menu runner for off-screen browsers and never falls back to JCEF's native presenter. Fabric presents menus
as Minecraft screen overlays associated with the originating web-view widget. Consumers can replace the policy and
presenter per browser.

## 3. Global configuration conflict handling

- [x] Detect incompatible process-wide configuration contributions from different consumers.
- [x] Define deterministic behavior for security-sensitive settings, especially file-system access.
- [x] Report conflicts with enough ownership information to identify the contributing mods.

Global configuration is resolved transactionally during registration. Browser runtime paths and explicit remote
debugging configurations must agree, extension folders are combined, and browser file-system access is enabled only
when every registered consumer allows it. Conflicts reject the new registration and report every contributing mod and
its requested value in deterministic mod-ID order.
