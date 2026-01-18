package mugasofer.aerb.skill;

/**
 * Client-side cache for player skills, synced from server.
 */
public class ClientSkillCache {
    private static int bloodMagic = -1;
    private static int boneMagic = -1;
    private static int oneHanded = -1;
    private static int parry = -1;

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

    public static void update(int bloodMagic, int boneMagic, int oneHanded, int parry) {
        ClientSkillCache.bloodMagic = bloodMagic;
        ClientSkillCache.boneMagic = boneMagic;
        ClientSkillCache.oneHanded = oneHanded;
        ClientSkillCache.parry = parry;
    }

    public static void reset() {
        bloodMagic = -1;
        boneMagic = -1;
        oneHanded = -1;
        parry = -1;
    }
}
