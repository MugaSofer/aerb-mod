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
    public record SyncSkillsPayload(int bloodMagic, int boneMagic) implements CustomPayload {
        public static final Id<SyncSkillsPayload> ID = new Id<>(SYNC_SKILLS_ID);
        public static final PacketCodec<RegistryByteBuf, SyncSkillsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, SyncSkillsPayload::bloodMagic,
            PacketCodecs.INTEGER, SyncSkillsPayload::boneMagic,
            SyncSkillsPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Send skill data to a player's client.
     */
    public static void syncSkillsToClient(ServerPlayerEntity player) {
        PlayerSkills skills = player.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
        SyncSkillsPayload payload = new SyncSkillsPayload(
            skills.getSkillLevel(PlayerSkills.BLOOD_MAGIC),
            skills.getSkillLevel(PlayerSkills.BONE_MAGIC)
        );
        ServerPlayNetworking.send(player, payload);
    }

    public static void init() {
        // Register payload types
        PayloadTypeRegistry.playC2S().register(OpenSpellInventoryPayload.ID, OpenSpellInventoryPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OpenVirtueInventoryPayload.ID, OpenVirtueInventoryPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncSkillsPayload.ID, SyncSkillsPayload.CODEC);

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
