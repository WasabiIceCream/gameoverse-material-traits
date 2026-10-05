package net.gameoverse.materialtraits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Gives equipment materials distinct identities instead of pure linear power progression
 * (Better Than Adventure / Raspberry Flavoured inspired) - see mod-dev/material-traits-prototype
 * for the original design notes and the recipe-only prototype this mod replaces.
 *
 * <p>Trait modifiers are attached dynamically via {@link net.gameoverse.materialtraits.mixin.ItemStackMaterialTraitsMixin},
 * which injects into both overloads of {@code ItemStack#forEachModifier} - the same proven,
 * verified-safe pattern used by apotheosis-fabric's own {@code StackAttributeModifiersEvent}
 * hook (confirmed via bytecode inspection that neither overload delegates to the other, so
 * both need instrumenting and doing so can't double-fire). Because this hooks the runtime
 * modifier-gathering path rather than an item's static default component, it applies
 * universally - crafted, looted, traded, or /given.
 *
 * <p>Design philosophy per material: one clear signature trait, not a pile of small bonuses, and
 * never touching the raw vanilla armor/durability/damage numbers - those already provide the
 * linear-progression backbone, and every trait here is layered on top, either via a real vanilla
 * attribute or a custom one ({@link MaterialAttributes}). Traits are checked against both Better
 * Than Adventure and Raspberry Flavoured where a real precedent exists (Leather/Diamond/Chainmail
 * all match one or both); where neither reference gives a material a combat-relevant trait at
 * all (Iron, Stone), it's left deliberately bare rather than inventing one - they double as honest
 * "no bonus, no drawback" baselines at different power points, matching vanilla's own existing feel.
 * Wood was bare too until the user gave it a job of its own (see {@link #registerWoodTraits}). Copper, Gold's tool trait, and Netherite's tool
 * trait have no clean reference equivalent either way (RF's copper/gold mechanics are
 * status-effect/loot-table based, not attribute-shaped) and stay as original design.
 */
public class MaterialTraits implements ModInitializer {
   public record TraitEntry(String material, Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup group) {
   }

   private static final Map<Item, List<TraitEntry>> TRAITS = new HashMap<>();

   public static List<TraitEntry> get(Item item) {
      return TRAITS.getOrDefault(item, List.of());
   }

   private static void add(Item item, Holder<Attribute> attribute, String id, double amount, Operation operation, EquipmentSlotGroup group) {
      AttributeModifier modifier = new AttributeModifier(Identifier.fromNamespaceAndPath("material_traits", id), amount, operation);
      String material = id.substring(0, id.indexOf('_'));
      TRAITS.computeIfAbsent(item, key -> new ArrayList<>()).add(new TraitEntry(material, attribute, modifier, group));
   }

   private record ArmorSet(Item helmet, Item chestplate, Item leggings, Item boots) {
   }

   private static void addArmorTrait(ArmorSet set, String materialName, String traitName, Holder<Attribute> attribute, double perPiece, Operation operation) {
      add(set.helmet(), attribute, materialName + "_helmet_" + traitName, perPiece, operation, EquipmentSlotGroup.HEAD);
      add(set.chestplate(), attribute, materialName + "_chestplate_" + traitName, perPiece, operation, EquipmentSlotGroup.CHEST);
      add(set.leggings(), attribute, materialName + "_leggings_" + traitName, perPiece, operation, EquipmentSlotGroup.LEGS);
      add(set.boots(), attribute, materialName + "_boots_" + traitName, perPiece, operation, EquipmentSlotGroup.FEET);
   }

   private record ToolSet(Item sword, Item pickaxe, Item axe, Item shovel, Item hoe) {
      List<Item> all() {
         return List.of(this.sword(), this.pickaxe(), this.axe(), this.shovel(), this.hoe());
      }

      /** The 4 tools that actually break blocks - excludes the sword, which mining_efficiency does nothing for. */
      List<Item> mining() {
         return List.of(this.pickaxe(), this.axe(), this.shovel(), this.hoe());
      }
   }

   private static void addToolTrait(ToolSet set, String materialName, String traitName, Holder<Attribute> attribute, double amount, Operation operation) {
      for (Item tool : set.all()) {
         add(tool, attribute, materialName + "_" + toolSuffix(tool) + "_" + traitName, amount, operation, EquipmentSlotGroup.MAINHAND);
      }
   }

   /** Same as {@link #addToolTrait}, but skips the sword - for traits like mining_efficiency that a sword can't use. */
   private static void addMiningTrait(ToolSet set, String materialName, String traitName, Holder<Attribute> attribute, double amount, Operation operation) {
      for (Item tool : set.mining()) {
         add(tool, attribute, materialName + "_" + toolSuffix(tool) + "_" + traitName, amount, operation, EquipmentSlotGroup.MAINHAND);
      }
   }

   private static String toolSuffix(Item tool) {
      if (tool == Items.WOODEN_SWORD || tool == Items.STONE_SWORD || tool == Items.COPPER_SWORD || tool == Items.IRON_SWORD
         || tool == Items.GOLDEN_SWORD
         || tool == Items.DIAMOND_SWORD
         || tool == Items.NETHERITE_SWORD) {
         return "sword";
      } else if (tool == Items.WOODEN_PICKAXE
         || tool == Items.STONE_PICKAXE
         || tool == Items.COPPER_PICKAXE
         || tool == Items.IRON_PICKAXE
         || tool == Items.GOLDEN_PICKAXE
         || tool == Items.DIAMOND_PICKAXE
         || tool == Items.NETHERITE_PICKAXE) {
         return "pickaxe";
      } else if (tool == Items.WOODEN_AXE
         || tool == Items.STONE_AXE
         || tool == Items.COPPER_AXE
         || tool == Items.IRON_AXE
         || tool == Items.GOLDEN_AXE
         || tool == Items.DIAMOND_AXE
         || tool == Items.NETHERITE_AXE) {
         return "axe";
      } else if (tool == Items.WOODEN_SHOVEL
         || tool == Items.STONE_SHOVEL
         || tool == Items.COPPER_SHOVEL
         || tool == Items.IRON_SHOVEL
         || tool == Items.GOLDEN_SHOVEL
         || tool == Items.DIAMOND_SHOVEL
         || tool == Items.NETHERITE_SHOVEL) {
         return "shovel";
      } else {
         return "hoe";
      }
   }

   @Override
   public void onInitialize() {
      registerLeatherTraits();
      registerChainmailTraits();
      registerCopperTraits();
      registerWoodTraits();
      WoodSetBonus.register();
      // Stone and Iron: deliberately no traits - see class javadoc.
      registerGoldTraits();
      registerDiamondTraits();
      registerNetheriteTraits();
   }

   /** Leather: best fall protection of any armor - "padded landing." */
   private static void registerLeatherTraits() {
      ArmorSet set = new ArmorSet(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);
      addArmorTrait(set, "leather", "safe_fall", Attributes.SAFE_FALL_DISTANCE, 2.0, Operation.ADD_VALUE);
   }

   /**
    * Chainmail: reduces melee and projectile damage - matches both reference mods (Raspberry
    * Flavoured: "deals damage to attackers"; Better Than Adventure: "excels at reducing melee and
    * projectile damage"). Picked the BTA framing (straight damage reduction) over RF's
    * reflect-to-attacker mechanic since it reuses the same resistance-attribute infrastructure
    * built for Diamond's fire resistance, rather than needing a whole separate thorns-style hook.
    */
   private static void registerChainmailTraits() {
      ArmorSet set = new ArmorSet(Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);
      addArmorTrait(set, "chainmail", "physical_resistance", MaterialAttributes.PHYSICAL_RESISTANCE, 0.05, Operation.ADD_VALUE);
   }

   /**
    * Wood: the user's own design (2026-10-05), not from either reference mod, which give wood nothing - the deliberate
    * "no trait" stance was reversed so the wooden tier has a job past the first pickaxe. "Woodsman": the axe, shovel
    * and hoe outpace stone and iron on their own blocks (mining_efficiency 2 -> speed 7 on wood's base 2; stone 4,
    * iron 6, diamond 8); the pickaxe stays the plain starter. "Light": every wooden tool swings faster. "Blunt": the
    * sword and axe knock back further. Wearing all four pieces of Immersive Armors' Wooden Armor strengthens it
    * further ({@link WoodSetBonus}).
    */
   private static void registerWoodTraits() {
      ToolSet tools = new ToolSet(Items.WOODEN_SWORD, Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL, Items.WOODEN_HOE);
      for (Item tool : List.of(Items.WOODEN_AXE, Items.WOODEN_SHOVEL, Items.WOODEN_HOE)) {
         add(tool, Attributes.MINING_EFFICIENCY, "wood_" + toolSuffix(tool) + "_mining_efficiency", 2.0, Operation.ADD_VALUE, EquipmentSlotGroup.MAINHAND);
      }
      addToolTrait(tools, "wood", "attack_speed", Attributes.ATTACK_SPEED, 0.2, Operation.ADD_VALUE);
      for (Item tool : List.of(Items.WOODEN_SWORD, Items.WOODEN_AXE)) {
         add(tool, Attributes.ATTACK_KNOCKBACK, "wood_" + toolSuffix(tool) + "_attack_knockback", 0.5, Operation.ADD_VALUE, EquipmentSlotGroup.MAINHAND);
      }
   }

   /**
    * Copper: the only material with a real aquatic identity (armor) and a real mining-precision
    * edge (tools) - "verdigris conducts."
    */
   private static void registerCopperTraits() {
      ArmorSet armor = new ArmorSet(Items.COPPER_HELMET, Items.COPPER_CHESTPLATE, Items.COPPER_LEGGINGS, Items.COPPER_BOOTS);
      addArmorTrait(armor, "copper", "water_efficiency", Attributes.WATER_MOVEMENT_EFFICIENCY, 0.15, Operation.ADD_VALUE);

      ToolSet tools = new ToolSet(Items.COPPER_SWORD, Items.COPPER_PICKAXE, Items.COPPER_AXE, Items.COPPER_SHOVEL, Items.COPPER_HOE);
      addMiningTrait(tools, "copper", "mining_efficiency", Attributes.MINING_EFFICIENCY, 1.0, Operation.ADD_VALUE);
   }

   /**
    * Gold: fastest and luckiest material to work with, lightest to wear. Paired with gold's
    * already-vanilla-true low durability and (for most tools) low attack damage as the
    * tradeoff - no separate nerf needed.
    */
   private static void registerGoldTraits() {
      ArmorSet armor = new ArmorSet(Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);
      addArmorTrait(armor, "gold", "speed", Attributes.MOVEMENT_SPEED, 0.01, Operation.ADD_MULTIPLIED_BASE);

      ToolSet tools = new ToolSet(Items.GOLDEN_SWORD, Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE);
      addMiningTrait(tools, "gold", "mining_efficiency", Attributes.MINING_EFFICIENCY, 2.0, Operation.ADD_VALUE);
      addToolTrait(tools, "gold", "luck", Attributes.LUCK, 1.0, Operation.ADD_VALUE);
   }

   /**
    * Diamond: hardest edge and toughest defense of the non-endgame tiers - a modest bump to
    * each, deliberately smaller than Gold's mining/luck bonus and Netherite's real defaults, so
    * neither of those materials' own identity gets crowded out. Also gets +8% fire resistance
    * per armor piece (+32% full set) - matches Better Than Adventure's own wording almost
    * exactly ("Diamond armor provides significant damage reduction against fire and heat
    * sources"). Raspberry Flavoured instead says Diamond has no special properties at all and
    * gives Gold slight fire protection - the two references disagree, and BTA's is the more
    * specific/deliberate one, so that's what this follows.
    */
   private static void registerDiamondTraits() {
      ArmorSet armor = new ArmorSet(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
      addArmorTrait(armor, "diamond", "toughness", Attributes.ARMOR_TOUGHNESS, 0.15, Operation.ADD_VALUE);
      addArmorTrait(armor, "diamond", "fire_resistance", MaterialAttributes.FIRE_RESISTANCE, 0.08, Operation.ADD_VALUE);

      ToolSet tools = new ToolSet(Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE);
      addMiningTrait(tools, "diamond", "mining_efficiency", Attributes.MINING_EFFICIENCY, 1.0, Operation.ADD_VALUE);
   }

   /**
    * Netherite: already fire-immune and doesn't float in lava (real vanilla defaults, untouched
    * here) - adds a knockback identity on both ends, "immovable heavy tank."
    */
   private static void registerNetheriteTraits() {
      ArmorSet armor = new ArmorSet(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);
      addArmorTrait(armor, "netherite", "knockback_resist", Attributes.KNOCKBACK_RESISTANCE, 0.025, Operation.ADD_VALUE);

      ToolSet tools = new ToolSet(
         Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE
      );
      addToolTrait(tools, "netherite", "attack_knockback", Attributes.ATTACK_KNOCKBACK, 1.0, Operation.ADD_VALUE);
   }
}
