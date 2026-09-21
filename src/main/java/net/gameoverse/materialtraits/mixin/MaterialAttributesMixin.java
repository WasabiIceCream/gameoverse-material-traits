package net.gameoverse.materialtraits.mixin;

import net.gameoverse.materialtraits.MaterialAttributes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers our own custom attributes at the tail of vanilla's own attribute static
 * initializer - the same trick Spell Power uses for its own custom attributes, confirmed via
 * decompile ({@code @Mixin(Attributes.class) @Inject(method = "<clinit>", at = @At("TAIL"))}).
 * The registry is frozen shortly after this runs, before any mod's normal onInitialize().
 */
@Mixin(Attributes.class)
public class MaterialAttributesMixin {
   @Inject(method = "<clinit>", at = @At("TAIL"))
   private static void materialTraits$registerAttributes(CallbackInfo ci) {
      MaterialAttributes.register();
   }
}
