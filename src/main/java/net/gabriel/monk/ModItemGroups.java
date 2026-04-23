package net.gabriel.monk;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItemGroups {

    public static final Identifier MONGE_GROUP_ID = Identifier.of(Monkmod.MOD_ID, "monge");

    public static final ItemGroup MONGE_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            MONGE_GROUP_ID,
            FabricItemGroup.builder()
                    .displayName(Text.literal("Monge"))
                    .icon(() -> new ItemStack(Registries.ITEM.get(ModItems.MONK_SPELL_BOOK_ID)))
                    .entries((displayContext, entries) -> {
                        entries.add(Registries.ITEM.get(ModItems.MONK_SPELL_BOOK_ID));
                        entries.add(Registries.ITEM.get(ModItems.SPIRITUAL_GAUNTLET_ID));
                        entries.add(Registries.ITEM.get(ModItems.WOODEN_BATTLE_STAFF_ID));
                        entries.add(Registries.ITEM.get(ModItems.STONE_BATTLE_STAFF_ID));
                        entries.add(Registries.ITEM.get(ModItems.IRON_BATTLE_STAFF_ID));
                        entries.add(Registries.ITEM.get(ModItems.GOLDEN_BATTLE_STAFF_ID));
                        entries.add(Registries.ITEM.get(ModItems.DIAMOND_BATTLE_STAFF_ID));
                        entries.add(Registries.ITEM.get(ModItems.NETHERITE_BATTLE_STAFF_ID));
                    })
                    .build()
    );

    public static void registerAll() {
        Monkmod.LOGGER.info("[Monkmod] ItemGroup registrado: {}", MONGE_GROUP_ID);
    }

    private ModItemGroups() { }
}