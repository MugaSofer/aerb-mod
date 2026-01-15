package mugasofer.aerb.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import mugasofer.aerb.network.ModNetworking;
import mugasofer.aerb.skill.PlayerSkills;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
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
                    .then(CommandManager.argument("level", IntegerArgumentType.integer(0, 100))
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
                        .then(CommandManager.argument("level2", IntegerArgumentType.integer(0, 100))
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
        source.sendFeedback(() -> Text.literal("Set " + target.getName().getString() + "'s " + skill + " to level " + level), true);
        return 1;
    }
}
