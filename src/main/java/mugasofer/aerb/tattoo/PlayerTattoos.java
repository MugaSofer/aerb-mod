package mugasofer.aerb.tattoo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mugasofer.aerb.Aerb;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Stores all tattoos a player has.
 * Players can have multiple instances of the same tattoo type at different positions.
 */
public class PlayerTattoos {

    // Known tattoo IDs
    public static final String FALL_RUNE = "fall_rune";
    public static final String ICY_DEVIL = "icy_devil";

    public static final Codec<PlayerTattoos> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.list(TattooInstance.CODEC)
                            .fieldOf("tattoos")
                            .forGetter(pt -> pt.tattoos)
            ).apply(instance, PlayerTattoos::new)
    );

    public static final AttachmentType<PlayerTattoos> ATTACHMENT = AttachmentRegistry.<PlayerTattoos>builder()
            .persistent(CODEC)
            .initializer(PlayerTattoos::new)
            .buildAndRegister(Identifier.of(Aerb.MOD_ID, "player_tattoos"));

    private final List<TattooInstance> tattoos;

    public PlayerTattoos() {
        this.tattoos = new ArrayList<>();
    }

    // Constructor for deserialization
    private PlayerTattoos(List<TattooInstance> tattoos) {
        this.tattoos = new ArrayList<>(tattoos);
    }

    /**
     * Check if the player has at least one instance of a specific tattoo.
     */
    public boolean hasTattoo(String tattooId) {
        return tattoos.stream().anyMatch(t -> t.tattooId().equals(tattooId));
    }

    /**
     * Get all instances of a specific tattoo type.
     */
    public List<TattooInstance> getTattooInstances(String tattooId) {
        return tattoos.stream()
                .filter(t -> t.tattooId().equals(tattooId))
                .toList();
    }

    /**
     * Get the first usable instance of a tattoo (not on cooldown).
     * Returns null if none available.
     */
    public TattooInstance getUsableTattoo(String tattooId, long currentTime) {
        return tattoos.stream()
                .filter(t -> t.tattooId().equals(tattooId) && t.canUse(currentTime))
                .findFirst()
                .orElse(null);
    }

    /**
     * Add a tattoo instance.
     */
    public void addTattoo(TattooInstance instance) {
        tattoos.add(instance);
    }

    /**
     * Add a tattoo at a specific position.
     */
    public void addTattoo(String tattooId, BodyPosition position) {
        tattoos.add(TattooInstance.create(tattooId, position));
    }

    /**
     * Remove a specific tattoo instance.
     * Returns true if it was found and removed.
     */
    public boolean removeTattoo(TattooInstance instance) {
        return tattoos.remove(instance);
    }

    /**
     * Remove the first instance of a tattoo type.
     * Returns the removed instance, or null if none found.
     */
    public TattooInstance removeFirstTattoo(String tattooId) {
        for (int i = 0; i < tattoos.size(); i++) {
            if (tattoos.get(i).tattooId().equals(tattooId)) {
                return tattoos.remove(i);
            }
        }
        return null;
    }

    /**
     * Remove all instances of a tattoo type.
     * Returns the number removed.
     */
    public int removeAllTattoos(String tattooId) {
        int removed = 0;
        Iterator<TattooInstance> iter = tattoos.iterator();
        while (iter.hasNext()) {
            if (iter.next().tattooId().equals(tattooId)) {
                iter.remove();
                removed++;
            }
        }
        return removed;
    }

    /**
     * Set cooldown on a specific tattoo instance.
     */
    public void setCooldown(TattooInstance instance, long until) {
        int index = tattoos.indexOf(instance);
        if (index >= 0) {
            tattoos.set(index, instance.withCooldown(until));
        }
    }

    /**
     * Move a tattoo to a new body position.
     */
    public void moveTattoo(TattooInstance instance, BodyPosition newPosition) {
        int index = tattoos.indexOf(instance);
        if (index >= 0) {
            tattoos.set(index, instance.movedTo(newPosition));
        }
    }

    /**
     * Get all tattoo instances.
     */
    public List<TattooInstance> getAllTattoos() {
        return new ArrayList<>(tattoos);
    }

    /**
     * Get the count of a specific tattoo type.
     */
    public int countTattoos(String tattooId) {
        return (int) tattoos.stream().filter(t -> t.tattooId().equals(tattooId)).count();
    }

    /**
     * Get all unique tattoo IDs the player has.
     */
    public Set<String> getTattooIds() {
        Set<String> ids = new HashSet<>();
        for (TattooInstance t : tattoos) {
            ids.add(t.tattooId());
        }
        return ids;
    }

    /**
     * Copy all tattoos from another instance.
     * Used for preserving data on death/respawn.
     */
    public void copyFrom(PlayerTattoos other) {
        this.tattoos.clear();
        this.tattoos.addAll(other.tattoos);
    }

    /**
     * Force class loading to register the attachment.
     */
    public static void init() {
        // Called from mod initializer
    }
}
