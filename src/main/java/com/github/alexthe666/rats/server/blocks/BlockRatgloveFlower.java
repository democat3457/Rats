package com.github.alexthe666.rats.server.blocks;

import com.github.alexthe666.rats.RatsMod;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

public class BlockRatgloveFlower extends BushBlock {

    public BlockRatgloveFlower() {
        super(Block.Properties.create(Material.PLANTS).sound(SoundType.PLANT).hardnessAndResistance(0F, 0.0F));
        this.setRegistryName(RatsMod.MODID, "ratglove_flower");
    }

    protected boolean isValidGround(BlockState state, IBlockReader worldIn, BlockPos pos) {
        return state.getBlock() == Blocks.SAND || state.getBlock() == Blocks.GRASS || state.getBlock() == Blocks.DIRT;
    }

    public Block.OffsetType getOffsetType() {
        return Block.OffsetType.XZ;
    }
}
