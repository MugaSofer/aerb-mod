package mugasofer.aerb.item;

import mugasofer.aerb.skill.PlayerSkills;
import mugasofer.aerb.skill.XpHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * A Commoner's Guide book that unlocks a specific skill when held.
 * These are regular items (can be dropped, stored anywhere) that teach skills.
 */
public class CommonersGuideItem extends Item implements DescribedItem {
    private final String skillToUnlock;
    private static boolean eventRegistered = false;

    public CommonersGuideItem(Settings settings, String skillToUnlock) {
        super(settings);
        this.skillToUnlock = skillToUnlock;
        registerTickEvent();
    }

    private void registerTickEvent() {
        if (eventRegistered) return;
        eventRegistered = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ItemStack mainHand = player.getMainHandStack();

                // Check if holding any Commoner's Guide
                if (mainHand.getItem() instanceof CommonersGuideItem guide) {
                    String skill = guide.getSkillToUnlock();
                    PlayerSkills skills = player.getAttachedOrCreate(PlayerSkills.ATTACHMENT);

                    // Unlock the skill if not already unlocked
                    if (!skills.isUnlocked(skill)) {
                        XpHelper.awardXp(player, skill, 0); // Unlock with 0 XP
                    }
                }
            }
        });
    }

    /**
     * Get the skill this guide unlocks.
     */
    public String getSkillToUnlock() {
        return skillToUnlock;
    }
}
