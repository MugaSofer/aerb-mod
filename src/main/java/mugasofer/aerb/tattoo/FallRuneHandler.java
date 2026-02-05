package mugasofer.aerb.tattoo;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.network.ModNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Handles the Fall Rune tattoo effect.
 *
 * The Fall Rune is a single-use tattoo that activates automatically when
 * the player reaches a dangerous falling velocity, slowing them to a safe
 * speed and preventing fall damage.
 */
public class FallRuneHandler {
    // Fall velocity threshold at which the rune CAN activate (blocks per tick)
    // Terminal velocity in Minecraft is about -3.92 blocks/tick
    // Fall damage starts at -0.4 blocks/tick (roughly 4 blocks height)
    // We require dangerous velocity to avoid wasting it on short falls
    private static final double ACTIVATION_VELOCITY = -0.5;

    // Safe velocity to slow down to (slightly slower than damage threshold)
    private static final double SAFE_VELOCITY = -0.3;

    // Duration of slow falling effect (in ticks) - 3 seconds
    private static final int SLOW_FALL_DURATION = 60;

    // Maximum distance to ground at which the rune will activate
    // This ensures it only triggers when you're about to hit, not high in the sky
    private static final int ACTIVATION_DISTANCE = 12;

    // Maximum distance to scan for ground (don't waste time checking into the void)
    private static final int MAX_SCAN_DISTANCE = 64;

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(FallRuneHandler::onWorldTick);
        Aerb.LOGGER.info("Fall Rune handler initialized");
    }

    private static void onWorldTick(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            checkFallRune(player);
        }
    }

    private static void checkFallRune(ServerPlayerEntity player) {
        // Skip if player is on ground, flying, or has slow falling already
        if (player.isOnGround() || player.getAbilities().flying ||
            player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            return;
        }

        // Check vertical velocity
        double yVelocity = player.getVelocity().y;
        if (yVelocity >= ACTIVATION_VELOCITY) {
            // Not falling fast enough to trigger
            return;
        }

        // Check if player has the Fall Rune
        PlayerTattoos tattoos = player.getAttachedOrCreate(PlayerTattoos.ATTACHMENT);
        if (!tattoos.hasTattoo(PlayerTattoos.FALL_RUNE)) {
            return;
        }

        // Check distance to ground - only activate when close to impact
        int distanceToGround = getDistanceToGround(player);
        if (distanceToGround > ACTIVATION_DISTANCE || distanceToGround < 0) {
            // Too high up or over the void - don't activate yet
            return;
        }

        // Activate the rune!
        activateFallRune(player, tattoos);
    }

    /**
     * Find the distance to the nearest solid ground below the player.
     * Returns -1 if no ground found within MAX_SCAN_DISTANCE (e.g., falling into void).
     */
    private static int getDistanceToGround(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        BlockPos playerPos = player.getBlockPos();

        // Start scanning from just below the player's feet
        for (int i = 0; i < MAX_SCAN_DISTANCE; i++) {
            BlockPos checkPos = playerPos.down(i);

            // Check if we've gone below world minimum
            if (checkPos.getY() < world.getBottomY()) {
                return -1; // Falling into void
            }

            // Check if this block would stop a fall
            if (!world.getBlockState(checkPos).isAir() &&
                world.getBlockState(checkPos).blocksMovement()) {
                return i;
            }
        }

        return -1; // No ground found within scan range
    }

    private static void activateFallRune(ServerPlayerEntity player, PlayerTattoos tattoos) {
        // Remove one Fall Rune instance (consumes the tattoo)
        TattooInstance removed = tattoos.removeFirstTattoo(PlayerTattoos.FALL_RUNE);
        if (removed == null) {
            return; // Shouldn't happen if hasTattoo returned true
        }

        // Apply slow falling effect
        player.addStatusEffect(new StatusEffectInstance(
            StatusEffects.SLOW_FALLING,
            SLOW_FALL_DURATION,
            0, // Amplifier 0 = level 1
            false, // Not ambient
            true,  // Show particles
            true   // Show icon
        ));

        // Reduce current velocity to safe level and sync to client
        player.setVelocity(
            player.getVelocity().x,
            Math.max(player.getVelocity().y, SAFE_VELOCITY),
            player.getVelocity().z
        );
        // Send the velocity update to the client
        player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket(player));

        // Notify the player
        player.sendMessage(Text.literal("Your Fall Rune glows and your descent slows!"), true);

        // Sync tattoo state to client (tattoo was consumed)
        ModNetworking.syncTattoosToClient(player);

        Aerb.LOGGER.info("Fall Rune activated for {}", player.getName().getString());
    }
}
