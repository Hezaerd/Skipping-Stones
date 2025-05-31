package com.hezaerd.item;

import com.hezaerd.entity.RockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class RockItem extends Item {
    
    public RockItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        world.playSound(user, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.3F, 1.2F);
        
        if (!world.isClient) {

            RockEntity rockEntity = new RockEntity(world, user, stack);
            rockEntity.setItem(stack);
            rockEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.0F, 1.0F);
            world.spawnEntity(rockEntity);
        }
        
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        if (!user.isCreative()) {
            stack.decrement(1);
        }
        
        return ActionResult.SUCCESS;
    }
}
