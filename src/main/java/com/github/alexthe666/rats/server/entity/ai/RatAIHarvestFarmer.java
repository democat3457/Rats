package com.github.alexthe666.rats.server.entity.ai;

import com.github.alexthe666.rats.server.entity.EntityRat;
import com.github.alexthe666.rats.server.entity.RatCommand;
import com.github.alexthe666.rats.server.items.RatsItemRegistry;
import net.minecraft.block.*;
import net.minecraft.block.state.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.util.Direction;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RatAIHarvestFarmer extends EntityAIBase {
    private static final int RADIUS = 16;
    private final EntityRat entity;
    private final BlockSorter targetSorter;
    private BlockPos targetBlock = null;


    public RatAIHarvestFarmer(EntityRat entity) {
        super();
        this.entity = entity;
        this.targetSorter = new BlockSorter(entity);
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (!this.entity.canMove() || !this.entity.isTamed() || this.entity.getCommand() != RatCommand.HARVEST || this.entity.isInCage() || !entity.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_FARMER) || !holdingSeeds() && !holdingBlock() && !holdingBonemeal()) {
            return false;
        }
        resetTarget();
        return targetBlock != null;
    }

    private boolean holdingSeeds() {
        return !this.entity.getHeldItem(EnumHand.MAIN_HAND).isEmpty() && (this.entity.getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof ItemSeeds || this.entity.getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof ItemSeedFood);
    }

    private boolean holdingBonemeal() {
        ItemStack stack = this.entity.getHeldItem(EnumHand.MAIN_HAND);
        return !stack.isEmpty() && stack.getItem() == Items.DYE && EnumDyeColor.byDyeDamage(stack.getMetadata()) == EnumDyeColor.WHITE;
    }

    private boolean holdingBlock() {
        return !this.entity.getHeldItem(EnumHand.MAIN_HAND).isEmpty() && (this.entity.getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof ItemBlock);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return targetBlock != null && (holdingSeeds() || holdingBonemeal() || holdingBlock());
    }

    public void resetTask() {
        this.entity.getNavigator().clearPath();
        resetTarget();
    }


    @Override
    public void updateTask() {
        if (this.targetBlock != null) {
            if (holdingSeeds()) {
                BlockState block = this.entity.world.getBlockState(this.targetBlock);
                this.entity.getNavigator().tryMoveToXYZ(this.targetBlock.getX() + 0.5D, this.targetBlock.getY(), this.targetBlock.getZ() + 0.5D, 1D);
                if (block.getBlock().isFertile(entity.world, targetBlock) && entity.world.isAirBlock(targetBlock.up())) {
                    double distance = this.entity.getDistance(this.targetBlock.getX(), this.targetBlock.getY(), this.targetBlock.getZ());
                    if (distance < 1.5F) {
                        if (holdingSeeds()) {
                            ItemStack seedStack = this.entity.getHeldItem(EnumHand.MAIN_HAND).copy();
                            seedStack.setCount(1);
                            this.entity.getHeldItem(EnumHand.MAIN_HAND).shrink(1);
                            if (seedStack.getItem() instanceof ItemSeeds) {
                                entity.world.setBlockState(targetBlock.up(), ((ItemSeeds) seedStack.getItem()).getPlant(entity.world, targetBlock.up()));
                            }
                            if (seedStack.getItem() instanceof ItemSeedFood) {
                                entity.world.setBlockState(targetBlock.up(), ((ItemSeedFood) seedStack.getItem()).getPlant(entity.world, targetBlock.up()));
                            }
                        }
                        this.targetBlock = null;
                        this.resetTask();
                    }
                } else {
                    this.targetBlock = null;
                    this.resetTask();
                }
            }
            if (holdingBonemeal()) {
                BlockState block = this.entity.world.getBlockState(this.targetBlock);
                this.entity.getNavigator().tryMoveToXYZ(this.targetBlock.getX() + 0.5D, this.targetBlock.getY(), this.targetBlock.getZ() + 0.5D, 1D);
                if (canPlantBeBonemealed(targetBlock, block)) {
                    double distance = this.entity.getDistance(this.targetBlock.getX(), this.targetBlock.getY(), this.targetBlock.getZ());
                    if (distance < 1.5F) {
                        if (holdingBonemeal()) {
                            this.entity.getHeldItem(EnumHand.MAIN_HAND).shrink(1);
                            if (block.getBlock() instanceof IGrowable) {
                                IGrowable igrowable = (IGrowable) block.getBlock();
                                if (igrowable.canGrow(entity.world, targetBlock, block, entity.world.isRemote)) {
                                    if (!entity.world.isRemote) {
                                        entity.world.playEvent(2005, targetBlock, 0);
                                        igrowable.grow(entity.world, entity.world.rand, targetBlock, block);
                                    }
                                }
                            }
                        }
                        this.targetBlock = null;
                        this.resetTask();
                    }
                } else{
                    this.targetBlock = null;
                    this.resetTask();
                }
            } else if (holdingBlock()) {
                ItemBlock itemBlock = ((ItemBlock)entity.getHeldItem(EnumHand.MAIN_HAND).getItem());
                this.entity.getNavigator().tryMoveToXYZ(this.targetBlock.getX() + 0.5D, this.targetBlock.getY(), this.targetBlock.getZ() + 0.5D, 1D);
                if (entity.world.mayPlace(itemBlock.getBlock(), this.targetBlock, false, Direction.UP, (Entity)null)) {
                    double distance = this.entity.getDistance(this.targetBlock.getX(), this.targetBlock.getY(), this.targetBlock.getZ());
                    if (distance < 1.5F) {
                        if (holdingBlock()) {
                            BlockState BlockState1 = itemBlock.getBlock().getStateForPlacement(entity.world, targetBlock, entity.getHorizontalFacing(), 0, 0, 0, entity.getHeldItem(EnumHand.MAIN_HAND).getMetadata(), entity, EnumHand.MAIN_HAND);
                            this.entity.getHeldItem(EnumHand.MAIN_HAND).shrink(1);
                            entity.world.setBlockState(targetBlock,  BlockState1);
                            if(entity.isEntityInsideOpaqueBlock()){
                                entity.setPosition(entity.posX, entity.posY + 1, entity.posZ);
                            }
                            SoundType placeSound =  BlockState1.getBlock().getSoundType(BlockState1, entity.world, targetBlock, entity);
                            entity.playSound(placeSound.getPlaceSound(), (placeSound.getVolume() + 1.0F) / 2.0F, placeSound.getPitch() * 0.8F);
                        }
                         this.targetBlock = null;
                        this.resetTask();
                    }
                } else {
                    this.targetBlock = null;
                    this.resetTask();
                }
            }

        }
    }

    private void resetTarget() {
        if (holdingBonemeal()) {
            List<BlockPos> allBlocks = new ArrayList<>();
            for (BlockPos pos : BlockPos.getAllInBox(this.entity.getPosition().add(-RADIUS, -RADIUS, -RADIUS), this.entity.getPosition().add(RADIUS, RADIUS, RADIUS))) {
                if (canPlantBeBonemealed(pos, this.entity.world.getBlockState(pos))) {
                    allBlocks.add(pos);
                }
            }
            if (!allBlocks.isEmpty()) {
                allBlocks.sort(this.targetSorter);
                this.targetBlock = allBlocks.get(0);
            }
        } else if(holdingSeeds()){
            List<BlockPos> allBlocks = new ArrayList<>();
            for (BlockPos pos : BlockPos.getAllInBox(this.entity.getPosition().add(-RADIUS, -RADIUS, -RADIUS), this.entity.getPosition().add(RADIUS, RADIUS, RADIUS))) {
                if (entity.world.getBlockState(pos).getBlock().isFertile(entity.world, pos) && entity.world.isAirBlock(pos.up())) {
                    allBlocks.add(pos);
                }
            }
            if (!allBlocks.isEmpty()) {
                allBlocks.sort(this.targetSorter);
                this.targetBlock = allBlocks.get(0);
            }
        } else if(holdingBlock()){
            List<BlockPos> allBlocks = new ArrayList<>();
            Block block = Blocks.SAPLING;
            if(this.entity.getHeldItem(EnumHand.MAIN_HAND).getItem() != null && this.entity.getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof ItemBlock){
                block = ((ItemBlock)this.entity.getHeldItem(EnumHand.MAIN_HAND).getItem()).getBlock();
            }
            for (BlockPos pos : BlockPos.getAllInBox(this.entity.getPosition().add(-RADIUS, -RADIUS, -RADIUS), this.entity.getPosition().add(RADIUS, RADIUS, RADIUS))) {
                if (entity.world.mayPlace(block, pos, false, Direction.UP, (Entity)null) && entity.world.isAirBlock(pos.up())) {
                    allBlocks.add(pos);
                }
            }
            if (!allBlocks.isEmpty()) {
                allBlocks.sort(this.targetSorter);
                this.targetBlock = allBlocks.get(0);
            }
        }

    }

    private boolean canPlantBeBonemealed(BlockPos pos, BlockState BlockState) {
        if (BlockState.getBlock() instanceof IGrowable && !(BlockState.getBlock() instanceof BlockTallGrass) && !(BlockState.getBlock() instanceof BlockGrass)) {
            IGrowable igrowable = (IGrowable) BlockState.getBlock();
            if (igrowable.canGrow(entity.world, pos, BlockState, entity.world.isRemote)) {
                if (!entity.world.isRemote) {
                    //  igrowable.grow(worldIn, worldIn.rand, target, BlockState);
                    return igrowable.canUseBonemeal(entity.world, entity.world.rand, pos, BlockState);
                }
            }
        }
        return false;
    }

    public class BlockSorter implements Comparator<BlockPos> {
        private final Entity entity;

        public BlockSorter(Entity entity) {
            this.entity = entity;
        }

        @Override
        public int compare(BlockPos pos1, BlockPos pos2) {
            double distance1 = this.getDistance(pos1);
            double distance2 = this.getDistance(pos2);
            return Double.compare(distance1, distance2);
        }

        private double getDistance(BlockPos pos) {
            double deltaX = this.entity.posX - (pos.getX() + 0.5);
            double deltaY = this.entity.posY + this.entity.getEyeHeight() - (pos.getY() + 0.5);
            double deltaZ = this.entity.posZ - (pos.getZ() + 0.5);
            return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        }
    }
}
