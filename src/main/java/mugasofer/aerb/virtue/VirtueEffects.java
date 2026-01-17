package mugasofer.aerb.virtue;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.item.ModItems;
import mugasofer.aerb.item.VirtueItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Handles passive effects from virtues.
 */
public class VirtueEffects {
    // Modifier ID for Hypertension's max health bonus
    private static final Identifier HYPERTENSION_MODIFIER_ID = Identifier.of(Aerb.MOD_ID, "hypertension_health");

    public static void init() {
        // Check virtue effects every tick for all players
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                updateHypertension(player);
            }
        });

        Aerb.LOGGER.info("Registered virtue effects for " + Aerb.MOD_ID);
    }

    /**
     * Check if player has Hypertension and apply/remove the HP modifier.
     */
    private static void updateHypertension(ServerPlayerEntity player) {
        boolean hasHypertension = playerHasVirtue(player, ModItems.HYPERTENSION);
        EntityAttributeInstance healthAttribute = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);

        if (healthAttribute == null) return;

        boolean hasModifier = healthAttribute.getModifier(HYPERTENSION_MODIFIER_ID) != null;

        if (hasHypertension && !hasModifier) {
            // Add modifier: +20 max health (doubles from 20 to 40)
            healthAttribute.addPersistentModifier(new EntityAttributeModifier(
                HYPERTENSION_MODIFIER_ID,
                20.0, // Add 20 HP (10 hearts)
                EntityAttributeModifier.Operation.ADD_VALUE
            ));
        } else if (!hasHypertension && hasModifier) {
            // Remove modifier
            healthAttribute.removeModifier(HYPERTENSION_MODIFIER_ID);

            // Clamp current health to new max if needed
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    /**
     * Check if player has a specific virtue item in virtue inventory, hotbar, or offhand.
     */
    private static boolean playerHasVirtue(ServerPlayerEntity player, net.minecraft.item.Item virtueItem) {
        // Check virtue inventory
        VirtueInventory virtueInv = player.getAttachedOrCreate(VirtueInventory.ATTACHMENT);
        for (int i = 0; i < virtueInv.size(); i++) {
            if (virtueInv.getStack(i).isOf(virtueItem)) {
                return true;
            }
        }

        // Check hotbar (slots 0-8)
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(virtueItem)) {
                return true;
            }
        }

        // Check offhand
        if (player.getOffHandStack().isOf(virtueItem)) {
            return true;
        }

        return false;
    }
}
