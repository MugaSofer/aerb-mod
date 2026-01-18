package mugasofer.aerb.stat;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Server-side stat calculations for AERB.
 * Mirrors the client-side calculations in CharacterSheetScreen.
 */
public class StatCalculator {
    private static final int BASE_POW = 2;
    private static final int BASE_SPD = 2;
    private static final int BASE_END = 2;

    /**
     * Calculate SPD stat for a player.
     * SPD = BASE + Speed effect levels + Haste effect levels
     */
    public static int calculateSPD(PlayerEntity player) {
        int spd = BASE_SPD;
        if (player.hasStatusEffect(StatusEffects.SPEED)) {
            spd += player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1;
        }
        if (player.hasStatusEffect(StatusEffects.HASTE)) {
            spd += player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1;
        }
        return spd;
    }

    /**
     * Calculate POW stat for a player.
     * POW = BASE + Strength effect levels + Jump Boost effect levels
     */
    public static int calculatePOW(PlayerEntity player) {
        int pow = BASE_POW;
        if (player.hasStatusEffect(StatusEffects.STRENGTH)) {
            pow += player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() + 1;
        }
        if (player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            pow += player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1;
        }
        return pow;
    }

    /**
     * Calculate END stat for a player.
     * END = BASE + Regeneration effect levels + Resistance effect levels
     */
    public static int calculateEND(PlayerEntity player) {
        int end = BASE_END;
        if (player.hasStatusEffect(StatusEffects.REGENERATION)) {
            end += player.getStatusEffect(StatusEffects.REGENERATION).getAmplifier() + 1;
        }
        if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
            end += player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1;
        }
        return end;
    }

    /**
     * Calculate PHY stat for a player.
     * PHY = min(POW, SPD, END) + 1
     */
    public static int calculatePHY(PlayerEntity player) {
        int pow = calculatePOW(player);
        int spd = calculateSPD(player);
        int end = calculateEND(player);
        return Math.min(Math.min(pow, spd), end) + 1;
    }
}
