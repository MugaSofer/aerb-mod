package mugasofer.aerb.skill;

/**
 * Client-side cache for player skills, synced from server.
 */
public class ClientSkillCache {
    // Skill levels (-1 = not synced yet)
    private static int bloodMagic = -1;
    private static int boneMagic = -1;
    private static int oneHanded = -1;
    private static int parry = -1;

    // Skill XP
    private static int bloodMagicXp = 0;
    private static int boneMagicXp = 0;
    private static int oneHandedXp = 0;
    private static int parryXp = 0;

    public static int getBloodMagic() {
        return bloodMagic;
    }

    public static int getBoneMagic() {
        return boneMagic;
    }

    public static int getOneHanded() {
        return oneHanded;
    }

    public static int getParry() {
        return parry;
    }

    public static int getBloodMagicXp() {
        return bloodMagicXp;
    }

    public static int getBoneMagicXp() {
        return boneMagicXp;
    }

    public static int getOneHandedXp() {
        return oneHandedXp;
    }

    public static int getParryXp() {
        return parryXp;
    }

    public static void update(int bloodMagic, int boneMagic, int oneHanded, int parry,
                              int bloodMagicXp, int boneMagicXp, int oneHandedXp, int parryXp) {
        ClientSkillCache.bloodMagic = bloodMagic;
        ClientSkillCache.boneMagic = boneMagic;
        ClientSkillCache.oneHanded = oneHanded;
        ClientSkillCache.parry = parry;
        ClientSkillCache.bloodMagicXp = bloodMagicXp;
        ClientSkillCache.boneMagicXp = boneMagicXp;
        ClientSkillCache.oneHandedXp = oneHandedXp;
        ClientSkillCache.parryXp = parryXp;
    }

    public static void reset() {
        bloodMagic = -1;
        boneMagic = -1;
        oneHanded = -1;
        parry = -1;
        bloodMagicXp = 0;
        boneMagicXp = 0;
        oneHandedXp = 0;
        parryXp = 0;
    }
}
