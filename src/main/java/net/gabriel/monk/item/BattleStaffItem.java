package net.gabriel.monk.item;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ToolMaterial;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.spell_engine.api.item.weapon.SpellWeaponItem;

public class BattleStaffItem extends SpellWeaponItem {

    public BattleStaffItem(ToolMaterial material, Settings settings) {
        super(material, settings);
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        return miner != null && !miner.isCreative();
    }
}