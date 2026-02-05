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
 * Tattoos are identified by their Identifier (e.g., "aerb:fall_rune").
 */
public class PlayerTattoos {

    // Known tattoo IDs
    public static final String FALL_RUNE = "fall_rune";
    public static final String ICY_DEVIL = "icy_devil";

    public static final Codec<PlayerTattoos> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, TattooState.CODEC)
                            .fieldOf("tattoos")
                            .forGetter(pt -> pt.tattoos)
            ).apply(instance, PlayerTattoos::new)
    );

    public static final AttachmentType<PlayerTattoos> ATTACHMENT = AttachmentRegistry.<PlayerTattoos>builder()
            .persistent(CODEC)
            .initializer(PlayerTattoos::new)
            .buildAndRegister(Identifier.of(Aerb.MOD_ID, "player_tattoos"));

    private final Map<String, TattooState> tattoos;

    public PlayerTattoos() {
        this.tattoos = new HashMap<>();
    }

    // Constructor for deserialization
    private PlayerTattoos(Map<String, TattooState> tattoos) {
        this.tattoos = new HashMap<>(tattoos);
    }

    /**
     * Check if the player has a specific tattoo (with charges remaining).
     */
    public boolean hasTattoo(String tattooId) {
        TattooState state = tattoos.get(tattooId);
        return state != null && state.hasCharges();
    }

    /**
     * Get the state of a tattoo, or null if not present.
     */
    public TattooState getTattoo(String tattooId) {
        return tattoos.get(tattooId);
    }

    /**
     * Add a tattoo to the player.
     * If they already have it, adds charges (for single/multi-use tattoos).
     */
    public void addTattoo(String tattooId, TattooState state) {
        TattooState existing = tattoos.get(tattooId);
        if (existing != null && state.charges() != TattooState.UNLIMITED) {
            // Stack charges if not unlimited
            int newCharges = existing.charges() == TattooState.UNLIMITED
                    ? TattooState.UNLIMITED
                    : existing.charges() + state.charges();
            tattoos.put(tattooId, new TattooState(newCharges, existing.cooldownUntil(), existing.position()));
        } else {
            tattoos.put(tattooId, state);
        }
    }

    /**
     * Add a single-use tattoo.
     */
    public void addSingleUseTattoo(String tattooId) {
        addTattoo(tattooId, TattooState.singleUse());
    }

    /**
     * Remove a tattoo completely.
     */
    public void removeTattoo(String tattooId) {
        tattoos.remove(tattooId);
    }

    /**
     * Use a charge of a tattoo. Returns true if successful.
     * Removes the tattoo if it runs out of charges.
     */
    public boolean useCharge(String tattooId) {
        TattooState state = tattoos.get(tattooId);
        if (state == null || !state.hasCharges()) {
            return false;
        }

        TattooState newState = state.useCharge();
        if (newState.charges() == TattooState.DEPLETED) {
            tattoos.remove(tattooId);
        } else {
            tattoos.put(tattooId, newState);
        }
        return true;
    }

    /**
     * Set cooldown on a tattoo.
     */
    public void setCooldown(String tattooId, long until) {
        TattooState state = tattoos.get(tattooId);
        if (state != null) {
            tattoos.put(tattooId, state.withCooldown(until));
        }
    }

    /**
     * Move a tattoo to a new body position.
     */
    public void moveTattoo(String tattooId, BodyPosition newPosition) {
        TattooState state = tattoos.get(tattooId);
        if (state != null) {
            tattoos.put(tattooId, state.movedTo(newPosition));
        }
    }

    /**
     * Get all tattoos as a map (for syncing/display).
     */
    public Map<String, TattooState> getAllTattoos() {
        return new HashMap<>(tattoos);
    }

    /**
     * Get set of all tattoo IDs the player has (with charges).
     */
    public Set<String> getActiveTattooIds() {
        Set<String> active = new HashSet<>();
        for (Map.Entry<String, TattooState> entry : tattoos.entrySet()) {
            if (entry.getValue().hasCharges()) {
                active.add(entry.getKey());
            }
        }
        return active;
    }

    /**
     * Copy all tattoos from another instance.
     * Used for preserving data on death/respawn.
     */
    public void copyFrom(PlayerTattoos other) {
        this.tattoos.clear();
        this.tattoos.putAll(other.tattoos);
    }

    /**
     * Force class loading to register the attachment.
     */
    public static void init() {
        // Called from mod initializer
    }
}
