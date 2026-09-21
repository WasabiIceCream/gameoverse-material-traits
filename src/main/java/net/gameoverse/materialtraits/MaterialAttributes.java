package net.gameoverse.materialtraits;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * Custom elemental/physical resistance attributes for material traits, registered independently
 * of Spell Power's own SpellResistance system - none of the installed RPG Series mods (Archers,
 * Paladins & Priests, Rogues & Warriors, Wizards) or the reference mods researched
 * (extraspellattributes, More RPG Library) actually extend SpellResistance, so there's no real
 * precedent to build on there, and doing so would mean fighting an undocumented internal
 * registration order in a third-party mod instead. Registered the same way Spell Power and those
 * reference mods register their own custom attributes: a mixin on {@code Attributes.<clinit>}
 * (TAIL), since {@code BuiltInRegistries.ATTRIBUTE} is frozen shortly after that static
 * initializer runs, well before any mod's normal {@code onInitialize()} - a plain
 * {@code Registry.register()} call there would throw.
 *
 * <p>A registered attribute also has to be added to every relevant entity's default
 * {@code AttributeSupplier} (see {@link net.gameoverse.materialtraits.mixin.LivingEntityAttributesMixin})
 * before it's usable at all - otherwise {@code LivingEntity#getAttributes()} doesn't track it and
 * modifiers contributed via {@link net.gameoverse.materialtraits.mixin.ItemStackMaterialTraitsMixin}
 * silently do nothing. Damage reduction itself is applied by
 * {@link net.gameoverse.materialtraits.mixin.LivingEntityDamageMixin}, which walks {@link #ALL}
 * and reduces the incoming damage amount for every entry whose damage-type tag matches.
 */
public class MaterialAttributes {
   public record ResistanceEntry(Holder<Attribute> attribute, Predicate<DamageSource> matches) {
   }

   public static final List<ResistanceEntry> ALL = new ArrayList<>();

   public static Holder<Attribute> FIRE_RESISTANCE;
   public static Holder<Attribute> PHYSICAL_RESISTANCE;

   private static final TagKey<DamageType> MELEE = TagKey.create(
      Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("material_traits", "melee")
   );

   public static void register() {
      FIRE_RESISTANCE = registerResistance("fire_resistance", source -> source.is(DamageTypeTags.IS_FIRE));
      PHYSICAL_RESISTANCE = registerResistance(
         "physical_resistance", source -> source.is(MELEE) || source.is(DamageTypeTags.IS_PROJECTILE)
      );
   }

   private static Holder<Attribute> registerResistance(String path, Predicate<DamageSource> matches) {
      Holder<Attribute> holder = registerAttribute(path);
      ALL.add(new ResistanceEntry(holder, matches));
      return holder;
   }

   private static Holder<Attribute> registerAttribute(String path) {
      Identifier id = Identifier.fromNamespaceAndPath("material_traits", path);
      Attribute attribute = new RangedAttribute("attribute.material_traits." + path, 0.0, 0.0, 1.0).setSyncable(true);
      return Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, id, attribute);
   }
}
