package net.gameoverse.materialtraits;

import java.util.List;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Names the material behind a trait in the item's tooltip ("◆ Gold: +2 Mining Speed, +1 Luck"), in the
 * material's colour. The trait bonuses are already in the attribute list, but nothing said where they
 * came from, and the tooltip mod we ship folds some (mining speed) into the base number, hiding them.
 * Armour lines add the full-set total, since every trait there is per piece.
 */
public class MaterialTraitsClient implements ClientModInitializer {
   private static final Map<String, Integer> COLORS = Map.of(
      "leather", 0xC28452,
      "chainmail", 0xB4B4B4,
      "copper", 0xE38B5E,
      "wood", 0xB08850,
      "gold", 0xFFD83D,
      "diamond", 0x5DE8E0,
      "netherite", 0xA8939A
   );

   @Override
   public void onInitializeClient() {
      ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
         List<MaterialTraits.TraitEntry> traits = MaterialTraits.get(stack.getItem());
         int at = insertAt(lines);
         if (!traits.isEmpty()) {
            lines.add(at++, line(traits));
         }
         if (IMMERSIVE_ARMORS && (WoodSetBonus.WOODEN_TOOLS.contains(stack.getItem()) || isWoodenArmor(stack))) {
            lines.add(at, Component.translatable("material_traits.tooltip.wood_set",
                  ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(WoodSetBonus.MINING),
                  ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(WoodSetBonus.SPEED))
               .withStyle(style -> style.withColor(TextColor.fromRgb(COLORS.get("wood")))));
         }
      });
   }

   private static final boolean IMMERSIVE_ARMORS = FabricLoader.getInstance().isModLoaded("immersive_armors");

   /** Immersive Armors' Wooden Armor (looked up by id: the client never runs {@link WoodSetBonus}'s server tick). */
   private static boolean isWoodenArmor(ItemStack stack) {
      Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      return id.getNamespace().equals("immersive_armors") && id.getPath().startsWith("wooden_");
   }

   private static Component line(List<MaterialTraits.TraitEntry> traits) {
      String material = traits.getFirst().material();
      MutableComponent bonuses = Component.empty();
      for (int i = 0; i < traits.size(); i++) {
         if (i > 0) {
            bonuses.append(", ");
         }
         bonuses.append(bonus(traits.get(i)));
      }
      return Component.translatable("material_traits.tooltip", Component.translatable("material_traits.material." + material), bonuses)
         .withStyle(style -> style.withColor(TextColor.fromRgb(COLORS.getOrDefault(material, 0xFFFFFF))));
   }

   private static Component bonus(MaterialTraits.TraitEntry trait) {
      Attribute attribute = trait.attribute().value();
      double amount = trait.modifier().amount();
      boolean percent = trait.modifier().operation() != Operation.ADD_VALUE
         || trait.attribute() == MaterialAttributes.FIRE_RESISTANCE || trait.attribute() == MaterialAttributes.PHYSICAL_RESISTANCE;
      if (percent) {
         amount *= 100;
      } else if (trait.attribute() == Attributes.KNOCKBACK_RESISTANCE) {
         amount *= 10; // vanilla shows knockback resistance in tenths
      }
      Component name = Component.translatable(attribute.getDescriptionId());
      Component one = Component.translatable("attribute.modifier.plus." + (percent ? 1 : 0),
         ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(amount), name);
      if (trait.group() == EquipmentSlotGroup.MAINHAND) {
         return one;
      }
      return Component.translatable("material_traits.tooltip.full_set", one,
         ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(amount * 4) + (percent ? "%" : ""));
   }

   /** After the durability line, else after the last attribute line, else at the end. */
   private static int insertAt(List<Component> lines) {
      int attributes = -1;
      for (int i = 0; i < lines.size(); i++) {
         if (lines.get(i).getContents() instanceof TranslatableContents t) {
            if (t.getKey().equals("item.durability")) {
               return i + 1;
            }
            if (t.getKey().startsWith("attribute.modifier.")) {
               attributes = i;
            }
         }
      }
      return attributes >= 0 ? attributes + 1 : lines.size();
   }
}
