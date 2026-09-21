package net.gameoverse.materialtraits.mixin;

import net.gameoverse.materialtraits.MaterialAttributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Applies every registered resistance attribute ({@link MaterialAttributes#ALL}) to incoming
 * damage - same target method Spell Power itself uses for SpellResistance
 * ({@code LivingEntity#actuallyHurt(ServerLevel, DamageSource, float)}, confirmed via decompile).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
   @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
   private float materialTraits$applyResistance(float amount, ServerLevel level, DamageSource source) {
      LivingEntity self = (LivingEntity) (Object) this;
      for (MaterialAttributes.ResistanceEntry entry : MaterialAttributes.ALL) {
         if (entry.matches().test(source) && self.getAttributes().hasAttribute(entry.attribute())) {
            double resistance = self.getAttributeValue(entry.attribute());
            amount = (float) (amount * (1.0 - resistance));
         }
      }

      return amount;
   }
}
