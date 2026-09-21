# gameoverse-material-traits

Gives equipment materials distinct identities instead of pure linear power
progression (Better Than Adventure / Raspberry Flavoured inspired). Wholly
original work, MIT licensed.

## Status

**Validated in-game, 2026-09-21 (Iron vs Gold).** Supersedes
`mod-dev/material-traits-prototype`, which proved the design but only
worked for freshly-crafted gear (recipe-result component overrides). This
mod attaches trait modifiers dynamically at the `ItemStack` level instead —
confirmed working via plain `/give` with zero manual components, and
further confirmed (accidentally but conclusively) when leftover items from
the prototype's manual-component testing showed *double* the bonus once
this mod went live: the old item's baked-in static modifier plus this
mod's own dynamically-added one, both applying independently and
correctly. That's not a bug — it can't happen through normal play (nobody
crafts/loots/trades an item with a hand-written component override) — but
it's about as strong a proof-by-construction as this mechanism could get.

## How it works

Two `@Mixin(ItemStack.class)` injections at `@At("TAIL")` into both
overloads of `forEachModifier` — the same proven pattern already used by
`apotheosis-fabric`'s own `StackAttributeModifiersEvent` hook (independently
reimplemented here rather than depending on Apotheosis, to keep this
feature decoupled from the loot/affix mod). Hooking the runtime
modifier-gathering path rather than an item's static default component
means trait bonuses apply universally, regardless of how the item was
obtained.

`MaterialTraits.java` holds a simple `Item -> List<TraitEntry>` registry,
populated at init. Currently just Gold (armor: +1% movement speed per
piece, stacking to +4% for a full set; tools: +2 mining efficiency, +1
luck while held) as the validated pilot material. Iron is deliberately
untouched — vanilla's existing "balanced generalist, no bonus, no
drawback" identity for iron already fits the intended design.

## Next steps

Extend `registerGoldTraits()`-style methods to the rest of the material
table (Wood, Stone, Copper, Diamond, Netherite, and eventually mod-added
materials) once the full trait design is finalized — see
`docs/current-state.md` in the project root for the design sketch and
research (Better Than Adventure's per-material damage-type specialization,
Raspberry Flavoured's `ItemAttributeModifierEvent`-based implementation,
Spell Power's resistance-attribute system for elemental/magical traits
later on).
