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

    private static final String GAUNTLET_SPELL_CHOICE_POOL = "monkmod:weapon/spiritual_gauntlet";
    private static final String BATTLE_STAFF_SPELL_CHOICE_POOL = "monkmod:weapon/battle_staff";

    private static final float PLAYER_BASE_ATTACK_DAMAGE = 1.0f;
    private static final float PLAYER_BASE_ATTACK_SPEED = 4.0f;

    private static final float GAUNTLET_DISPLAY_ATTACK_DAMAGE = 6.5f;
    private static final float GAUNTLET_DISPLAY_ATTACK_SPEED = 1.8f;
    private static final float GAUNTLET_ATTACK_RANGE_BONUS = -0.75f; // final 2.25
    private static final float GAUNTLET_ARCANE_POWER_BONUS = 5.5f;
    private static final float GAUNTLET_ARMOR_BONUS = 3.0f;

    private static final float BATTLE_STAFF_DISPLAY_ATTACK_SPEED = 1.4f;
    private static final float BATTLE_STAFF_ATTACK_RANGE_BONUS = 0.5f; // final 3.5

    public static Item SPIRITUAL_GAUNTLET;
    public static Item WOODEN_BATTLE_STAFF;
    public static Item STONE_BATTLE_STAFF;
    public static Item IRON_BATTLE_STAFF;
    public static Item GOLDEN_BATTLE_STAFF;
    public static Item DIAMOND_BATTLE_STAFF;
    public static Item NETHERITE_BATTLE_STAFF;

    public static void register() {
        SPIRITUAL_GAUNTLET = registerGauntlet(
                "spiritual_gauntlet",
                ToolMaterials.NETHERITE
        );

        WOODEN_BATTLE_STAFF = registerBattleStaff(
                "wooden_battle_staff",
                ToolMaterials.WOOD,
                3.0f,
                0.0f
        );

        STONE_BATTLE_STAFF = registerBattleStaff(
                "stone_battle_staff",
                ToolMaterials.STONE,
                4.0f,
                0.0f
        );

        IRON_BATTLE_STAFF = registerBattleStaff(
                "iron_battle_staff",
                ToolMaterials.IRON,
                5.0f,
                3.0f
        );

        GOLDEN_BATTLE_STAFF = registerBattleStaff(
                "golden_battle_staff",
                ToolMaterials.GOLD,
                3.0f,
                3.0f
        );

        DIAMOND_BATTLE_STAFF = registerBattleStaff(
                "diamond_battle_staff",
                ToolMaterials.DIAMOND,
                6.0f,
                4.0f
        );

        NETHERITE_BATTLE_STAFF = registerBattleStaff(
                "netherite_battle_staff",
                ToolMaterials.NETHERITE,
                6.5f,
                5.0f
        );

        Monkmod.LOGGER.info("[Monkmod] Weapons registradas: 7");
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

    private static Item registerBattleStaff(
            String name,
            ToolMaterial material,
            float displayAttackDamage,
            float arcanePower
    ) {
        Identifier id = Identifier.of(Monkmod.MOD_ID, name);

        Item.Settings settings = new Item.Settings()
                .maxDamage(material.getDurability())
                .attributeModifiers(createBattleStaffAttributes(
                        name,
                        displayAttackDamage,
                        BATTLE_STAFF_DISPLAY_ATTACK_SPEED,
                        BATTLE_STAFF_ATTACK_RANGE_BONUS,
                        arcanePower
                ))
                .component(
                        SpellDataComponents.SPELL_CONTAINER,
                        SpellContainers.forMagicWeapon()
                                .withBindingPool(Identifier.of(BATTLE_STAFF_SPELL_CHOICE_POOL))
                                .withMaxSpellCount(1)
                )
                .component(
                        SpellDataComponents.SPELL_CHOICE,
                        SpellChoice.of(BATTLE_STAFF_SPELL_CHOICE_POOL)
                );

        return Registry.register(
                Registries.ITEM,
                id,
                new BattleStaffItem(material, settings)
        );
    }

    private static AttributeModifiersComponent createGauntletAttributes(String itemName) {
        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();

        builder.add(
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                vanillaAttackDamageModifier(GAUNTLET_DISPLAY_ATTACK_DAMAGE),
                AttributeModifierSlot.MAINHAND
        );

        builder.add(
                EntityAttributes.GENERIC_ATTACK_SPEED,
                vanillaAttackSpeedModifier(GAUNTLET_DISPLAY_ATTACK_SPEED),
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

    private static AttributeModifiersComponent createBattleStaffAttributes(
            String itemName,
            float displayAttackDamage,
            float displayAttackSpeed,
            float attackRangeBonus,
            float arcanePower
    ) {
        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();

        builder.add(
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                vanillaAttackDamageModifier(displayAttackDamage),
                AttributeModifierSlot.MAINHAND
        );

        builder.add(
                EntityAttributes.GENERIC_ATTACK_SPEED,
                vanillaAttackSpeedModifier(displayAttackSpeed),
                AttributeModifierSlot.MAINHAND
        );

        builder.add(
                EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE,
                modifier(itemName, "entity_interaction_range", attackRangeBonus),
                AttributeModifierSlot.MAINHAND
        );

        if (arcanePower > 0) {
            builder.add(
                    arcaneAttribute(),
                    modifier(itemName, "arcane_power", arcanePower),
                    AttributeModifierSlot.MAINHAND
            );
        }

        return builder.build();
    }

    private static EntityAttributeModifier vanillaAttackDamageModifier(float displayedAttackDamage) {
        return new EntityAttributeModifier(
                Item.BASE_ATTACK_DAMAGE_MODIFIER_ID,
                displayedAttackDamage - PLAYER_BASE_ATTACK_DAMAGE,
                EntityAttributeModifier.Operation.ADD_VALUE
        );
    }

    private static EntityAttributeModifier vanillaAttackSpeedModifier(float displayedAttackSpeed) {
        return new EntityAttributeModifier(
                Item.BASE_ATTACK_SPEED_MODIFIER_ID,
                displayedAttackSpeed - PLAYER_BASE_ATTACK_SPEED,
                EntityAttributeModifier.Operation.ADD_VALUE
        );
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