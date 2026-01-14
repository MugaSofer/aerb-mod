package mugasofer.aerb.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.List;

public class BloodMagicItem extends Item {

    private static final float BLOOD_COST_CHANCE = 0.05f; // 5% chance to take damage
    private static final float BLOOD_DAMAGE = 1.0f; // Half a heart

    public record SpellEffect(RegistryEntry<StatusEffect> effect, int durationTicks, int amplifier) {}

    private final List<SpellEffect> effects;

    public BloodMagicItem(Settings settings, List<SpellEffect> effects) {
        super(settings);
        this.effects = effects;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        // Only work on non-hostile creatures
        if (entity instanceof HostileEntity) {
            return ActionResult.PASS;
        }

        World world = user.getEntityWorld();
        if (!world.isClient()) {
            // Apply blood cost (5% chance of armor-bypassing damage)
            applyBloodCost(user, (ServerWorld) world);

            // Apply effects to the target entity
            for (SpellEffect spellEffect : effects) {
                entity.addStatusEffect(new StatusEffectInstance(
                    spellEffect.effect(),
                    spellEffect.durationTicks(),
                    spellEffect.amplifier()
                ));
            }

            // Play blood magic sound
            entity.playSound(SoundEvents.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, 0.5f, 1.2f);
        }

        user.getItemCooldownManager().set(stack, 20);
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient()) {
            // Apply blood cost (5% chance of armor-bypassing damage)
            applyBloodCost(user, (ServerWorld) world);

            // Apply all effects to self
            for (SpellEffect spellEffect : effects) {
                user.addStatusEffect(new StatusEffectInstance(
                    spellEffect.effect(),
                    spellEffect.durationTicks(),
                    spellEffect.amplifier()
                ));
            }

            // Play blood magic sound
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 0.5f, 1.2f);
        }

        // Add cooldown to prevent spam (1 second)
        user.getItemCooldownManager().set(this.getDefaultStack(), 20);

        return ActionResult.SUCCESS;
    }

    private void applyBloodCost(PlayerEntity player, ServerWorld world) {
        if (world.getRandom().nextFloat() < BLOOD_COST_CHANCE) {
            // Use magic damage (bypasses armor)
            DamageSource magicDamage = world.getDamageSources().magic();
            player.damage(world, magicDamage, BLOOD_DAMAGE);

            // Extra feedback when blood cost is paid
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 0.3f, 1.5f);
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
