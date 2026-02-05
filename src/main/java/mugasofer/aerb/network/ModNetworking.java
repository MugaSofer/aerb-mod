package mugasofer.aerb.network;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.item.TattooDesignItem;
import mugasofer.aerb.item.TattooInkItem;
import mugasofer.aerb.item.TattooNeedleItem;
import mugasofer.aerb.screen.SpellSlotsScreenHandler;
import mugasofer.aerb.screen.VirtuesScreenHandler;
import mugasofer.aerb.skill.PlayerSkills;
import mugasofer.aerb.skill.XpHelper;
import mugasofer.aerb.spell.SpellInventory;
import mugasofer.aerb.tattoo.BodyPosition;
import mugasofer.aerb.tattoo.PlayerTattoos;
import mugasofer.aerb.tattoo.TattooInstance;
import mugasofer.aerb.virtue.VirtueInventory;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ModNetworking {
    // Packet IDs
    public static final Identifier OPEN_SPELL_INVENTORY_ID = Identifier.of(Aerb.MOD_ID, "open_spell_inventory");
    public static final Identifier OPEN_VIRTUE_INVENTORY_ID = Identifier.of(Aerb.MOD_ID, "open_virtue_inventory");
    public static final Identifier SYNC_SKILLS_ID = Identifier.of(Aerb.MOD_ID, "sync_skills");
    public static final Identifier SET_SELECTED_SLOT_ID = Identifier.of(Aerb.MOD_ID, "set_selected_slot");
    public static final Identifier SYNC_TATTOOS_ID = Identifier.of(Aerb.MOD_ID, "sync_tattoos");
    public static final Identifier APPLY_TATTOO_ID = Identifier.of(Aerb.MOD_ID, "apply_tattoo");

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

    // Payload for syncing skills from server to client (includes levels and XP)
    public record SyncSkillsPayload(
        int bloodMagic, int boneMagic, int oneHanded, int parry,
        int horticulture, int art, int skinMagic,
        int bloodMagicXp, int boneMagicXp, int oneHandedXp, int parryXp,
        int horticultureXp, int artXp, int skinMagicXp
    ) implements CustomPayload {
        public static final Id<SyncSkillsPayload> ID = new Id<>(SYNC_SKILLS_ID);
        public static final PacketCodec<RegistryByteBuf, SyncSkillsPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.bloodMagic);
                buf.writeInt(value.boneMagic);
                buf.writeInt(value.oneHanded);
                buf.writeInt(value.parry);
                buf.writeInt(value.horticulture);
                buf.writeInt(value.art);
                buf.writeInt(value.skinMagic);
                buf.writeInt(value.bloodMagicXp);
                buf.writeInt(value.boneMagicXp);
                buf.writeInt(value.oneHandedXp);
                buf.writeInt(value.parryXp);
                buf.writeInt(value.horticultureXp);
                buf.writeInt(value.artXp);
                buf.writeInt(value.skinMagicXp);
            },
            buf -> new SyncSkillsPayload(
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt()
            )
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

    // Payload for syncing tattoos from server to client
    public record SyncTattoosPayload(List<TattooInstance> tattoos) implements CustomPayload {
        public static final Id<SyncTattoosPayload> ID = new Id<>(SYNC_TATTOOS_ID);
        public static final PacketCodec<RegistryByteBuf, SyncTattoosPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.tattoos.size());
                for (TattooInstance instance : value.tattoos) {
                    buf.writeString(instance.tattooId());
                    buf.writeString(instance.position().name());
                    buf.writeLong(instance.cooldownUntil());
                }
            },
            buf -> {
                int count = buf.readInt();
                List<TattooInstance> tattoos = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String id = buf.readString();
                    BodyPosition position = BodyPosition.valueOf(buf.readString());
                    long cooldown = buf.readLong();
                    tattoos.add(new TattooInstance(id, position, cooldown));
                }
                return new SyncTattoosPayload(tattoos);
            }
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // Payload for applying a tattoo (client to server)
    public record ApplyTattooPayload(String tattooId, String position) implements CustomPayload {
        public static final Id<ApplyTattooPayload> ID = new Id<>(APPLY_TATTOO_ID);
        public static final PacketCodec<RegistryByteBuf, ApplyTattooPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeString(value.tattooId);
                buf.writeString(value.position);
            },
            buf -> new ApplyTattooPayload(buf.readString(), buf.readString())
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
            skills.getSkillLevel(PlayerSkills.PARRY),
            skills.getSkillLevel(PlayerSkills.HORTICULTURE),
            skills.getSkillLevel(PlayerSkills.ART),
            skills.getSkillLevel(PlayerSkills.SKIN_MAGIC),
            skills.getSkillXp(PlayerSkills.BLOOD_MAGIC),
            skills.getSkillXp(PlayerSkills.BONE_MAGIC),
            skills.getSkillXp(PlayerSkills.ONE_HANDED),
            skills.getSkillXp(PlayerSkills.PARRY),
            skills.getSkillXp(PlayerSkills.HORTICULTURE),
            skills.getSkillXp(PlayerSkills.ART),
            skills.getSkillXp(PlayerSkills.SKIN_MAGIC)
        );
        ServerPlayNetworking.send(player, payload);
    }

    /**
     * Send tattoo data to a player's client.
     */
    public static void syncTattoosToClient(ServerPlayerEntity player) {
        PlayerTattoos tattoos = player.getAttachedOrCreate(PlayerTattoos.ATTACHMENT);
        SyncTattoosPayload payload = new SyncTattoosPayload(tattoos.getAllTattoos());
        ServerPlayNetworking.send(player, payload);
    }

    public static void init() {
        // Register payload types
        PayloadTypeRegistry.playC2S().register(OpenSpellInventoryPayload.ID, OpenSpellInventoryPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OpenVirtueInventoryPayload.ID, OpenVirtueInventoryPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ApplyTattooPayload.ID, ApplyTattooPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncSkillsPayload.ID, SyncSkillsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SetSelectedSlotPayload.ID, SetSelectedSlotPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncTattoosPayload.ID, SyncTattoosPayload.CODEC);

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

        // Register server-side handler for applying tattoos
        ServerPlayNetworking.registerGlobalReceiver(ApplyTattooPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                handleApplyTattoo(player, payload.tattooId(), payload.position());
            });
        });

        Aerb.LOGGER.info("Registered networking for " + Aerb.MOD_ID);
    }

    /**
     * Handle tattoo application request from client.
     */
    private static void handleApplyTattoo(ServerPlayerEntity player, String tattooId, String positionStr) {
        // Validate needle in hand
        ItemStack mainHand = player.getMainHandStack();
        if (!(mainHand.getItem() instanceof TattooNeedleItem)) {
            player.sendMessage(Text.literal("You need to hold a tattoo needle!"), true);
            return;
        }

        // Find the design in inventory
        TattooDesignItem design = null;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof TattooDesignItem d && d.getTattooId().equals(tattooId)) {
                design = d;
                break;
            }
        }
        if (design == null) {
            player.sendMessage(Text.literal("You don't have that tattoo design!"), true);
            return;
        }

        // Find ink in inventory
        int inkSlot = -1;
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).getItem() instanceof TattooInkItem) {
                inkSlot = i;
                break;
            }
        }
        if (inkSlot < 0) {
            player.sendMessage(Text.literal("You need tattoo ink!"), true);
            return;
        }

        // Check Skin Magic skill
        PlayerSkills skills = player.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
        int skinMagicLevel = skills.getSkillLevel(PlayerSkills.SKIN_MAGIC);
        if (skinMagicLevel < 0) {
            player.sendMessage(Text.literal("You haven't learned Skin Magic!"), true);
            return;
        }
        if (skinMagicLevel < design.getRequiredSkinMagicLevel()) {
            player.sendMessage(Text.literal("Requires Skin Magic level " + design.getRequiredSkinMagicLevel() + "!"), true);
            return;
        }

        // Parse position
        BodyPosition position;
        try {
            position = BodyPosition.valueOf(positionStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.literal("Invalid body position!"), true);
            return;
        }

        // All checks passed - apply the tattoo!

        // Consume ink
        player.getInventory().getStack(inkSlot).decrement(1);

        // Damage needle
        TattooNeedleItem.damageNeedle(mainHand, player);

        // Add tattoo
        PlayerTattoos tattoos = player.getAttachedOrCreate(PlayerTattoos.ATTACHMENT);
        tattoos.addTattoo(tattooId, position);

        // Award Skin Magic XP
        XpHelper.awardXp(player, PlayerSkills.SKIN_MAGIC, 10);

        // Sync to client
        syncTattoosToClient(player);

        // Success message
        String posName = position.name().replace("_", " ").toLowerCase();
        player.sendMessage(Text.literal("Applied " + tattooId.replace("_", " ") + " to your " + posName + "!"), true);

        Aerb.LOGGER.info("{} applied tattoo {} at {}", player.getName().getString(), tattooId, position);
    }
}
