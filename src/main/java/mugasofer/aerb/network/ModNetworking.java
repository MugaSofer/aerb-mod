package mugasofer.aerb.network;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.screen.SpellSlotsScreenHandler;
import mugasofer.aerb.skill.PlayerSkills;
import mugasofer.aerb.spell.SpellInventory;
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
    // Packet ID for opening spell inventory
    public static final Identifier OPEN_SPELL_INVENTORY_ID = Identifier.of(Aerb.MOD_ID, "open_spell_inventory");
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
        PayloadTypeRegistry.playS2C().register(SyncSkillsPayload.ID, SyncSkillsPayload.CODEC);

        // Register server-side handler
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

        Aerb.LOGGER.info("Registered networking for " + Aerb.MOD_ID);
    }
}
