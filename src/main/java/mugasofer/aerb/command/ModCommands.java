package mugasofer.aerb.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import mugasofer.aerb.item.ModItems;
import mugasofer.aerb.network.ModNetworking;
import mugasofer.aerb.skill.PlayerSkills;
import mugasofer.aerb.spell.SpellInventory;
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

    static {
        SPELLS.put("aardes_touch", ModItems.AARDES_TOUCH);
        SPELLS.put("crimson_fist", ModItems.CRIMSON_FIST);
        SPELLS.put("sanguine_surge", ModItems.SANGUINE_SURGE);
        SPELLS.put("physical_tapping", ModItems.PHYSICAL_TAPPING);
        SPELLS.put("power_tapping", ModItems.POWER_TAPPING);
        SPELLS.put("speed_tapping", ModItems.SPEED_TAPPING);
        SPELLS.put("endurance_tapping", ModItems.ENDURANCE_TAPPING);
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
        });
    }

    private static int setSkill(ServerCommandSource source, ServerPlayerEntity target, String skill, int level) {
        PlayerSkills skills = target.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
        skills.setSkillLevel(skill, level);
        ModNetworking.syncSkillsToClient(target);

        // Check for spell unlocks
        checkSpellUnlocks(target, skill, level);

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
     * Send discovery message if this is a new spell discovery.
     */
    public static void sendDiscoveryMessage(ServerPlayerEntity player, String spellId, String spellName) {
        PlayerSkills skills = player.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
        if (skills.discoverSpell(spellId)) {
            player.sendMessage(Text.literal("Spell discovered: " + spellName + "!"), false);
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
     * Remove a spell from a player via command.
     */
    private static int takeSpell(ServerCommandSource source, ServerPlayerEntity target, String spellName) {
        Item spell = SPELLS.get(spellName);
        if (spell == null) {
            source.sendError(Text.literal("Unknown spell: " + spellName));
            return 0;
        }

        SpellInventory spellInv = target.getAttachedOrCreate(SpellInventory.ATTACHMENT);

        // Check spell inventory
        for (int i = 0; i < spellInv.size(); i++) {
            if (spellInv.getStack(i).isOf(spell)) {
                spellInv.setStack(i, ItemStack.EMPTY);
                source.sendFeedback(() -> Text.literal("Took " + spellName + " from " + target.getName().getString()), true);
                return 1;
            }
        }

        // Check hotbar
        for (int i = 0; i < 9; i++) {
            if (target.getInventory().getStack(i).isOf(spell)) {
                target.getInventory().setStack(i, ItemStack.EMPTY);
                source.sendFeedback(() -> Text.literal("Took " + spellName + " from " + target.getName().getString()), true);
                return 1;
            }
        }

        // Check offhand
        if (target.getOffHandStack().isOf(spell)) {
            target.getInventory().setStack(40, ItemStack.EMPTY);
            source.sendFeedback(() -> Text.literal("Took " + spellName + " from " + target.getName().getString()), true);
            return 1;
        }

        source.sendError(Text.literal(target.getName().getString() + " doesn't have " + spellName));
        return 0;
    }
}
