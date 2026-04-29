package com.elthisboy.mapupgrades.mixin;

import com.elthisboy.mapupgrades.UpgradeDataHolder;
import com.elthisboy.mapupgrades.UpgradeManager;
import com.elthisboy.mapupgrades.UpgradeType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements UpgradeDataHolder {
    @Unique private int mapupgrades$walkSpeed;
    @Unique private int mapupgrades$stoneSpeed;
    @Unique private int mapupgrades$dirtSpeed;
    @Unique private int mapupgrades$woodSpeed;
    @Unique private int mapupgrades$maxHealth;

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void mapupgrades$write(NbtCompound nbt, CallbackInfo ci) {
        NbtCompound upgrades = new NbtCompound();
        upgrades.putInt(UpgradeType.WALK_SPEED.nbtKey(),  this.mapupgrades$walkSpeed);
        upgrades.putInt(UpgradeType.STONE_SPEED.nbtKey(), this.mapupgrades$stoneSpeed);
        upgrades.putInt(UpgradeType.DIRT_SPEED.nbtKey(),  this.mapupgrades$dirtSpeed);
        upgrades.putInt(UpgradeType.WOOD_SPEED.nbtKey(),  this.mapupgrades$woodSpeed);
        upgrades.putInt(UpgradeType.MAX_HEALTH.nbtKey(),  this.mapupgrades$maxHealth);
        nbt.put("MapUpgrades", upgrades);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void mapupgrades$read(NbtCompound nbt, CallbackInfo ci) {
        if (!nbt.contains("MapUpgrades")) {
            return;
        }
        NbtCompound upgrades = nbt.getCompound("MapUpgrades");
        this.mapupgrades$walkSpeed  = upgrades.getInt(UpgradeType.WALK_SPEED.nbtKey());
        this.mapupgrades$stoneSpeed = upgrades.getInt(UpgradeType.STONE_SPEED.nbtKey());
        this.mapupgrades$dirtSpeed  = upgrades.getInt(UpgradeType.DIRT_SPEED.nbtKey());
        this.mapupgrades$woodSpeed  = upgrades.getInt(UpgradeType.WOOD_SPEED.nbtKey());
        this.mapupgrades$maxHealth  = upgrades.getInt(UpgradeType.MAX_HEALTH.nbtKey());
    }

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void mapupgrades$modifyBreakSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        float multiplier = UpgradeManager.getBreakSpeedMultiplier(player, block);
        if (multiplier != 1.0F) {
            cir.setReturnValue(cir.getReturnValue() * multiplier);
        }
    }

    @Override
    public int mapupgrades$getUpgradeLevel(UpgradeType type) {
        return switch (type) {
            case WALK_SPEED  -> this.mapupgrades$walkSpeed;
            case STONE_SPEED -> this.mapupgrades$stoneSpeed;
            case DIRT_SPEED  -> this.mapupgrades$dirtSpeed;
            case WOOD_SPEED  -> this.mapupgrades$woodSpeed;
            case MAX_HEALTH  -> this.mapupgrades$maxHealth;
        };
    }

    @Override
    public void mapupgrades$setUpgradeLevel(UpgradeType type, int level) {
        int safe = Math.max(level, 0);
        switch (type) {
            case WALK_SPEED  -> this.mapupgrades$walkSpeed  = safe;
            case STONE_SPEED -> this.mapupgrades$stoneSpeed = safe;
            case DIRT_SPEED  -> this.mapupgrades$dirtSpeed  = safe;
            case WOOD_SPEED  -> this.mapupgrades$woodSpeed  = safe;
            case MAX_HEALTH  -> this.mapupgrades$maxHealth  = safe;
        }
    }

    @Override
    public void mapupgrades$addUpgradeLevel(UpgradeType type, int amount) {
        this.mapupgrades$setUpgradeLevel(type, this.mapupgrades$getUpgradeLevel(type) + amount);
    }

    @Override
    public void mapupgrades$copyUpgradesFrom(UpgradeDataHolder other) {
        for (UpgradeType type : UpgradeType.values()) {
            this.mapupgrades$setUpgradeLevel(type, other.mapupgrades$getUpgradeLevel(type));
        }
    }
}
