package com.hezaerd.entity;

import com.hezaerd.registry.ModEntityType;
import com.hezaerd.registry.ModItems;
import com.hezaerd.registry.ModStats;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RockEntity extends ThrownItemEntity {

    private static final float DEFAULT_DAMAGE = 1.0F;
    private static final double HORIZONTAL_DAMPING = 0.85;
    private static final double MIN_SKIP_SPEED = 0.05;
    private static final double BOUNCE_EFFICIENCY = 0.6;

    private static final int[] FIREWORK_THRESHOLDS = {5, 10, 15, 20};
    
    public int skipsRemaining = 3 + random.nextInt(13);
    private boolean wasInWater = false;
    private boolean hasSkippedBefore = false;
    private int ticksInWater = 0;
    private int maxLifetime = 600;

    private int currentSkipsPerformed = 0;
    private Vec3d initialPosition;
    private boolean hasTriggeredFirework = false;
    
    public RockEntity(EntityType<? extends RockEntity> entityType, World world) {
        super(entityType, world);
        if (!world.isClient()) {
            this.initialPosition = this.getPos();
        }
    }

    public RockEntity(World world, LivingEntity owner, ItemStack stack) {
        super(ModEntityType.ROCK, owner, world, stack);
        if (!world.isClient()) {
            this.initialPosition = this.getPos();
        }
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
        this.playSound(SoundEvents.BLOCK_STONE_BREAK, 1.0F, 1.2F);
        this.getWorld().sendEntityStatus(this, EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES);
        this.discard(); // discard() will eventually call our overridden remove()
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (!this.getWorld().isClient) {
            this.playSound(SoundEvents.BLOCK_STONE_BREAK, 1.0F, 1.2F);
            this.getWorld().sendEntityStatus(this, EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES);
            this.discard(); // discard() will eventually call our overridden remove()
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
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
        if (maxLifetime <= 0 && !this.isRemoved()) { // Check !isRemoved
            this.discard();
            return;
        }

        boolean currentlyInWater = this.isTouchingWater();
        if (currentlyInWater) {
            ticksInWater++;
            if (!wasInWater && canSkip()) {
                performSkip();
            } else if (ticksInWater > 5 && !this.isRemoved()) { // Check !isRemoved
                this.discard();
                return;
            }
        } else {
            if (wasInWater) {
                ticksInWater = 0;
            }
        }

        wasInWater = currentlyInWater;
        if (currentlyInWater && this.random.nextInt(200) == 1 && !this.isRemoved()) { // Check !isRemoved
            this.discard();
        }
    }

    private boolean canSkip() {
        if (skipsRemaining <= 0) return false;
        Vec3d velocity = this.getVelocity();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed <= MIN_SKIP_SPEED) return false;
        if (!hasSkippedBefore) return true;
        return velocity.y > -0.3;
    }

    private void performSkip() {
        Vec3d currentVel = this.getVelocity();
        double newX = currentVel.x * HORIZONTAL_DAMPING;
        double newZ = currentVel.z * HORIZONTAL_DAMPING;
        double horizontalSpeed = Math.sqrt(newX * newX + newZ * newZ);
        double bounceHeight = Math.min(0.4, horizontalSpeed * BOUNCE_EFFICIENCY);
        this.setVelocity(newX, bounceHeight, newZ);

        skipsRemaining--;
        currentSkipsPerformed++;
        hasSkippedBefore = true;

        this.playSound(SoundEvents.ENTITY_PLAYER_SPLASH, 0.8F, 1.0F + this.random.nextFloat() * 0.4F);

        if (!this.getWorld().isClient()) {
            Entity owner = this.getOwner();
            if (owner instanceof ServerPlayerEntity player) {
                // display current skip count, just the number
                player.sendMessage(Text.literal((String.valueOf(currentSkipsPerformed))), true);
            }
        }

        if (currentSkipsPerformed == 6 && !hasTriggeredFirework) {
            spawnCelebrationFirework();
            hasTriggeredFirework = true;
        }
    }
    
    private void spawnCelebrationFirework() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;

        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);

        // Random firework colors
        DyeColor[] colors = DyeColor.values();
        DyeColor primaryColor = colors[random.nextInt(colors.length)];
        DyeColor secondaryColor = colors[random.nextInt(colors.length)];

        // Random firework shape
        FireworkExplosionComponent.Type[] shapes = FireworkExplosionComponent.Type.values();
        FireworkExplosionComponent.Type shape = shapes[random.nextInt(shapes.length)];

        // Random effects
        boolean hasTrail = random.nextBoolean();
        boolean hasTwinkle = random.nextBoolean();

        FireworkExplosionComponent explosion = new FireworkExplosionComponent(
                shape,
                IntList.of(primaryColor.getFireworkColor(), secondaryColor.getFireworkColor()),
                IntList.of(), // No fade colors
                hasTrail,
                hasTwinkle
        );

        FireworksComponent fireworksComponent = new FireworksComponent(
                1 + random.nextInt(2), // Random flight duration 1-2
                List.of(explosion)
        );

        fireworkStack.set(DataComponentTypes.FIREWORKS, fireworksComponent);

        // Spawn firework slightly above the rock position
        Vec3d spawnPos = this.getPos().add(0, 2, 0);
        FireworkRocketEntity firework = new FireworkRocketEntity(
                this.getWorld(),
                fireworkStack,
                spawnPos.x,
                spawnPos.y,
                spawnPos.z,
                true
        );

        ProjectileEntity.spawn(firework, serverWorld, fireworkStack);
    }
    
    // Override remove instead of discard
    @Override
    public void remove(Entity.RemovalReason reason) {
        // Perform stat updates before the entity is actually removed
        if (!this.getWorld().isClient()) { // Check if on server
            Entity owner = this.getOwner();
            if (owner instanceof ServerPlayerEntity player) {
                ServerStatHandler statHandler = player.getStatHandler();

                // Update BEST_ROCK_SKIPS
                int oldBestSkips = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(ModStats.BEST_ROCK_SKIPS));
                if (currentSkipsPerformed > oldBestSkips)
                    statHandler.setStat(player, Stats.CUSTOM.getOrCreateStat(ModStats.BEST_ROCK_SKIPS), currentSkipsPerformed);

                // Update BEST_SKIP_DISTANCE if at least one skip occurred
                if (hasSkippedBefore && this.initialPosition != null) {
                    double distanceTraveled = this.getPos().distanceTo(this.initialPosition);
                    int distanceCm = (int) (distanceTraveled * 100.0);

                    int oldBestDistance = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(ModStats.BEST_SKIP_DISTANCE));
                    if (distanceCm > oldBestDistance)
                        statHandler.setStat(player, Stats.CUSTOM.getOrCreateStat(ModStats.BEST_SKIP_DISTANCE), distanceCm);
                }
            }
        }
        super.remove(reason); // Call the super method to ensure proper removal
    }
}