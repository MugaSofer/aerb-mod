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

public class ModCommands {
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
        // Blood Magic 0 → Aarde's Touch
        if (skill.equals(PlayerSkills.BLOOD_MAGIC) && level >= 0) {
            grantSpellIfMissing(player, ModItems.AARDES_TOUCH, "Aarde's Touch");
        }

        // Bone Magic 0 → Physical Tapping
        if (skill.equals(PlayerSkills.BONE_MAGIC) && level >= 0) {
            grantSpellIfMissing(player, ModItems.PHYSICAL_TAPPING, "Physical Tapping");
        }
    }

    /**
     * Add a spell to the player's spell inventory if they don't already have it.
     */
    private static void grantSpellIfMissing(ServerPlayerEntity player, Item spell, String spellName) {
        SpellInventory spellInv = player.getAttachedOrCreate(SpellInventory.ATTACHMENT);

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
                player.sendMessage(Text.literal("Learned: " + spellName), false);
                return;
            }
        }

        // If spell inventory is full, try to give directly
        if (!player.giveItemStack(new ItemStack(spell))) {
            player.sendMessage(Text.literal("No room for " + spellName + "!"), false);
        } else {
            player.sendMessage(Text.literal("Learned: " + spellName), false);
        }
    }
}
