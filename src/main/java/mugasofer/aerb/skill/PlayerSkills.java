package mugasofer.aerb.skill;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;
import mugasofer.aerb.Aerb;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores player skill levels. Skills start at 0 and can be leveled up.
 */
public class PlayerSkills {
    // Known skill names
    public static final String BLOOD_MAGIC = "blood_magic";
    public static final String BONE_MAGIC = "bone_magic";

    public static final Codec<PlayerSkills> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("skills").forGetter(ps -> ps.skills)
        ).apply(instance, PlayerSkills::new)
    );

    public static final AttachmentType<PlayerSkills> ATTACHMENT = AttachmentRegistry.<PlayerSkills>builder()
        .persistent(CODEC)
        .initializer(PlayerSkills::new)
        .buildAndRegister(Identifier.of(Aerb.MOD_ID, "player_skills"));

    private final Map<String, Integer> skills;

    public PlayerSkills() {
        this.skills = new HashMap<>();
    }

    // Constructor for deserialization
    private PlayerSkills(Map<String, Integer> skills) {
        this.skills = new HashMap<>(skills);
    }

    /**
     * Get the level of a skill. Returns 0 if not set.
     */
    public int getSkillLevel(String skillName) {
        return skills.getOrDefault(skillName, 0);
    }

    /**
     * Set the level of a skill.
     */
    public void setSkillLevel(String skillName, int level) {
        skills.put(skillName, Math.max(0, level));
    }

    /**
     * Add levels to a skill.
     */
    public void addSkillLevel(String skillName, int amount) {
        int current = getSkillLevel(skillName);
        setSkillLevel(skillName, current + amount);
    }

    /**
     * Get all skills as a map.
     */
    public Map<String, Integer> getAllSkills() {
        return new HashMap<>(skills);
    }

    public static void init() {
        // Force class loading to register the attachment
    }
}
