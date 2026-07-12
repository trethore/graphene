# TODO

- [ ] Documentation
- [ ] Deployment
- [ ] Keybindings
- [ ] Rework the JavaScript bridges
- [ ] Add a public API for custom dialog and platform presenters
- [ ] Resolve the JCEF integration blockers and remove temporary Graphene workarounds
  - [ ] Fix `trethore/jcef#9` so Linux injected key events honor the supplied layout-aware character
  - [ ] Fix `trethore/jcef#10` so standard custom schemes work on Linux without NetworkContext Mojo failures
  - [ ] Update Graphene to use the corrected JCEF keyboard behavior and remove any CEF-specific input workarounds
  - [ ] Register `app://` as a standard secure scheme on every platform
  - [ ] Use the browser Clipboard API directly and remove the debug clipboard bridge fallback
