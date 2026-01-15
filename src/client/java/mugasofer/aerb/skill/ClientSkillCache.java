package mugasofer.aerb.skill;

/**
 * Client-side cache for player skills, synced from server.
 */
public class ClientSkillCache {
    private static int bloodMagic = 0;
    private static int boneMagic = 0;

    public static int getBloodMagic() {
        return bloodMagic;
    }

    public static int getBoneMagic() {
        return boneMagic;
    }

    public static void update(int bloodMagic, int boneMagic) {
        ClientSkillCache.bloodMagic = bloodMagic;
        ClientSkillCache.boneMagic = boneMagic;
    }

    public static void reset() {
        bloodMagic = 0;
        boneMagic = 0;
    }
}
