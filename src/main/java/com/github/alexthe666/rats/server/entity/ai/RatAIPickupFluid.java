package com.github.alexthe666.rats.server.entity.ai;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.EntityRat;
import com.github.alexthe666.rats.server.entity.RatCommand;
import com.github.alexthe666.rats.server.entity.RatUtils;
import com.github.alexthe666.rats.server.items.RatsItemRegistry;
import com.github.alexthe666.rats.server.message.MessageUpdateRatFluid;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public class RatAIPickupFluid extends EntityAIBase {
    private static final int RADIUS = 16;
    private final EntityRat entity;
    private BlockPos targetBlock = null;
    private int feedingTicks;

    public RatAIPickupFluid(EntityRat entity) {
        super();
        this.entity = entity;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (!this.entity.canMove() || !this.entity.isTamed() || !canPickUp() || entity.getAttackTarget() != null || entity.getMBTransferRate() == 0) {
            return false;
        }
        if (this.entity.transportingFluid != null && this.entity.transportingFluid.amount >= this.entity.getMBTransferRate()) {
            return false;
        }
        resetTarget();
        return targetBlock != null;
    }

    private void resetTarget() {
        this.targetBlock = entity.pickupPos;
    }

    private boolean canPickUp() {
        return this.entity.getCommand() == RatCommand.TRANSPORT || this.entity.getCommand() == RatCommand.HARVEST && this.entity.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_FARMER);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return targetBlock != null && (this.entity.transportingFluid == null || this.entity.transportingFluid.amount < this.entity.getMBTransferRate());
    }

    public void resetTask() {
        this.entity.getNavigator().clearPath();
        resetTarget();
    }

    public boolean canSeeChest() {
        RayTraceResult rayTrace = RatUtils.rayTraceBlocksIgnoreRatholes(entity.world, entity.getPositionVector(), new Vec3d(targetBlock.getX() + 0.5, targetBlock.getY() + 0.5, targetBlock.getZ() + 0.5), false);
        if (rayTrace != null && rayTrace.hitVec != null) {
            BlockPos sidePos = rayTrace.getBlockPos();
            BlockPos pos = new BlockPos(rayTrace.hitVec);
            return entity.world.isAirBlock(sidePos) || entity.world.isAirBlock(pos) || this.entity.world.getTileEntity(pos) == this.entity.world.getTileEntity(targetBlock);
        }
        return true;
    }

    @Override
    public void updateTask() {
        if (this.targetBlock != null && this.entity.world.getTileEntity(this.targetBlock) != null) {
            TileEntity entity = this.entity.world.getTileEntity(this.targetBlock);
            this.entity.getNavigator().tryMoveToXYZ(this.targetBlock.getX() + 0.5D, this.targetBlock.getY(), this.targetBlock.getZ() + 0.5D, 1D);
            double distance = this.entity.getDistance(this.targetBlock.getX() + 0.5D, this.targetBlock.getY() + 1, this.targetBlock.getZ() + 0.5D);
            if (distance <= 1.7 && canSeeChest()) {
                IFluidHandler handler = entity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.DOWN);
                if (handler == null) {
                    return;
                }
                int currentAmount = 0;
                if(this.entity.transportingFluid != null){
                    currentAmount = this.entity.transportingFluid.amount;
                }
                int howMuchWeWant = this.entity.getMBTransferRate() - currentAmount;

                FluidStack drainedStack = null;
                try {
                    int totalTankHeld = 0;
                    if(handler.getTankProperties().length > 0){
                        IFluidTankProperties firstTank = handler.getTankProperties()[0];
                        if(handler.getTankProperties().length > 1){
                            for(IFluidTankProperties otherTank : handler.getTankProperties()){
                                if(this.entity.transportingFluid != null && this.entity.transportingFluid.isFluidEqual(otherTank.getContents())){
                                    firstTank = otherTank;
                                }
                            }
                        }
                        if(firstTank.getContents() != null && (this.entity.transportingFluid == null || this.entity.transportingFluid.isFluidEqual(firstTank.getContents()))){
                            howMuchWeWant = Math.min(firstTank.getContents().amount, howMuchWeWant);

                            if (handler.drain(howMuchWeWant, false) != null) {
                                drainedStack = handler.drain(howMuchWeWant, true);
                            }
                        }

                    }
                } catch (Exception e) {
                    //container is empty
                }
                if (drainedStack == null) {
                    this.targetBlock = null;
                    this.resetTask();
                } else {
                    if(this.entity.transportingFluid == null){
                        this.entity.transportingFluid = drainedStack.copy();
                    }else{
                        this.entity.transportingFluid.amount += Math.max(drainedStack.amount, 0);
                    }
                    if(!this.entity.world.isRemote){
                        RatsMod.NETWORK_WRAPPER.sendToAll(new MessageUpdateRatFluid(this.entity.getEntityId(), this.entity.transportingFluid));
                    }
                    SoundEvent sound = this.entity.transportingFluid == null ? SoundEvents.ITEM_BUCKET_FILL : this.entity.transportingFluid.getFluid().getFillSound();
                    this.entity.playSound(sound, 1, 1);
                    this.targetBlock = null;
                    this.resetTask();
                }
            }

        }
    }
}
