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
 * Stores player skill levels, XP, and discovered spells/items.
 * Skills start at -1 (locked) and can be unlocked/leveled up.
 * A skill at -1 is locked (greyed out in UI). At 0+ the skill is unlocked.
 */
public class PlayerSkills {
    public static final int LOCKED = -1;

    // Known skill names
    public static final String BLOOD_MAGIC = "blood_magic";
    public static final String BONE_MAGIC = "bone_magic";
    public static final String ONE_HANDED = "one_handed";
    public static final String PARRY = "parry";

    public static final Codec<PlayerSkills> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("skills").forGetter(ps -> ps.skills),
            Codec.STRING.listOf().fieldOf("discovered_spells").orElse(new ArrayList<>()).forGetter(ps -> new ArrayList<>(ps.discoveredSpells)),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("skill_xp").orElse(new HashMap<>()).forGetter(ps -> ps.skillXp),
            Codec.STRING.listOf().fieldOf("discovered_items").orElse(new ArrayList<>()).forGetter(ps -> new ArrayList<>(ps.discoveredItems))
        ).apply(instance, PlayerSkills::new)
    );

    public static final AttachmentType<PlayerSkills> ATTACHMENT = AttachmentRegistry.<PlayerSkills>builder()
        .persistent(CODEC)
        .initializer(PlayerSkills::new)
        .buildAndRegister(Identifier.of(Aerb.MOD_ID, "player_skills"));

    private final Map<String, Integer> skills;
    private final Set<String> discoveredSpells;
    private final Map<String, Integer> skillXp;
    private final Set<String> discoveredItems;

    public PlayerSkills() {
        this.skills = new HashMap<>();
        this.discoveredSpells = new HashSet<>();
        this.skillXp = new HashMap<>();
        this.discoveredItems = new HashSet<>();
    }

    // Constructor for deserialization
    private PlayerSkills(Map<String, Integer> skills, List<String> discoveredSpells,
                         Map<String, Integer> skillXp, List<String> discoveredItems) {
        this.skills = new HashMap<>(skills);
        this.discoveredSpells = new HashSet<>(discoveredSpells);
        this.skillXp = new HashMap<>(skillXp);
        this.discoveredItems = new HashSet<>(discoveredItems);
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

    // ============ XP Methods ============

    /**
     * Get the current XP for a skill. Returns 0 if not set.
     */
    public int getSkillXp(String skillName) {
        return skillXp.getOrDefault(skillName, 0);
    }

    /**
     * Set the XP for a skill.
     */
    public void setSkillXp(String skillName, int xp) {
        skillXp.put(skillName, Math.max(0, xp));
    }

    /**
     * Add XP to a skill. Returns the new total.
     */
    public int addSkillXp(String skillName, int amount) {
        int current = getSkillXp(skillName);
        int newXp = current + amount;
        setSkillXp(skillName, newXp);
        return newXp;
    }

    /**
     * Get all skill XP as a map.
     */
    public Map<String, Integer> getAllSkillXp() {
        return new HashMap<>(skillXp);
    }

    // ============ Discovered Items Methods ============

    /**
     * Check if an item has been discovered before (for first-equip XP).
     */
    public boolean hasDiscoveredItem(String itemId) {
        return discoveredItems.contains(itemId);
    }

    /**
     * Mark an item as discovered. Returns true if this is a new discovery.
     */
    public boolean discoverItem(String itemId) {
        return discoveredItems.add(itemId);
    }

    /**
     * Copy all skills, XP, and discovered spells/items from another PlayerSkills instance.
     * Used for preserving data on death/respawn.
     */
    public void copyFrom(PlayerSkills other) {
        this.skills.clear();
        this.skills.putAll(other.skills);
        this.discoveredSpells.clear();
        this.discoveredSpells.addAll(other.discoveredSpells);
        this.skillXp.clear();
        this.skillXp.putAll(other.skillXp);
        this.discoveredItems.clear();
        this.discoveredItems.addAll(other.discoveredItems);
    }

    public static void init() {
        // Force class loading to register the attachment
    }
}
