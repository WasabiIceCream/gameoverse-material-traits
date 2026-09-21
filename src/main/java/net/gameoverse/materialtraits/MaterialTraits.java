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
 * <p>Design philosophy per material (matching what BTA/Raspberry Flavoured research found):
 * one clear signature trait, not a pile of small bonuses, and never touching the raw vanilla
 * armor/durability/damage numbers - those already provide the linear-progression backbone, and
 * every trait here is layered on top via real vanilla attributes only (no custom attributes
 * yet - elemental/magical traits are a planned follow-up leaning on Spell Power's already-
 * installed resistance-attribute system instead of building one from scratch). Iron and Stone
 * are deliberately untouched: two honest "no bonus, no drawback" baselines at different power
 * points, matching vanilla's own existing feel for those tiers.
 */
public class MaterialTraits implements ModInitializer {
   public record TraitEntry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup group) {
   }

   private static final Map<Item, List<TraitEntry>> TRAITS = new HashMap<>();

   public static List<TraitEntry> get(Item item) {
      return TRAITS.getOrDefault(item, List.of());
   }

   private static void add(Item item, Holder<Attribute> attribute, String id, double amount, Operation operation, EquipmentSlotGroup group) {
      AttributeModifier modifier = new AttributeModifier(Identifier.fromNamespaceAndPath("material_traits", id), amount, operation);
      TRAITS.computeIfAbsent(item, key -> new ArrayList<>()).add(new TraitEntry(attribute, modifier, group));
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
   }

   private static void addToolTrait(ToolSet set, String materialName, String traitName, Holder<Attribute> attribute, double amount, Operation operation) {
      for (Item tool : set.all()) {
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

   /** Chainmail: best knockback resistance of the early armors - "heavy interlocking rings." */
   private static void registerChainmailTraits() {
      ArmorSet set = new ArmorSet(Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);
      addArmorTrait(set, "chainmail", "knockback_resist", Attributes.KNOCKBACK_RESISTANCE, 0.02, Operation.ADD_VALUE);
   }

   /**
    * Copper: the only material with a real aquatic identity (armor) and a real mining-precision
    * edge (tools) - "verdigris conducts."
    */
   private static void registerCopperTraits() {
      ArmorSet armor = new ArmorSet(Items.COPPER_HELMET, Items.COPPER_CHESTPLATE, Items.COPPER_LEGGINGS, Items.COPPER_BOOTS);
      addArmorTrait(armor, "copper", "water_efficiency", Attributes.WATER_MOVEMENT_EFFICIENCY, 0.15, Operation.ADD_VALUE);

      ToolSet tools = new ToolSet(Items.COPPER_SWORD, Items.COPPER_PICKAXE, Items.COPPER_AXE, Items.COPPER_SHOVEL, Items.COPPER_HOE);
      addToolTrait(tools, "copper", "mining_efficiency", Attributes.MINING_EFFICIENCY, 1.0, Operation.ADD_VALUE);
   }

   /** Wood: no armor in vanilla. Tools: quickest swing of any tier - "light and quick." */
   private static void registerWoodTraits() {
      ToolSet tools = new ToolSet(Items.WOODEN_SWORD, Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL, Items.WOODEN_HOE);
      addToolTrait(tools, "wood", "attack_speed", Attributes.ATTACK_SPEED, 0.5, Operation.ADD_VALUE);
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
      addToolTrait(tools, "gold", "mining_efficiency", Attributes.MINING_EFFICIENCY, 2.0, Operation.ADD_VALUE);
      addToolTrait(tools, "gold", "luck", Attributes.LUCK, 1.0, Operation.ADD_VALUE);
   }

   /**
    * Diamond: hardest edge and toughest defense of the non-endgame tiers - a modest bump to
    * each, deliberately smaller than Gold's mining/luck bonus and Netherite's real defaults, so
    * neither of those materials' own identity gets crowded out.
    */
   private static void registerDiamondTraits() {
      ArmorSet armor = new ArmorSet(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
      addArmorTrait(armor, "diamond", "toughness", Attributes.ARMOR_TOUGHNESS, 0.15, Operation.ADD_VALUE);

      ToolSet tools = new ToolSet(Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE);
      addToolTrait(tools, "diamond", "mining_efficiency", Attributes.MINING_EFFICIENCY, 1.0, Operation.ADD_VALUE);
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
