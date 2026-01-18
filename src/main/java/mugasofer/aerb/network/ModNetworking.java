package mugasofer.aerb.network;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.screen.SpellSlotsScreenHandler;
import mugasofer.aerb.screen.VirtuesScreenHandler;
import mugasofer.aerb.skill.PlayerSkills;
import mugasofer.aerb.spell.SpellInventory;
import mugasofer.aerb.virtue.VirtueInventory;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class ModNetworking {
    // Packet IDs
    public static final Identifier OPEN_SPELL_INVENTORY_ID = Identifier.of(Aerb.MOD_ID, "open_spell_inventory");
    public static final Identifier OPEN_VIRTUE_INVENTORY_ID = Identifier.of(Aerb.MOD_ID, "open_virtue_inventory");
    public static final Identifier SYNC_SKILLS_ID = Identifier.of(Aerb.MOD_ID, "sync_skills");
    public static final Identifier SET_SELECTED_SLOT_ID = Identifier.of(Aerb.MOD_ID, "set_selected_slot");

    // Custom payload for opening spell inventory (empty payload, just a signal)
    public record OpenSpellInventoryPayload() implements CustomPayload {
        public static final Id<OpenSpellInventoryPayload> ID = new Id<>(OPEN_SPELL_INVENTORY_ID);
        public static final PacketCodec<RegistryByteBuf, OpenSpellInventoryPayload> CODEC = PacketCodec.unit(new OpenSpellInventoryPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // Custom payload for opening virtue inventory (empty payload, just a signal)
    public record OpenVirtueInventoryPayload() implements CustomPayload {
        public static final Id<OpenVirtueInventoryPayload> ID = new Id<>(OPEN_VIRTUE_INVENTORY_ID);
        public static final PacketCodec<RegistryByteBuf, OpenVirtueInventoryPayload> CODEC = PacketCodec.unit(new OpenVirtueInventoryPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // Payload for syncing skills from server to client
    public record SyncSkillsPayload(int bloodMagic, int boneMagic, int oneHanded, int parry) implements CustomPayload {
        public static final Id<SyncSkillsPayload> ID = new Id<>(SYNC_SKILLS_ID);
        public static final PacketCodec<RegistryByteBuf, SyncSkillsPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.bloodMagic);
                buf.writeInt(value.boneMagic);
                buf.writeInt(value.oneHanded);
                buf.writeInt(value.parry);
            },
            buf -> new SyncSkillsPayload(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // Payload for setting client's selected hotbar slot (server to client)
    // If swingAfter is true, client will also swing the weapon immediately
    public record SetSelectedSlotPayload(int slot, boolean swingAfter) implements CustomPayload {
        public static final Id<SetSelectedSlotPayload> ID = new Id<>(SET_SELECTED_SLOT_ID);
        public static final PacketCodec<RegistryByteBuf, SetSelectedSlotPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.slot);
                buf.writeBoolean(value.swingAfter);
            },
            buf -> new SetSelectedSlotPayload(buf.readInt(), buf.readBoolean())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Tell the client to change their selected hotbar slot.
     */
    public static void setSelectedSlot(ServerPlayerEntity player, int slot) {
        ServerPlayNetworking.send(player, new SetSelectedSlotPayload(slot, false));
    }

    /**
     * Tell the client to change their selected hotbar slot and swing immediately.
     */
    public static void setSelectedSlotAndSwing(ServerPlayerEntity player, int slot) {
        ServerPlayNetworking.send(player, new SetSelectedSlotPayload(slot, true));
    }

    /**
     * Send skill data to a player's client.
     */
    public static void syncSkillsToClient(ServerPlayerEntity player) {
        PlayerSkills skills = player.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
        SyncSkillsPayload payload = new SyncSkillsPayload(
            skills.getSkillLevel(PlayerSkills.BLOOD_MAGIC),
            skills.getSkillLevel(PlayerSkills.BONE_MAGIC),
            skills.getSkillLevel(PlayerSkills.ONE_HANDED),
            skills.getSkillLevel(PlayerSkills.PARRY)
        );
        ServerPlayNetworking.send(player, payload);
    }

    public static void init() {
        // Register payload types
        PayloadTypeRegistry.playC2S().register(OpenSpellInventoryPayload.ID, OpenSpellInventoryPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OpenVirtueInventoryPayload.ID, OpenVirtueInventoryPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncSkillsPayload.ID, SyncSkillsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SetSelectedSlotPayload.ID, SetSelectedSlotPayload.CODEC);

        // Register server-side handler for spell inventory
        ServerPlayNetworking.registerGlobalReceiver(OpenSpellInventoryPayload.ID, (payload, context) -> {
            // Run on server thread
            context.server().execute(() -> {
                var player = context.player();
                SpellInventory spellInventory = player.getAttachedOrCreate(SpellInventory.ATTACHMENT);

                player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, p) -> new SpellSlotsScreenHandler(syncId, playerInventory, spellInventory),
                    Text.literal("Spell Inventory")
                ));
            });
        });

        // Register server-side handler for virtue inventory
        ServerPlayNetworking.registerGlobalReceiver(OpenVirtueInventoryPayload.ID, (payload, context) -> {
            // Run on server thread
            context.server().execute(() -> {
                var player = context.player();
                VirtueInventory virtueInventory = player.getAttachedOrCreate(VirtueInventory.ATTACHMENT);

                player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, p) -> new VirtuesScreenHandler(syncId, playerInventory, virtueInventory),
                    Text.literal("Virtue Inventory")
                ));
            });
        });

        Aerb.LOGGER.info("Registered networking for " + Aerb.MOD_ID);
    }
}
