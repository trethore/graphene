# V1 Remaining Tasks

## 1. DevTools target discovery

- [ ] Discover remote DevTools page targets through Graphene-owned APIs.
- [ ] Expose target metadata and inspector URIs without leaking JCEF types.
- [ ] Handle disabled remote debugging, missing targets, and ambiguous session matches explicitly.

## 2. Context-menu policy or presenter

- [ ] Define per-browser context-menu configuration.
- [ ] Expose Graphene-owned request metadata and common browser actions.
- [ ] Determine whether CEF's native menu works with windowless Minecraft surfaces or a Fabric presenter is required.

## 3. Global configuration conflict handling

- [ ] Detect incompatible process-wide configuration contributions from different consumers.
- [ ] Define deterministic behavior for security-sensitive settings, especially file-system access.
- [ ] Report conflicts with enough ownership information to identify the contributing mods.
