package mugasofer.aerb.item;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.entity.ClaretSpearEntity;
import mugasofer.aerb.skill.PlayerSkills;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Claret Spear - A blood magic spell that forms a spear from your blood.
 *
 * Mechanics:
 * - Costs 2 HP (1 heart) when equipped (blood forms into spear)
 * - Heals 2 HP when unequipped normally (blood returns to you)
 * - On throw: no heal on unequip (blood is spent with the spear)
 * - Damage scales with Blood Magic level
 * - Functions as a spear for parrying
 */
public class ClaretSpearItem extends Item implements SpellItem, DescribedItem {
    private static final float BLOOD_COST = 2.0f; // 1 heart

    // Track players currently holding the spear
    private static final Map<UUID, Boolean> playersHoldingSpear = new HashMap<>();
    // Track players who threw the spear (so we don't heal them on unequip)
    private static final Map<UUID, Boolean> playersThrewSpear = new HashMap<>();

    private static boolean eventRegistered = false;

    public ClaretSpearItem(Settings settings) {
        super(settings);
        registerTickEvent();
    }

    private void registerTickEvent() {
        if (eventRegistered) return;
        eventRegistered = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID playerId = player.getUuid();
                ItemStack mainHand = player.getMainHandStack();
                boolean isHoldingSpear = mainHand.getItem() instanceof ClaretSpearItem;
                boolean wasHoldingSpear = playersHoldingSpear.getOrDefault(playerId, false);

                if (isHoldingSpear && !wasHoldingSpear) {
                    // Just started holding - pay blood cost
                    net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) player.getEntityWorld();
                    player.damage(serverWorld, serverWorld.getDamageSources().magic(), BLOOD_COST);
                    playersHoldingSpear.put(playerId, true);
                    playersThrewSpear.remove(playerId);
                } else if (!isHoldingSpear && wasHoldingSpear) {
                    // Stopped holding
                    playersHoldingSpear.put(playerId, false);

                    // Heal if we didn't throw the spear (blood returns to us)
                    if (!playersThrewSpear.getOrDefault(playerId, false)) {
                        player.heal(BLOOD_COST);
                    }
                    playersThrewSpear.remove(playerId);
                }
            }
        });
    }

    /**
     * Mark that a player threw their spear (so we don't heal them on unequip).
     */
    public static void markSpearThrown(UUID playerId) {
        playersThrewSpear.put(playerId, true);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient()) {
            // Mark that this player threw the spear
            markSpearThrown(user.getUuid());

            // Get Blood Magic level for damage scaling
            int bloodMagicLevel = 0;
            if (user instanceof ServerPlayerEntity serverPlayer) {
                PlayerSkills skills = serverPlayer.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
                bloodMagicLevel = Math.max(0, skills.getSkillLevel(PlayerSkills.BLOOD_MAGIC));
            }

            // Create and throw the spear entity
            ItemStack stack = user.getStackInHand(hand);
            ClaretSpearEntity spearEntity = new ClaretSpearEntity(world, user, stack, bloodMagicLevel);
            spearEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 2.5f, 1.0f);

            world.spawnEntity(spearEntity);

            // Blood magic throw sound (frog leap + wet splash)
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_FROG_LONG_JUMP, SoundCategory.PLAYERS, 0.8f, 1.2f);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 0.5f, 1.5f);

            user.incrementStat(Stats.USED.getOrCreateStat(this));

            // Remove the spear from hand (it's now a projectile)
            // It will return to spell inventory when it lands
            user.setStackInHand(hand, ItemStack.EMPTY);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    /**
     * Get base damage for the spear based on Blood Magic level.
     * Base: 6 damage, +0.5 per Blood Magic level
     */
    public static float getDamage(int bloodMagicLevel) {
        return 6.0f + (bloodMagicLevel * 0.5f);
    }
}
