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
    private static int horticulture = -1;
    private static int art = -1;
    private static int skinMagic = -1;

    // Skill XP
    private static int bloodMagicXp = 0;
    private static int boneMagicXp = 0;
    private static int oneHandedXp = 0;
    private static int parryXp = 0;
    private static int horticultureXp = 0;
    private static int artXp = 0;
    private static int skinMagicXp = 0;

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

    public static int getHorticulture() {
        return horticulture;
    }

    public static int getArt() {
        return art;
    }

    public static int getSkinMagic() {
        return skinMagic;
    }

    public static int getHorticultureXp() {
        return horticultureXp;
    }

    public static int getArtXp() {
        return artXp;
    }

    public static int getSkinMagicXp() {
        return skinMagicXp;
    }

    public static void update(int bloodMagic, int boneMagic, int oneHanded, int parry,
                              int horticulture, int art, int skinMagic,
                              int bloodMagicXp, int boneMagicXp, int oneHandedXp, int parryXp,
                              int horticultureXp, int artXp, int skinMagicXp) {
        ClientSkillCache.bloodMagic = bloodMagic;
        ClientSkillCache.boneMagic = boneMagic;
        ClientSkillCache.oneHanded = oneHanded;
        ClientSkillCache.parry = parry;
        ClientSkillCache.horticulture = horticulture;
        ClientSkillCache.art = art;
        ClientSkillCache.skinMagic = skinMagic;
        ClientSkillCache.bloodMagicXp = bloodMagicXp;
        ClientSkillCache.boneMagicXp = boneMagicXp;
        ClientSkillCache.oneHandedXp = oneHandedXp;
        ClientSkillCache.parryXp = parryXp;
        ClientSkillCache.horticultureXp = horticultureXp;
        ClientSkillCache.artXp = artXp;
        ClientSkillCache.skinMagicXp = skinMagicXp;
    }

    public static void reset() {
        bloodMagic = -1;
        boneMagic = -1;
        oneHanded = -1;
        parry = -1;
        horticulture = -1;
        art = -1;
        skinMagic = -1;
        bloodMagicXp = 0;
        boneMagicXp = 0;
        oneHandedXp = 0;
        parryXp = 0;
        horticultureXp = 0;
        artXp = 0;
        skinMagicXp = 0;
    }
}
