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
 * for the design notes and the recipe-only prototype this mod replaces.
 *
 * <p>Trait modifiers are attached dynamically via {@link net.gameoverse.materialtraits.mixin.ItemStackMaterialTraitsMixin},
 * which injects into both overloads of {@code ItemStack#forEachModifier} - the same proven,
 * verified-safe pattern used by apotheosis-fabric's own {@code StackAttributeModifiersEvent}
 * hook (confirmed via bytecode inspection that neither overload delegates to the other, so
 * both need instrumenting and doing so can't double-fire). Because this hooks the runtime
 * modifier-gathering path rather than an item's static default component, it applies
 * universally - crafted, looted, traded, or /given, unlike the prototype's recipe-only
 * overrides.
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

   @Override
   public void onInitialize() {
      registerGoldTraits();
   }

   /**
    * Gold: fastest and luckiest material to work with, lightest to wear. Paired with gold's
    * already-vanilla-true low durability and (for most tools) low attack damage as the
    * tradeoff - no separate nerf needed.
    */
   private static void registerGoldTraits() {
      add(Items.GOLDEN_HELMET, Attributes.MOVEMENT_SPEED, "gold_helmet_speed", 0.01, Operation.ADD_MULTIPLIED_BASE, EquipmentSlotGroup.HEAD);
      add(Items.GOLDEN_CHESTPLATE, Attributes.MOVEMENT_SPEED, "gold_chestplate_speed", 0.01, Operation.ADD_MULTIPLIED_BASE, EquipmentSlotGroup.CHEST);
      add(Items.GOLDEN_LEGGINGS, Attributes.MOVEMENT_SPEED, "gold_leggings_speed", 0.01, Operation.ADD_MULTIPLIED_BASE, EquipmentSlotGroup.LEGS);
      add(Items.GOLDEN_BOOTS, Attributes.MOVEMENT_SPEED, "gold_boots_speed", 0.01, Operation.ADD_MULTIPLIED_BASE, EquipmentSlotGroup.FEET);

      for (Item tool : new Item[]{
         Items.GOLDEN_SWORD, Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE
      }) {
         add(
            tool,
            Attributes.MINING_EFFICIENCY,
            "gold_tool_mining_efficiency_" + toolSuffix(tool),
            2.0,
            Operation.ADD_VALUE,
            EquipmentSlotGroup.MAINHAND
         );
         add(tool, Attributes.LUCK, "gold_tool_luck_" + toolSuffix(tool), 1.0, Operation.ADD_VALUE, EquipmentSlotGroup.MAINHAND);
      }
   }

   private static String toolSuffix(Item tool) {
      if (tool == Items.GOLDEN_SWORD) {
         return "sword";
      } else if (tool == Items.GOLDEN_PICKAXE) {
         return "pickaxe";
      } else if (tool == Items.GOLDEN_AXE) {
         return "axe";
      } else if (tool == Items.GOLDEN_SHOVEL) {
         return "shovel";
      } else {
         return "hoe";
      }
   }
}
