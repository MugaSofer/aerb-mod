package mugasofer.aerb.skill;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;
import mugasofer.aerb.Aerb;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * Stores player skill levels and discovered spells.
 * Skills start at -1 (locked) and can be unlocked/leveled up.
 * A skill at -1 is locked (greyed out in UI). At 0+ the skill is unlocked.
 */
public class PlayerSkills {
    public static final int LOCKED = -1;

    // Known skill names
    public static final String BLOOD_MAGIC = "blood_magic";
    public static final String BONE_MAGIC = "bone_magic";

    public static final Codec<PlayerSkills> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("skills").forGetter(ps -> ps.skills),
            Codec.STRING.listOf().fieldOf("discovered_spells").orElse(new ArrayList<>()).forGetter(ps -> new ArrayList<>(ps.discoveredSpells))
        ).apply(instance, PlayerSkills::new)
    );

    public static final AttachmentType<PlayerSkills> ATTACHMENT = AttachmentRegistry.<PlayerSkills>builder()
        .persistent(CODEC)
        .initializer(PlayerSkills::new)
        .buildAndRegister(Identifier.of(Aerb.MOD_ID, "player_skills"));

    private final Map<String, Integer> skills;
    private final Set<String> discoveredSpells;

    public PlayerSkills() {
        this.skills = new HashMap<>();
        this.discoveredSpells = new HashSet<>();
    }

    // Constructor for deserialization
    private PlayerSkills(Map<String, Integer> skills, List<String> discoveredSpells) {
        this.skills = new HashMap<>(skills);
        this.discoveredSpells = new HashSet<>(discoveredSpells);
    }

    /**
     * Get the level of a skill. Returns LOCKED (-1) if not set.
     */
    public int getSkillLevel(String skillName) {
        return skills.getOrDefault(skillName, LOCKED);
    }

    /**
     * Check if a skill is unlocked (level >= 0).
     */
    public boolean isUnlocked(String skillName) {
        return getSkillLevel(skillName) >= 0;
    }

    /**
     * Set the level of a skill. Minimum is LOCKED (-1).
     */
    public void setSkillLevel(String skillName, int level) {
        skills.put(skillName, Math.max(LOCKED, level));
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

    /**
     * Check if a spell has been discovered before.
     */
    public boolean hasDiscoveredSpell(String spellId) {
        return discoveredSpells.contains(spellId);
    }

    /**
     * Mark a spell as discovered. Returns true if this is a new discovery.
     */
    public boolean discoverSpell(String spellId) {
        return discoveredSpells.add(spellId);
    }

    /**
     * Remove a spell from discovered list (for /takespell command).
     */
    public void forgetSpell(String spellId) {
        discoveredSpells.remove(spellId);
    }

    public static void init() {
        // Force class loading to register the attachment
    }
}
