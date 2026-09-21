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
populated at init via small `register<Material>Traits()` methods (using
`ArmorSet`/`ToolSet` + `addArmorTrait`/`addToolTrait` helpers so each
material's registration is a few lines, not the original hand-written
Gold pattern repeated 6 more times).

## The full table (2026-09-21)

One signature trait per material, not a pile of small bonuses, and never
touching vanilla's own armor/durability/damage numbers — those already
carry the linear-progression backbone; every trait here layers a real
vanilla attribute on top.

| Material | Armor trait | Tool trait |
|---|---|---|
| Leather | +2 `safe_fall_distance`/piece — "padded landing" | *(no tools)* |
| Chainmail | +0.02 `knockback_resistance`/piece | *(no tools)* |
| Copper | +0.15 `water_movement_efficiency`/piece | +1 `mining_efficiency` |
| Wood | *(no armor)* | +0.5 `attack_speed` — "light and quick" |
| Stone | *(none — deliberate)* | *(none — deliberate)* |
| Iron | *(none — deliberate)* | *(none — deliberate)* |
| Gold | +1% `movement_speed`/piece (stacks to +4%) | +2 `mining_efficiency`, +1 `luck` |
| Diamond | +0.15 `armor_toughness`/piece | +1 `mining_efficiency` |
| Netherite | +0.025 `knockback_resistance`/piece (on top of its real 0.1 base) | +1 `attack_knockback` |

Stone and Iron are deliberately bare — two honest "no bonus, no drawback"
baselines at different power points, matching vanilla's own existing feel
for those tiers. Diamond's mining bonus is intentionally smaller than
Gold's, so Gold keeps its "fastest miner" identity rather than getting
crowded out by the higher tier.

## Next steps

- **Elemental/magical traits** (e.g. Diamond resisting fire) — deferred,
  since vanilla has no attribute for damage-type resistance. Spell Power
  (already installed on this server, same author as the RPG Series mods)
  is the planned foundation: its `spell_power:resistance.*` attributes
  already hook real vanilla damage types via `SpellResistance.resist()`,
  not just its own spell-school damage, so this shouldn't need a custom
  attribute built from scratch.
- **Mod-added materials** — deliberately out of scope until it's clear
  which ones are actually staying in the pack (a lot of that got sorted
  out during the SimplySwords/RPG-Series overlap work).
