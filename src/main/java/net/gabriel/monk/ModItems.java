package net.gabriel.monk;

import net.gabriel.monk.item.MonkWeapons;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class ModItems {

    public static final Identifier MONK_POOL = Identifier.of(Monkmod.MOD_ID, "monk");

    // Spell Engine 1.9+: spell books usam item compartilhado data-driven
    public static final Identifier MONK_SPELL_BOOK_ID = Identifier.of("spell_engine", "spell_book");

    public static final Identifier SPIRITUAL_GAUNTLET_ID = Identifier.of(Monkmod.MOD_ID, "spiritual_gauntlet");

    public static Item MONK_SPELL_BOOK;
    public static Item SPIRITUAL_GAUNTLET;

    public static void registerAll() {
        MONK_SPELL_BOOK = Registries.ITEM.get(MONK_SPELL_BOOK_ID);

        MonkWeapons.register();

        SPIRITUAL_GAUNTLET = Registries.ITEM.get(SPIRITUAL_GAUNTLET_ID);

        Monkmod.LOGGER.info("[Monkmod] Itens registrados: spiritual_gauntlet");
    }

    private ModItems() { }
}