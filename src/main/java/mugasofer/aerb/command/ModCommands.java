package mugasofer.aerb.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import mugasofer.aerb.item.ModItems;
import mugasofer.aerb.item.VirtueItem;
import mugasofer.aerb.network.ModNetworking;
import mugasofer.aerb.skill.PlayerSkills;
import mugasofer.aerb.spell.SpellInventory;
import mugasofer.aerb.virtue.VirtueInventory;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public class ModCommands {
    // Map of spell names to items for commands
    private static final Map<String, Item> SPELLS = new HashMap<>();
    // Map of virtue names to items for commands
    private static final Map<String, Item> VIRTUES = new HashMap<>();

    static {
        SPELLS.put("aardes_touch", ModItems.AARDES_TOUCH);
        SPELLS.put("crimson_fist", ModItems.CRIMSON_FIST);
        SPELLS.put("sanguine_surge", ModItems.SANGUINE_SURGE);
        SPELLS.put("physical_tapping", ModItems.PHYSICAL_TAPPING);
        SPELLS.put("power_tapping", ModItems.POWER_TAPPING);
        SPELLS.put("speed_tapping", ModItems.SPEED_TAPPING);
        SPELLS.put("endurance_tapping", ModItems.ENDURANCE_TAPPING);

        VIRTUES.put("hypertension", ModItems.HYPERTENSION);
    }
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // /setskill <skill> <level> - set your own skill
            // /setskill <player> <skill> <level> - set another player's skill
            dispatcher.register(CommandManager.literal("setskill")
                // Self version: /setskill <skill> <level>
                .then(CommandManager.argument("skill", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        builder.suggest(PlayerSkills.BLOOD_MAGIC);
                        builder.suggest(PlayerSkills.BONE_MAGIC);
                        return builder.buildFuture();
                    })
                    .then(CommandManager.argument("level", IntegerArgumentType.integer(-1, 100))
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity player = source.getPlayerOrThrow();
                            String skill = StringArgumentType.getString(context, "skill");
                            int level = IntegerArgumentType.getInteger(context, "level");
                            return setSkill(source, player, skill, level);
                        })
                    )
                )
                // Target version: /setskill <player> <skill> <level>
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("skill2", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest(PlayerSkills.BLOOD_MAGIC);
                            builder.suggest(PlayerSkills.BONE_MAGIC);
                            return builder.buildFuture();
                        })
                        .then(CommandManager.argument("level2", IntegerArgumentType.integer(-1, 100))
                            .executes(context -> {
                                ServerCommandSource source = context.getSource();
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                                String skill = StringArgumentType.getString(context, "skill2");
                                int level = IntegerArgumentType.getInteger(context, "level2");
                                return setSkill(source, target, skill, level);
                            })
                        )
                    )
                )
            );

            // /getskill <skill>
            dispatcher.register(CommandManager.literal("getskill")
                .then(CommandManager.argument("skill", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        builder.suggest(PlayerSkills.BLOOD_MAGIC);
                        builder.suggest(PlayerSkills.BONE_MAGIC);
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        ServerPlayerEntity player = source.getPlayerOrThrow();
                        String skill = StringArgumentType.getString(context, "skill");

                        PlayerSkills skills = player.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
                        int level = skills.getSkillLevel(skill);

                        source.sendFeedback(() -> Text.literal(skill + " is at level " + level), false);
                        return level;
                    })
                )
            );

            // /givespell <spell> - give yourself a spell
            // /givespell <player> <spell> - give a player a spell
            dispatcher.register(CommandManager.literal("givespell")
                .then(CommandManager.argument("spell", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        SPELLS.keySet().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        ServerPlayerEntity player = source.getPlayerOrThrow();
                        String spellName = StringArgumentType.getString(context, "spell");
                        return giveSpell(source, player, spellName);
                    })
                )
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("spell2", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            SPELLS.keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                            String spellName = StringArgumentType.getString(context, "spell2");
                            return giveSpell(source, target, spellName);
                        })
                    )
                )
            );

            // /takespell <spell> - remove a spell from yourself
            // /takespell <player> <spell> - remove a spell from a player
            dispatcher.register(CommandManager.literal("takespell")
                .then(CommandManager.argument("spell", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        SPELLS.keySet().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        ServerPlayerEntity player = source.getPlayerOrThrow();
                        String spellName = StringArgumentType.getString(context, "spell");
                        return takeSpell(source, player, spellName);
                    })
                )
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("spell2", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            SPELLS.keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                            String spellName = StringArgumentType.getString(context, "spell2");
                            return takeSpell(source, target, spellName);
                        })
                    )
                )
            );

            // /givevirtue <virtue> - give yourself a virtue
            // /givevirtue <player> <virtue> - give a player a virtue
            dispatcher.register(CommandManager.literal("givevirtue")
                .then(CommandManager.argument("virtue", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        VIRTUES.keySet().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        ServerPlayerEntity player = source.getPlayerOrThrow();
                        String virtueName = StringArgumentType.getString(context, "virtue");
                        return giveVirtue(source, player, virtueName);
                    })
                )
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("virtue2", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            VIRTUES.keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                            String virtueName = StringArgumentType.getString(context, "virtue2");
                            return giveVirtue(source, target, virtueName);
                        })
                    )
                )
            );

            // /takevirtue <virtue> - remove a virtue from yourself
            // /takevirtue <player> <virtue> - remove a virtue from a player
            dispatcher.register(CommandManager.literal("takevirtue")
                .then(CommandManager.argument("virtue", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        VIRTUES.keySet().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        ServerPlayerEntity player = source.getPlayerOrThrow();
                        String virtueName = StringArgumentType.getString(context, "virtue");
                        return takeVirtue(source, player, virtueName);
                    })
                )
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("virtue2", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            VIRTUES.keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                            String virtueName = StringArgumentType.getString(context, "virtue2");
                            return takeVirtue(source, target, virtueName);
                        })
                    )
                )
            );
        });
    }

    private static int setSkill(ServerCommandSource source, ServerPlayerEntity target, String skill, int level) {
        PlayerSkills skills = target.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
        skills.setSkillLevel(skill, level);
        ModNetworking.syncSkillsToClient(target);

        // Check for spell and virtue unlocks
        checkSpellUnlocks(target, skill, level);
        checkVirtueUnlocks(target, skill, level);

        source.sendFeedback(() -> Text.literal("Set " + target.getName().getString() + "'s " + skill + " to level " + level), true);
        return 1;
    }

    /**
     * Grant spells when skills reach certain thresholds.
     */
    private static void checkSpellUnlocks(ServerPlayerEntity player, String skill, int level) {
        // Blood Magic unlocks
        if (skill.equals(PlayerSkills.BLOOD_MAGIC)) {
            if (level >= 0) grantSpellIfMissing(player, ModItems.AARDES_TOUCH, "Aarde's Touch");
            if (level >= 2) grantSpellIfMissing(player, ModItems.CRIMSON_FIST, "Crimson Fist");
            if (level >= 5) grantSpellIfMissing(player, ModItems.SANGUINE_SURGE, "Sanguine Surge");
        }

        // Bone Magic unlocks
        if (skill.equals(PlayerSkills.BONE_MAGIC)) {
            if (level >= 0) grantSpellIfMissing(player, ModItems.PHYSICAL_TAPPING, "Physical Tapping");
            if (level >= 10) {
                grantSpellIfMissing(player, ModItems.POWER_TAPPING, "Power Tapping");
                grantSpellIfMissing(player, ModItems.SPEED_TAPPING, "Speed Tapping");
                grantSpellIfMissing(player, ModItems.ENDURANCE_TAPPING, "Endurance Tapping");
            }
        }
    }

    /**
     * Add a spell to the player's spell inventory if they don't already have it.
     */
    private static void grantSpellIfMissing(ServerPlayerEntity player, Item spell, String spellName) {
        SpellInventory spellInv = player.getAttachedOrCreate(SpellInventory.ATTACHMENT);
        String spellId = net.minecraft.registry.Registries.ITEM.getId(spell).toString();

        // Check if player already has this spell
        for (int i = 0; i < spellInv.size(); i++) {
            if (spellInv.getStack(i).isOf(spell)) {
                return; // Already has it
            }
        }

        // Also check hotbar and offhand
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(spell)) {
                return; // Already has it in hotbar
            }
        }
        if (player.getOffHandStack().isOf(spell)) {
            return; // Already has it in offhand
        }

        // Find empty slot in spell inventory and add the spell
        for (int i = 0; i < SpellInventory.MAIN_SLOTS; i++) {
            if (spellInv.getStack(i).isEmpty()) {
                spellInv.setStack(i, new ItemStack(spell));
                sendDiscoveryMessage(player, spellId, spellName);
                return;
            }
        }

        // If spell inventory is full, try to give directly
        if (!player.giveItemStack(new ItemStack(spell))) {
            player.sendMessage(Text.literal("No room for " + spellName + "!"), false);
        } else {
            sendDiscoveryMessage(player, spellId, spellName);
        }
    }

    /**
     * Grant virtues when skills reach certain thresholds.
     */
    private static void checkVirtueUnlocks(ServerPlayerEntity player, String skill, int level) {
        // Blood Magic virtue unlocks
        if (skill.equals(PlayerSkills.BLOOD_MAGIC)) {
            if (level >= 20) grantVirtueIfMissing(player, ModItems.HYPERTENSION, "Hypertension");
        }
    }

    /**
     * Add a virtue to the player's virtue inventory if they don't already have it.
     */
    private static void grantVirtueIfMissing(ServerPlayerEntity player, Item virtue, String virtueName) {
        VirtueInventory virtueInv = player.getAttachedOrCreate(VirtueInventory.ATTACHMENT);
        String virtueId = net.minecraft.registry.Registries.ITEM.getId(virtue).toString();
        boolean isPassive = virtue instanceof VirtueItem v && v.isPassive();

        // Check if player already has this virtue in virtue inventory
        for (int i = 0; i < virtueInv.size(); i++) {
            if (virtueInv.getStack(i).isOf(virtue)) {
                return; // Already has it
            }
        }

        // For non-passive virtues, also check hotbar and offhand
        if (!isPassive) {
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).isOf(virtue)) {
                    return; // Already has it in hotbar
                }
            }
            if (player.getOffHandStack().isOf(virtue)) {
                return; // Already has it in offhand
            }
        }

        // Find empty slot in virtue inventory and add the virtue
        for (int i = 0; i < VirtueInventory.TOTAL_SLOTS; i++) {
            if (virtueInv.getStack(i).isEmpty()) {
                virtueInv.setStack(i, new ItemStack(virtue));
                sendVirtueDiscoveryMessage(player, virtueId, virtueName);
                return;
            }
        }

        // If virtue inventory is full
        if (isPassive) {
            // Passive virtues can only go in virtue inventory
            player.sendMessage(Text.literal("No room for " + virtueName + "!"), false);
        } else {
            // Non-passive virtues can overflow to hotbar/inventory
            if (!player.giveItemStack(new ItemStack(virtue))) {
                player.sendMessage(Text.literal("No room for " + virtueName + "!"), false);
            } else {
                sendVirtueDiscoveryMessage(player, virtueId, virtueName);
            }
        }
    }

    /**
     * Send discovery message if this is a new spell discovery.
     */
    public static void sendDiscoveryMessage(ServerPlayerEntity player, String spellId, String spellName) {
        PlayerSkills skills = player.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
        if (skills.discoverSpell(spellId)) {
            player.sendMessage(Text.literal("Spell discovered: " + spellName + "!"), false);
        }
    }

    /**
     * Send discovery message for virtues.
     */
    public static void sendVirtueDiscoveryMessage(ServerPlayerEntity player, String virtueId, String virtueName) {
        PlayerSkills skills = player.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
        if (skills.discoverSpell(virtueId)) { // Reuse spell discovery tracking for virtues
            player.sendMessage(Text.literal("New Virtue: " + virtueName + "!"), false);
        }
    }

    /**
     * Give a spell to a player via command.
     */
    private static int giveSpell(ServerCommandSource source, ServerPlayerEntity target, String spellName) {
        Item spell = SPELLS.get(spellName);
        if (spell == null) {
            source.sendError(Text.literal("Unknown spell: " + spellName));
            return 0;
        }

        SpellInventory spellInv = target.getAttachedOrCreate(SpellInventory.ATTACHMENT);
        String spellId = net.minecraft.registry.Registries.ITEM.getId(spell).toString();

        // Find empty slot in spell inventory
        for (int i = 0; i < SpellInventory.MAIN_SLOTS; i++) {
            if (spellInv.getStack(i).isEmpty()) {
                spellInv.setStack(i, new ItemStack(spell));
                sendDiscoveryMessage(target, spellId, spellName);
                source.sendFeedback(() -> Text.literal("Gave " + spellName + " to " + target.getName().getString()), true);
                return 1;
            }
        }

        // Try hotbar if spell inventory is full
        if (target.giveItemStack(new ItemStack(spell))) {
            sendDiscoveryMessage(target, spellId, spellName);
            source.sendFeedback(() -> Text.literal("Gave " + spellName + " to " + target.getName().getString()), true);
            return 1;
        }

        source.sendError(Text.literal(target.getName().getString() + " has no room for " + spellName));
        return 0;
    }

    /**
     * Remove a spell from a player via command. Also removes from discovered list.
     */
    private static int takeSpell(ServerCommandSource source, ServerPlayerEntity target, String spellName) {
        Item spell = SPELLS.get(spellName);
        if (spell == null) {
            source.sendError(Text.literal("Unknown spell: " + spellName));
            return 0;
        }

        SpellInventory spellInv = target.getAttachedOrCreate(SpellInventory.ATTACHMENT);
        String spellId = net.minecraft.registry.Registries.ITEM.getId(spell).toString();

        // Check spell inventory
        for (int i = 0; i < spellInv.size(); i++) {
            if (spellInv.getStack(i).isOf(spell)) {
                spellInv.setStack(i, ItemStack.EMPTY);
                target.getAttachedOrCreate(PlayerSkills.ATTACHMENT).forgetSpell(spellId);
                source.sendFeedback(() -> Text.literal("Took " + spellName + " from " + target.getName().getString()), true);
                return 1;
            }
        }

        // Check hotbar
        for (int i = 0; i < 9; i++) {
            if (target.getInventory().getStack(i).isOf(spell)) {
                target.getInventory().setStack(i, ItemStack.EMPTY);
                target.getAttachedOrCreate(PlayerSkills.ATTACHMENT).forgetSpell(spellId);
                source.sendFeedback(() -> Text.literal("Took " + spellName + " from " + target.getName().getString()), true);
                return 1;
            }
        }

        // Check offhand
        if (target.getOffHandStack().isOf(spell)) {
            target.getInventory().setStack(40, ItemStack.EMPTY);
            target.getAttachedOrCreate(PlayerSkills.ATTACHMENT).forgetSpell(spellId);
            source.sendFeedback(() -> Text.literal("Took " + spellName + " from " + target.getName().getString()), true);
            return 1;
        }

        source.sendError(Text.literal(target.getName().getString() + " doesn't have " + spellName));
        return 0;
    }

    /**
     * Give a virtue to a player via command.
     */
    private static int giveVirtue(ServerCommandSource source, ServerPlayerEntity target, String virtueName) {
        Item virtue = VIRTUES.get(virtueName);
        if (virtue == null) {
            source.sendError(Text.literal("Unknown virtue: " + virtueName));
            return 0;
        }

        VirtueInventory virtueInv = target.getAttachedOrCreate(VirtueInventory.ATTACHMENT);
        String virtueId = net.minecraft.registry.Registries.ITEM.getId(virtue).toString();

        // Find empty slot in virtue inventory
        for (int i = 0; i < VirtueInventory.TOTAL_SLOTS; i++) {
            if (virtueInv.getStack(i).isEmpty()) {
                virtueInv.setStack(i, new ItemStack(virtue));
                sendVirtueDiscoveryMessage(target, virtueId, virtueName);
                source.sendFeedback(() -> Text.literal("Gave " + virtueName + " to " + target.getName().getString()), true);
                return 1;
            }
        }

        // Try hotbar if virtue inventory is full
        if (target.giveItemStack(new ItemStack(virtue))) {
            sendVirtueDiscoveryMessage(target, virtueId, virtueName);
            source.sendFeedback(() -> Text.literal("Gave " + virtueName + " to " + target.getName().getString()), true);
            return 1;
        }

        source.sendError(Text.literal(target.getName().getString() + " has no room for " + virtueName));
        return 0;
    }

    /**
     * Remove a virtue from a player via command. Also removes from discovered list.
     */
    private static int takeVirtue(ServerCommandSource source, ServerPlayerEntity target, String virtueName) {
        Item virtue = VIRTUES.get(virtueName);
        if (virtue == null) {
            source.sendError(Text.literal("Unknown virtue: " + virtueName));
            return 0;
        }

        VirtueInventory virtueInv = target.getAttachedOrCreate(VirtueInventory.ATTACHMENT);
        String virtueId = net.minecraft.registry.Registries.ITEM.getId(virtue).toString();

        // Check virtue inventory
        for (int i = 0; i < virtueInv.size(); i++) {
            if (virtueInv.getStack(i).isOf(virtue)) {
                virtueInv.setStack(i, ItemStack.EMPTY);
                target.getAttachedOrCreate(PlayerSkills.ATTACHMENT).forgetSpell(virtueId);
                source.sendFeedback(() -> Text.literal("Took " + virtueName + " from " + target.getName().getString()), true);
                return 1;
            }
        }

        // Check hotbar
        for (int i = 0; i < 9; i++) {
            if (target.getInventory().getStack(i).isOf(virtue)) {
                target.getInventory().setStack(i, ItemStack.EMPTY);
                target.getAttachedOrCreate(PlayerSkills.ATTACHMENT).forgetSpell(virtueId);
                source.sendFeedback(() -> Text.literal("Took " + virtueName + " from " + target.getName().getString()), true);
                return 1;
            }
        }

        // Check offhand
        if (target.getOffHandStack().isOf(virtue)) {
            target.getInventory().setStack(40, ItemStack.EMPTY);
            target.getAttachedOrCreate(PlayerSkills.ATTACHMENT).forgetSpell(virtueId);
            source.sendFeedback(() -> Text.literal("Took " + virtueName + " from " + target.getName().getString()), true);
            return 1;
        }

        source.sendError(Text.literal(target.getName().getString() + " doesn't have " + virtueName));
        return 0;
    }
}
