# Immersive Engineering: Fix Pack

Unofficial fixes for long-standing [Immersive Engineering](https://github.com/BluSunrize/ImmersiveEngineering) bugs, each with its own switch, for NeoForge 1.21.1. Not made by or affiliated with BluSunrize; this is an addon, as Immersive Engineering's licence allows.

| Switch | Issue | What it fixes |
|---|---|---|
| `workbenchNoQuickMoveDupe` | [#6393](https://github.com/BluSunrize/ImmersiveEngineering/issues/6393) | Shift-clicking a stack into the Engineer's Workbench dropped a copy of what didn't fit on the ground (item dupe). |

Config: `config/ie_fix_pack.properties`. Fixes without a passing test start switched off.

## Tests

Every fix has a GameTest, and a control that runs the same setup with the fix off and has to reproduce the bug.

MIT.
