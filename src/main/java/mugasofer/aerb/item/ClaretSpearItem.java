package mugasofer.aerb.item;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.entity.ClaretSpearEntity;
import mugasofer.aerb.skill.PlayerSkills;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Claret Spear - A blood magic spell that forms a spear from your blood.
 *
 * Mechanics:
 * - Reduces max HP by 2 (1 heart) while held in main hand
 * - On normal unequip: restores max HP AND heals the lost HP
 * - On throw: restores max HP but does NOT heal (blood is spent)
 * - Damage scales with Blood Magic level
 * - Functions as a spear for parrying
 */
public class ClaretSpearItem extends Item implements SpellItem {
    private static final Identifier HEALTH_MODIFIER_ID = Identifier.of(Aerb.MOD_ID, "claret_spear_blood_cost");
    private static final double BLOOD_COST = -2.0; // Reduce max HP by 1 heart (2 HP)

    // Track players who have the HP modifier active
    private static final Map<UUID, Boolean> playersWithModifier = new HashMap<>();
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
                boolean hasModifier = playersWithModifier.getOrDefault(playerId, false);

                if (isHoldingSpear && !hasModifier) {
                    // Just started holding - apply HP reduction
                    applyBloodCost(player);
                    playersWithModifier.put(playerId, true);
                    playersThrewSpear.remove(playerId); // Reset throw flag
                } else if (!isHoldingSpear && hasModifier) {
                    // Stopped holding - restore HP
                    removeBloodCost(player);
                    playersWithModifier.put(playerId, false);

                    // Heal if we didn't throw the spear
                    if (!playersThrewSpear.getOrDefault(playerId, false)) {
                        // Heal the blood cost back
                        player.heal((float) -BLOOD_COST);
                    }
                    playersThrewSpear.remove(playerId);
                }
            }
        });
    }

    private void applyBloodCost(ServerPlayerEntity player) {
        EntityAttributeInstance healthAttr = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (healthAttr != null && healthAttr.getModifier(HEALTH_MODIFIER_ID) == null) {
            healthAttr.addPersistentModifier(new EntityAttributeModifier(
                HEALTH_MODIFIER_ID,
                BLOOD_COST,
                EntityAttributeModifier.Operation.ADD_VALUE
            ));
            // Clamp health if it exceeds new max
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    private static void removeBloodCost(ServerPlayerEntity player) {
        EntityAttributeInstance healthAttr = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(HEALTH_MODIFIER_ID);
        }
    }

    /**
     * Mark that a player threw their spear (so we don't heal them).
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

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0f, 1.0f);

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
