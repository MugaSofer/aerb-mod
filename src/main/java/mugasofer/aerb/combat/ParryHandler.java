package mugasofer.aerb.combat;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.item.ModItems;
import mugasofer.aerb.network.ModNetworking;
import mugasofer.aerb.skill.PlayerSkills;
import mugasofer.aerb.stat.StatCalculator;
import mugasofer.aerb.virtue.VirtueInventory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Handles attack-based parry system.
 * When swinging a sword/axe, player can parry frontal attacks.
 * Parry success is determined by dice roll: 1d100 + (SPD x Parry) vs 1d100 + modifier
 */
public class ParryHandler {
    // Attack animation duration in milliseconds
    // Minecraft sword attack cooldown is ~0.625 seconds (12.5 ticks)
    private static final long ATTACK_SWING_DURATION_MS = 500;

    // Frontal attack angle (degrees from facing direction)
    private static final double FRONTAL_ANGLE_DEGREES = 90.0;

    // Dice roll modifiers
    private static final int ARROW_MODIFIER = 25;

    // Track last attack time per player
    private static final Map<UUID, AttackState> attackStates = new HashMap<>();

    private static final Random random = new Random();

    /**
     * Record a player's attack swing.
     * Called when player attacks with a sword or axe.
     */
    public static void recordAttack(ServerPlayerEntity player, ItemStack weapon) {
        if (!isParryableWeapon(weapon)) {
            return;
        }

        UUID playerId = player.getUuid();
        attackStates.put(playerId, new AttackState(System.currentTimeMillis(), weapon.copy()));
        Aerb.LOGGER.debug("Recorded attack swing for {}", player.getName().getString());
    }

    /**
     * Check if an item is a parryable weapon (sword or axe).
     */
    public static boolean isParryableWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // Check swords
        if (stack.isOf(Items.WOODEN_SWORD) || stack.isOf(Items.STONE_SWORD) ||
            stack.isOf(Items.IRON_SWORD) || stack.isOf(Items.GOLDEN_SWORD) ||
            stack.isOf(Items.DIAMOND_SWORD) || stack.isOf(Items.NETHERITE_SWORD)) {
            return true;
        }
        // Check axes
        if (stack.isOf(Items.WOODEN_AXE) || stack.isOf(Items.STONE_AXE) ||
            stack.isOf(Items.IRON_AXE) || stack.isOf(Items.GOLDEN_AXE) ||
            stack.isOf(Items.DIAMOND_AXE) || stack.isOf(Items.NETHERITE_AXE)) {
            return true;
        }
        return false;
    }

    /**
     * Check if player is currently in their attack swing animation.
     */
    public static boolean isInAttackSwing(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        AttackState state = attackStates.get(playerId);

        if (state == null) {
            return false;
        }

        long elapsed = System.currentTimeMillis() - state.attackTime;
        return elapsed <= ATTACK_SWING_DURATION_MS;
    }

    /**
     * Check if an attack is coming from in front of the player.
     * Returns true if the angle between player's facing direction and
     * the direction to the attacker is within FRONTAL_ANGLE_DEGREES.
     */
    public static boolean isFrontalAttack(ServerPlayerEntity player, DamageSource source) {
        Entity attacker = source.getAttacker();
        if (attacker == null) {
            // For projectiles, use the projectile's position
            Entity sourceEntity = source.getSource();
            if (sourceEntity != null) {
                attacker = sourceEntity;
            } else {
                return false; // Can't determine direction
            }
        }

        // Get player's facing direction (horizontal only)
        Vec3d playerFacing = player.getRotationVec(1.0f);
        Vec3d horizontalFacing = new Vec3d(playerFacing.x, 0, playerFacing.z).normalize();

        // Get direction from player to attacker
        Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d attackerPos = new Vec3d(attacker.getX(), attacker.getY(), attacker.getZ());
        Vec3d toAttacker = attackerPos.subtract(playerPos);
        Vec3d horizontalToAttacker = new Vec3d(toAttacker.x, 0, toAttacker.z).normalize();

        // Calculate angle between facing and attacker direction
        double dot = horizontalFacing.dotProduct(horizontalToAttacker);
        double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));

        return angle <= FRONTAL_ANGLE_DEGREES;
    }

    /**
     * Attempt to parry an incoming attack.
     * Returns true if parry succeeds, false otherwise.
     * This should be called when a player in attack swing is hit from the front.
     */
    public static ParryResult attemptParry(ServerPlayerEntity player, DamageSource source, float damage) {
        // Get player's stats and skills
        PlayerSkills skills = player.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
        int parryLevel = Math.max(0, skills.getSkillLevel(PlayerSkills.PARRY));
        int spd = StatCalculator.calculateSPD(player);

        // Unlock parry skill on first attempt
        boolean skillUnlocked = false;
        if (!skills.isUnlocked(PlayerSkills.PARRY)) {
            skills.setSkillLevel(PlayerSkills.PARRY, 0);
            player.sendMessage(Text.literal("New Skill: Parry!"), false);
            ModNetworking.syncSkillsToClient(player);
            skillUnlocked = true;
            parryLevel = 0;
        }

        // Calculate dice rolls
        // Player: 1d100 + (SPD x Parry)
        int playerRoll = random.nextInt(100) + 1;
        int playerBonus = spd * parryLevel;
        int playerTotal = playerRoll + playerBonus;

        // Enemy: 1d100 + modifier
        int enemyRoll = random.nextInt(100) + 1;
        int enemyModifier = getAttackModifier(player, source);
        int enemyTotal = enemyRoll + enemyModifier;

        // Determine success (player wins ties)
        boolean success = playerTotal >= enemyTotal;

        Aerb.LOGGER.info("Parry attempt: Player rolled {} + {} (SPD {} x Parry {}) = {} vs Enemy {} + {} = {} -> {}",
            playerRoll, playerBonus, spd, parryLevel, playerTotal,
            enemyRoll, enemyModifier, enemyTotal,
            success ? "SUCCESS" : "FAIL");

        if (success) {
            onParrySuccess(player, damage);
        } else {
            onParryFail(player);
        }

        return new ParryResult(success, skillUnlocked, playerTotal, enemyTotal);
    }

    /**
     * Get the attack modifier for a damage source.
     * Prescient Blade virtue halves projectile modifier.
     */
    private static int getAttackModifier(ServerPlayerEntity player, DamageSource source) {
        // Arrows and other projectiles get +25 (or +12 with Prescient Blade)
        if (source.getSource() instanceof ProjectileEntity) {
            if (hasVirtue(player, ModItems.PRESCIENT_BLADE)) {
                return ARROW_MODIFIER / 2; // Half penalty with Prescient Blade
            }
            return ARROW_MODIFIER;
        }
        // Default: no modifier
        return 0;
    }

    /**
     * Check if a player has a specific virtue in their virtue inventory.
     */
    private static boolean hasVirtue(ServerPlayerEntity player, net.minecraft.item.Item virtue) {
        VirtueInventory virtueInv = player.getAttachedOrCreate(VirtueInventory.ATTACHMENT);
        for (int i = 0; i < virtueInv.size(); i++) {
            if (virtueInv.getStack(i).isOf(virtue)) {
                return true;
            }
        }
        return false;
    }

    // Minimum damage threshold for weapon durability loss (like shields)
    private static final int PARRY_DAMAGE_THRESHOLD = 3;

    /**
     * Called when a parry succeeds.
     * Damages the weapon used to parry (like shields - damage equal to attack, threshold of 3).
     */
    private static void onParrySuccess(ServerPlayerEntity player, float attackDamage) {
        // Play parry success sound (metallic clang)
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.2f);

        // Damage the weapon (durability = attack damage, with threshold of 3)
        ItemStack weapon = player.getMainHandStack();
        if (!weapon.isEmpty() && weapon.isDamageable() && attackDamage >= PARRY_DAMAGE_THRESHOLD) {
            int durabilityDamage = (int) Math.ceil(attackDamage);
            weapon.damage(durabilityDamage, (ServerWorld) player.getEntityWorld(), player, item -> {
                // Weapon broke from parrying
                player.sendMessage(Text.literal("Your weapon broke!"), false);
            });
        }

        // Send success message
        player.sendMessage(Text.literal("Parried!"), true);
    }

    /**
     * Called when a parry fails.
     */
    private static void onParryFail(ServerPlayerEntity player) {
        // Could play a different sound or show message
        // For now, just log it
        Aerb.LOGGER.debug("Parry failed for {}", player.getName().getString());
    }

    /**
     * Clean up state for a player (called on disconnect).
     */
    public static void removePlayer(UUID playerId) {
        attackStates.remove(playerId);
    }

    /**
     * Internal class to track attack state.
     */
    private static class AttackState {
        final long attackTime;
        final ItemStack weapon;

        AttackState(long attackTime, ItemStack weapon) {
            this.attackTime = attackTime;
            this.weapon = weapon;
        }
    }

    /**
     * Result of a parry attempt.
     */
    public record ParryResult(boolean success, boolean skillUnlocked, int playerTotal, int enemyTotal) {}
}
