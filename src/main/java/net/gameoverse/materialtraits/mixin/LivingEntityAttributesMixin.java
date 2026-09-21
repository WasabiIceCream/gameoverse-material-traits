package net.gameoverse.materialtraits.mixin;

import net.gameoverse.materialtraits.MaterialAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds our custom attributes to every entity's default attribute supplier - required for an
 * attribute to be tracked/queryable at all via {@code LivingEntity#getAttributes()}, confirmed by
 * decompiling Spell Power's own equivalent mixin on this same method.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAttributesMixin {
   @Inject(method = "createLivingAttributes", at = @At("RETURN"))
   private static void materialTraits$addAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
      for (MaterialAttributes.ResistanceEntry entry : MaterialAttributes.ALL) {
         cir.getReturnValue().add(entry.attribute());
      }
   }
}
