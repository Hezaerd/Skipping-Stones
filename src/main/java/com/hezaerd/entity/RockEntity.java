package com.hezaerd.entity;

import com.hezaerd.registry.ModEntityType;
import com.hezaerd.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class RockEntity extends ThrownItemEntity {

    private static final float DEFAULT_DAMAGE = 1.0F;
    private static final double HORIZONTAL_DAMPING = 0.85; // Retain 85% of horizontal velocity
    private static final double MIN_SKIP_SPEED = 0.05; // Minimum speed required to skip
    private static final double BOUNCE_EFFICIENCY = 0.6; // How much vertical velocity to add

    public int skipsRemaining = 1 + random.nextInt(15); // 1-15 skips initially
    private boolean wasInWater = false;
    private boolean hasSkippedBefore = false;
    private int ticksInWater = 0;
    private int maxLifetime = 600; // 30 seconds max lifetime

    public RockEntity(EntityType<? extends RockEntity> entityType, World world) {
        super(entityType, world);
    }

    public RockEntity(World world, LivingEntity owner, ItemStack stack) {
        super(ModEntityType.ROCK, owner, world, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ROCK;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Entity entity = entityHitResult.getEntity();
            entity.damage(serverWorld, this.getDamageSources().thrown(this, this.getOwner()), DEFAULT_DAMAGE);
        }
        // Don't call super.onEntityHit to avoid immediate discard
        this.playSound(SoundEvents.BLOCK_STONE_BREAK, 1.0F, 1.2F);
        this.getWorld().sendEntityStatus(this, EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES);
        this.discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        // Only discard on solid block hits, not water
        if (!this.getWorld().isClient) {
            this.playSound(SoundEvents.BLOCK_STONE_BREAK, 1.0F, 1.2F);
            this.getWorld().sendEntityStatus(this, EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES);
            this.discard();
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        // Only handle non-water collisions here
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            // Check if it's water - if so, let tick() handle the skipping
            if (this.getWorld().getBlockState(blockHit.getBlockPos()).getFluidState().isEmpty()) {
                super.onCollision(hitResult);
            }
        } else {
            super.onCollision(hitResult);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient) return;

        maxLifetime--;
        if (maxLifetime <= 0) {
            this.discard();
            return;
        }

        boolean currentlyInWater = this.isTouchingWater();

        if (currentlyInWater) {
            ticksInWater++;

            if (!wasInWater && canSkip()) {
                performSkip();
            } else if (ticksInWater > 5) {
                this.discard();
                return;
            }
        } else {
            if (wasInWater) {
                ticksInWater = 0;
            }
        }

        wasInWater = currentlyInWater;
        if (currentlyInWater && this.random.nextInt(200) == 1) {
            this.discard();
        }
    }

    private boolean canSkip() {
        if (skipsRemaining <= 0) return false;

        Vec3d velocity = this.getVelocity();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        if (horizontalSpeed <= MIN_SKIP_SPEED) return false;

        if (!hasSkippedBefore) {
            return true;
        }

        return velocity.y > -0.3;
    }

    private void performSkip() {
        Vec3d currentVel = this.getVelocity();

        // Preserve and dampen horizontal momentum
        double newX = currentVel.x * HORIZONTAL_DAMPING;
        double newZ = currentVel.z * HORIZONTAL_DAMPING;

        // Calculate bounce based on impact speed
        double horizontalSpeed = Math.sqrt(newX * newX + newZ * newZ);
        double bounceHeight = Math.min(0.4, horizontalSpeed * BOUNCE_EFFICIENCY);

        // Apply the new velocity
        this.setVelocity(newX, bounceHeight, newZ);

        skipsRemaining--;
        hasSkippedBefore = true; // Mark that we've skipped at least once

        // Play skip sound
        this.playSound(SoundEvents.ENTITY_PLAYER_SPLASH, 0.8F, 1.0F + this.random.nextFloat() * 0.4F);
    }
}
