package mugasofer.aerb.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class PhysicalTappingItem extends Item implements SpellItem {
    private static final int EFFECT_DURATION = 120; // 6 seconds = 120 ticks

    public PhysicalTappingItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        // Only work on non-hostile creatures
        if (entity instanceof HostileEntity) {
            return ActionResult.PASS;
        }

        ItemStack boneStack = findBone(user);

        if (boneStack.isEmpty()) {
            entity.playSound(SoundEvents.BLOCK_BONE_BLOCK_BREAK, 0.5f, 0.5f);
            return ActionResult.FAIL;
        }

        if (!user.isRemoved() && boneStack.getCount() > 0) {
            boneStack.decrement(1);

            // Apply effects to the target entity
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, EFFECT_DURATION, 1));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, EFFECT_DURATION, 1));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, EFFECT_DURATION, 1));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, EFFECT_DURATION, 0));

            entity.playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
        }

        user.getItemCooldownManager().set(stack, 20);
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        // Find and consume a bone from the player's inventory
        ItemStack boneStack = findBone(user);

        if (boneStack.isEmpty()) {
            // No bones available - play a fail sound
            if (!world.isClient()) {
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BLOCK_BONE_BLOCK_BREAK, SoundCategory.PLAYERS, 0.5f, 0.5f);
            }
            return ActionResult.FAIL;
        }

        if (!world.isClient()) {
            // Consume the bone
            boneStack.decrement(1);

            // Apply the effects: Speed II, Haste II, Strength II, Regeneration I
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, EFFECT_DURATION, 1));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, EFFECT_DURATION, 1));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, EFFECT_DURATION, 1));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, EFFECT_DURATION, 0));

            // Play a bone-crunching sound
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        // Add cooldown to prevent spam (optional, 1 second)
        user.getItemCooldownManager().set(this.getDefaultStack(), 20);

        return ActionResult.SUCCESS;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    private ItemStack findBone(PlayerEntity player) {
        // Check offhand first (standard ammo behavior)
        ItemStack offhand = player.getOffHandStack();
        if (offhand.isOf(Items.BONE)) {
            return offhand;
        }

        // Then check main inventory
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.BONE)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
