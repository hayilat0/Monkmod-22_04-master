package net.gabriel.monk.item;

import net.gabriel.monk.Monkmod;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.SpellDataComponents;
import net.spell_engine.api.spell.container.SpellChoice;
import net.spell_engine.api.spell.container.SpellContainers;

public final class MonkWeapons {

    private static final float GAUNTLET_ATTACK_DAMAGE_BONUS = 7.0f;
    private static final float GAUNTLET_ATTACK_SPEED_BONUS = -2.2f;
    private static final float GAUNTLET_ATTACK_RANGE_BONUS = -0.75f;
    private static final float GAUNTLET_ARCANE_POWER_BONUS = 5.5f;
    private static final float GAUNTLET_ARMOR_BONUS = 3.0f;

    private static final String GAUNTLET_SPELL_CHOICE_POOL = "monkmod:weapon/spiritual_gauntlet";

    public static Item SPIRITUAL_GAUNTLET;

    public static void register() {
        SPIRITUAL_GAUNTLET = registerGauntlet(
                "spiritual_gauntlet",
                ToolMaterials.NETHERITE
        );

        Monkmod.LOGGER.info("[Monkmod] Weapons registradas: 1");
    }

    private static Item registerGauntlet(String name, ToolMaterial material) {
        Identifier id = Identifier.of(Monkmod.MOD_ID, name);

        Item.Settings settings = new Item.Settings()
                .maxDamage(material.getDurability())
                .attributeModifiers(createGauntletAttributes(name))
                .component(
                        SpellDataComponents.SPELL_CONTAINER,
                        SpellContainers.forMagicWeapon()
                                .withBindingPool(Identifier.of(GAUNTLET_SPELL_CHOICE_POOL))
                                .withMaxSpellCount(1)
                )
                .component(
                        SpellDataComponents.SPELL_CHOICE,
                        SpellChoice.of(GAUNTLET_SPELL_CHOICE_POOL)
                );

        return Registry.register(
                Registries.ITEM,
                id,
                new SpiritualGauntletItem(material, settings)
        );
    }

    private static AttributeModifiersComponent createGauntletAttributes(String itemName) {
        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();

        builder.add(
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                modifier(itemName, "attack_damage", GAUNTLET_ATTACK_DAMAGE_BONUS),
                AttributeModifierSlot.MAINHAND
        );

        builder.add(
                EntityAttributes.GENERIC_ATTACK_SPEED,
                modifier(itemName, "attack_speed", GAUNTLET_ATTACK_SPEED_BONUS),
                AttributeModifierSlot.MAINHAND
        );

        builder.add(
                EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE,
                modifier(itemName, "entity_interaction_range", GAUNTLET_ATTACK_RANGE_BONUS),
                AttributeModifierSlot.MAINHAND
        );

        builder.add(
                arcaneAttribute(),
                modifier(itemName, "arcane_power", GAUNTLET_ARCANE_POWER_BONUS),
                AttributeModifierSlot.MAINHAND
        );

        builder.add(
                EntityAttributes.GENERIC_ARMOR,
                modifier(itemName, "armor", GAUNTLET_ARMOR_BONUS),
                AttributeModifierSlot.MAINHAND
        );

        return builder.build();
    }

    private static RegistryEntry<EntityAttribute> arcaneAttribute() {
        RegistryKey<EntityAttribute> key = RegistryKey.of(
                RegistryKeys.ATTRIBUTE,
                Identifier.of("spell_power", "arcane")
        );

        return Registries.ATTRIBUTE
                .getEntry(key)
                .orElseThrow(() -> new IllegalStateException("Atributo spell_power:arcane não encontrado"));
    }

    private static EntityAttributeModifier modifier(String itemName, String suffix, double value) {
        return new EntityAttributeModifier(
                Identifier.of(Monkmod.MOD_ID, itemName + "_" + suffix),
                value,
                EntityAttributeModifier.Operation.ADD_VALUE
        );
    }

    private MonkWeapons() { }
}