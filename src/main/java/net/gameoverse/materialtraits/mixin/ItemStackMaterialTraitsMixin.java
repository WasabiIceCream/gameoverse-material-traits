package net.gameoverse.materialtraits.mixin;

import java.util.function.BiConsumer;
import net.gameoverse.materialtraits.MaterialTraits;
import net.gameoverse.materialtraits.MaterialTraits.TraitEntry;
import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Injects material-trait modifiers into vanilla's attribute-modifier gathering. Both overloads
 * of {@code forEachModifier} need instrumenting - the per-slot one used for live combat stat
 * computation, and the per-{@link EquipmentSlotGroup} one used for tooltip rendering - confirmed
 * (matching apotheosis-fabric's own equivalent mixin, same target class) that neither delegates
 * to the other, so no double-fire risk from instrumenting both.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMaterialTraitsMixin {
   @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("TAIL"))
   private void materialTraits$addForSlot(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> sink, CallbackInfo ci) {
      ItemStack self = (ItemStack) (Object) this;
      for (TraitEntry entry : MaterialTraits.get(self.getItem())) {
         if (entry.group().test(slot)) {
            sink.accept(entry.attribute(), entry.modifier());
         }
      }
   }

   @Inject(
      method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V",
      at = @At("TAIL")
   )
   private void materialTraits$addForGroup(
      EquipmentSlotGroup group, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> sink, CallbackInfo ci
   ) {
      ItemStack self = (ItemStack) (Object) this;
      for (TraitEntry entry : MaterialTraits.get(self.getItem())) {
         if (entry.group() == group) {
            sink.accept(entry.attribute(), entry.modifier(), ItemAttributeModifiers.Display.attributeModifiers());
         }
      }
   }
}
