package net.gabriel.monk;

import net.gabriel.monk.item.MonkWeapons;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class ModItems {

    public static final Identifier MONK_POOL = Identifier.of(Monkmod.MOD_ID, "monk");

    public static final Identifier MONK_SPELL_BOOK_ID = Identifier.of("spell_engine", "spell_book");

    public static final Identifier SPIRITUAL_GAUNTLET_ID = Identifier.of(Monkmod.MOD_ID, "spiritual_gauntlet");
    public static final Identifier WOODEN_BATTLE_STAFF_ID = Identifier.of(Monkmod.MOD_ID, "wooden_battle_staff");
    public static final Identifier STONE_BATTLE_STAFF_ID = Identifier.of(Monkmod.MOD_ID, "stone_battle_staff");
    public static final Identifier IRON_BATTLE_STAFF_ID = Identifier.of(Monkmod.MOD_ID, "iron_battle_staff");
    public static final Identifier GOLDEN_BATTLE_STAFF_ID = Identifier.of(Monkmod.MOD_ID, "golden_battle_staff");
    public static final Identifier DIAMOND_BATTLE_STAFF_ID = Identifier.of(Monkmod.MOD_ID, "diamond_battle_staff");
    public static final Identifier NETHERITE_BATTLE_STAFF_ID = Identifier.of(Monkmod.MOD_ID, "netherite_battle_staff");

    public static Item MONK_SPELL_BOOK;
    public static Item SPIRITUAL_GAUNTLET;
    public static Item WOODEN_BATTLE_STAFF;
    public static Item STONE_BATTLE_STAFF;
    public static Item IRON_BATTLE_STAFF;
    public static Item GOLDEN_BATTLE_STAFF;
    public static Item DIAMOND_BATTLE_STAFF;
    public static Item NETHERITE_BATTLE_STAFF;

    public static void registerAll() {
        MONK_SPELL_BOOK = Registries.ITEM.get(MONK_SPELL_BOOK_ID);

        MonkWeapons.register();

        SPIRITUAL_GAUNTLET = Registries.ITEM.get(SPIRITUAL_GAUNTLET_ID);
        WOODEN_BATTLE_STAFF = Registries.ITEM.get(WOODEN_BATTLE_STAFF_ID);
        STONE_BATTLE_STAFF = Registries.ITEM.get(STONE_BATTLE_STAFF_ID);
        IRON_BATTLE_STAFF = Registries.ITEM.get(IRON_BATTLE_STAFF_ID);
        GOLDEN_BATTLE_STAFF = Registries.ITEM.get(GOLDEN_BATTLE_STAFF_ID);
        DIAMOND_BATTLE_STAFF = Registries.ITEM.get(DIAMOND_BATTLE_STAFF_ID);
        NETHERITE_BATTLE_STAFF = Registries.ITEM.get(NETHERITE_BATTLE_STAFF_ID);

        Monkmod.LOGGER.info(
                "[Monkmod] Itens registrados: spiritual_gauntlet, wooden_battle_staff, stone_battle_staff, iron_battle_staff, golden_battle_staff, diamond_battle_staff, netherite_battle_staff"
        );
    }

    private ModItems() { }
}