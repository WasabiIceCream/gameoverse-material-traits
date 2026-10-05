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

## The full table (2026-09-21, later revised)

One signature trait per material, not a pile of small bonuses, and never
touching vanilla's own armor/durability/damage numbers — those already
carry the linear-progression backbone; every trait here layers a real
vanilla or custom (see below) attribute on top. Checked against real
per-material text from both Better Than Adventure and Raspberry
Flavoured where a precedent exists; left deliberately bare rather than
inventing something where neither reference gives a material a
combat-relevant trait at all.

| Material | Armor trait | Tool trait |
|---|---|---|
| Leather | +2 `safe_fall_distance`/piece — "padded landing" | *(no tools)* |
| Chainmail | +5% physical resistance/piece (melee + projectile) | *(no tools)* |
| Copper | +0.15 `water_movement_efficiency`/piece | +1 `mining_efficiency` (mining tools only, not the sword) |
| Wood | *(no vanilla armor; see the set bonus below)* | Woodsman: +2 `mining_efficiency` on the axe, shovel and hoe (not the pickaxe). Light: +0.2 `attack_speed` (all 5). Blunt: +0.5 `attack_knockback` (sword, axe) |
| Stone | *(none — deliberate)* | *(none — deliberate)* |
| Iron | *(none — deliberate)* | *(none — deliberate)* |
| Gold | +1% `movement_speed`/piece (stacks to +4%) | +2 `mining_efficiency` (mining tools only), +1 `luck` (all tools) |
| Diamond | +0.15 `armor_toughness`/piece, +8% fire resistance/piece | +1 `mining_efficiency` (mining tools only) |
| Netherite | +0.025 `knockback_resistance`/piece (on top of its real 0.1 base) | +1 `attack_knockback` |

**Wood set bonus**: all four pieces of Immersive Armors' Wooden Armor plus a wooden tool in the main hand give +1
`mining_efficiency` and +0.1 `attack_speed` more (a transient player modifier, `WoodSetBonus`, checked twice a second;
nothing without Immersive Armors). Wood's traits are the user's own design (2026-10-05), not from either reference
mod: the wooden tier needed a job past the first pickaxe.

Stone and Iron are deliberately bare — two honest "no bonus, no drawback"
baselines at different power points, matching vanilla's own existing feel
for those tiers. Wood was bare too (its original attack_speed bonus had no
grounding in either reference and was removed), until 1.2.0 gave it a job
on purpose. Diamond's mining bonus is intentionally
smaller than Gold's, so Gold keeps its "fastest miner" identity rather
than getting crowded out by the higher tier. `mining_efficiency` only
ever applies to a material's 4 actual mining tools, never the sword
(found via in-game testing on Copper — the bonus was silently useless
there since nothing mines with a sword; fixed the same way for Copper,
Gold, and Diamond).

### Grounding vs. original design

Checked against both reference mods' actual per-material text
(bta.miraheze.org, raspberryflavoured.wiki.gg / moddedmc.wiki), not
guessed:

- **Matches one or both references**: Leather (fall damage, both), Diamond
  fire resistance (BTA: "significant damage reduction against fire and
  heat sources" — Raspberry Flavoured disagrees and puts fire protection
  on Gold instead, but BTA's wording is the more specific/deliberate one),
  Chainmail physical resistance (RF: "deals damage to attackers"; BTA:
  "excels at reducing melee and projectile damage" — picked BTA's
  straight-reduction framing since it reuses the same resistance-attribute
  mechanism as fire resistance instead of needing a separate thorns-style
  hook).
- **No clean reference equivalent, stays original design**: Copper (RF's
  copper mechanics are cosmetic tool oxidation + a lightning-triggered
  status effect on armor, neither attribute-shaped; BTA has no Copper tier
  at all), Gold's mining/luck tool trait (both references give Gold tools
  a *drop-quality* effect — RF: fortune/looting, BTA: silk-touch-like —
  which would mean hooking loot-table generation, out of this project's
  "flat attribute bonus" scope), Netherite's tool trait (RF gives
  Netherite tools "smelts drops + ignites targets," also loot/status-effect
  based, not attribute-shaped).

## Elemental/physical resistance (2026-09-21, later still)

A second, custom attribute system (`MaterialAttributes.java`) for traits
no real vanilla attribute covers — currently fire resistance (Diamond)
and physical/melee+projectile resistance (Chainmail).

Checked how the installed RPG Series mods (Archers, Paladins & Priests,
Rogues & Warriors, Wizards) and two other researched mods
([extraspellattributes](https://github.com/cleannrooster/extraspellattributes),
[More RPG Library](https://github.com/ProfessorFichte/More-RPG-Library))
handle this first. None of the four RPG Series mods touch Spell Power's
own `SpellResistance` system at all (they only use its offensive
spell-power-scaling side) — no precedent to build on there, and extending
it directly would mean fighting an undocumented internal registration
order in a third-party mod (`SpellResistance.Attributes.entry()` only
works if called before Spell Power's own tail-mixin on
`Attributes.<clinit>` consumes the list, which isn't a documented
timing guarantee). Both other researched mods aren't buildable for this
server's 26.x/Mojmap target anyway (extraspellattributes is old
Yarn-mapped 1.20-era code; More RPG Library has no branch past 1.21.1),
but both independently confirmed a cleaner, fully self-contained pattern:
register your own brand-new attribute via a mixin on `Attributes.<clinit>`
(`@Inject(method = "<clinit>", at = @At("TAIL"))`, `Registry.register()`
directly) — no dependency on Spell Power at all. Spell Power's own
`LivingEntityMixin` (decompiled for reference) confirmed the two other
pieces needed: a custom attribute has to be added to every entity's
default `AttributeSupplier` (via a mixin on
`LivingEntity.createLivingAttributes()`) to be tracked at all, and damage
reduction applies via a mixin on the exact same
`LivingEntity#actuallyHurt(ServerLevel, DamageSource, float)` Spell Power
itself hooks for `SpellResistance`.

`MaterialAttributes.ResistanceEntry` pairs a `Holder<Attribute>` with a
`Predicate<DamageSource>` (not a single tag) so a resistance can combine
multiple vanilla tag checks — needed for physical resistance, which means
"melee OR projectile," two separate vanilla concepts
(`data/material_traits/tags/damage_type/melee.json` tags the real vanilla
melee damage types; projectile reuses vanilla's own
`DamageTypeTags.IS_PROJECTILE`).

In-game validated both ways: attribute value reads exactly right (Diamond
full set → 0.32, Chainmail full set → 0.2), and real damage reduction
confirmed via RCON's `/damage` command with a specific damage type
(`minecraft:in_fire`, `minecraft:arrow`) for a clean before/after — both
land where expected once vanilla's own armor/toughness formula (which
runs *before* this mod's resistance layer, since it hooks the same
already-armor-reduced `amount` parameter) is accounted for.

## Next steps

- **More elements** (frost, etc.) — the resistance-attribute
  infrastructure is now general enough to add more via
  `MaterialAttributes.register()`; no material currently owns anything
  beyond fire/physical.
- **Mod-added materials** — deliberately out of scope until it's clear
  which ones are actually staying in the pack (a lot of that got sorted
  out during the SimplySwords/RPG-Series overlap work).
