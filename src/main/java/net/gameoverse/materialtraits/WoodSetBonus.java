package net.gameoverse.materialtraits;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The wood set bonus: all four pieces of Immersive Armors' Wooden Armor and a wooden tool in the main hand give
 * {@link #MINING} more mining efficiency and {@link #SPEED} more attack speed. It depends on what the player wears and
 * holds together, so it's a transient modifier on the player, checked twice a second, not an item modifier. Nothing
 * happens without Immersive Armors.
 */
final class WoodSetBonus {
   static final double MINING = 1.0;
   static final double SPEED = 0.1;
   private static final AttributeModifier MINING_MODIFIER = new AttributeModifier(
      Identifier.fromNamespaceAndPath("material_traits", "wood_set_mining_efficiency"), MINING, Operation.ADD_VALUE);
   private static final AttributeModifier SPEED_MODIFIER = new AttributeModifier(
      Identifier.fromNamespaceAndPath("material_traits", "wood_set_attack_speed"), SPEED, Operation.ADD_VALUE);
   static final Set<Item> WOODEN_TOOLS = Set.of(Items.WOODEN_SWORD, Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL, Items.WOODEN_HOE);
   private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final String[] PIECES = {"wooden_helmet", "wooden_chestplate", "wooden_leggings", "wooden_boots"};
   /** Immersive Armors' Wooden Armor, helmet to boots; empty without the mod. */
   static final List<Item> ARMOR_SET = new ArrayList<>();

   private WoodSetBonus() {
   }

   static void register() {
      ServerTickEvents.END_SERVER_TICK.register(server -> {
         if (server.getTickCount() % 10 != 0) return;
         if (ARMOR_SET.isEmpty()) {
            for (String piece : PIECES) {
               BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("immersive_armors", piece)).ifPresent(ARMOR_SET::add);
            }
            if (ARMOR_SET.size() != 4) {
               ARMOR_SET.clear();
               return;
            }
         }
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean active = WOODEN_TOOLS.contains(player.getMainHandItem().getItem()) && wearsSet(player);
            apply(player, Attributes.MINING_EFFICIENCY, MINING_MODIFIER, active);
            apply(player, Attributes.ATTACK_SPEED, SPEED_MODIFIER, active);
         }
      });
   }

   private static boolean wearsSet(ServerPlayer player) {
      for (int i = 0; i < 4; i++) {
         if (player.getItemBySlot(ARMOR[i]).getItem() != ARMOR_SET.get(i)) return false;
      }
      return true;
   }

   private static void apply(ServerPlayer player, Holder<Attribute> attribute, AttributeModifier modifier, boolean active) {
      AttributeInstance instance = player.getAttribute(attribute);
      if (instance == null || instance.hasModifier(modifier.id()) == active) return;
      if (active) instance.addTransientModifier(modifier);
      else instance.removeModifier(modifier.id());
   }
}
