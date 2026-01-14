package mugasofer.aerb.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.CandleCakeBlock;
import net.minecraft.block.LightBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.HashMap;
import java.util.UUID;

public class AardesTouchItem extends Item {
    private static final int LIGHT_LEVEL = 14; // Slightly less than max (15) for torch-like feel
    private static final int FREEZE_INTERVAL = 30; // Add 1 frozen tick every 30 game ticks (slower than powder snow)
    private static final HashMap<UUID, BlockPos> playerLightPositions = new HashMap<>();
    private static boolean eventRegistered = false;

    public AardesTouchItem(Settings settings) {
        super(settings);
        registerTickEvent();
    }

    private static void registerTickEvent() {
        if (eventRegistered) return;
        eventRegistered = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ItemStack mainHand = player.getMainHandStack();
                boolean holdingTouch = mainHand.getItem() instanceof AardesTouchItem;
                UUID playerId = player.getUuid();
                BlockPos currentPos = player.getBlockPos();
                ServerWorld world = (ServerWorld) player.getEntityWorld();

                if (world == null) continue;

                if (holdingTouch) {
                    // === Dynamic Light ===
                    BlockPos oldPos = playerLightPositions.get(playerId);

                    // Only update if position changed or no light placed yet
                    if (oldPos == null || !oldPos.equals(currentPos)) {
                        // Remove old light block
                        if (oldPos != null) {
                            removeLightBlock(world, oldPos);
                        }

                        // Place new light block if the space is air
                        if (world.getBlockState(currentPos).isAir()) {
                            world.setBlockState(currentPos,
                                Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, LIGHT_LEVEL));
                            playerLightPositions.put(playerId, currentPos);
                        } else {
                            playerLightPositions.remove(playerId);
                        }
                    }

                    // === Cold/Freeze Effect ===
                    // Set "in powder snow" for the visual frost overlay
                    player.setInPowderSnow(true);

                    // Slowly freeze the player (if they can freeze - leather armor protects)
                    if (player.canFreeze() && server.getTicks() % FREEZE_INTERVAL == 0) {
                        int currentFrozen = player.getFrozenTicks();
                        int maxFrozen = player.getMinFreezeDamageTicks();
                        player.setFrozenTicks(Math.min(maxFrozen, currentFrozen + 1));
                    }
                } else {
                    // Not holding - remove any existing light
                    BlockPos oldPos = playerLightPositions.remove(playerId);
                    if (oldPos != null) {
                        removeLightBlock(world, oldPos);
                    }
                }
            }
        });
    }

    private static void removeLightBlock(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isOf(Blocks.LIGHT)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        PlayerEntity player = context.getPlayer();

        // Try to light campfires, candles, etc.
        if (CampfireBlock.canBeLit(state) || CandleBlock.canBeLit(state) || CandleCakeBlock.canBeLit(state)) {
            if (!world.isClient()) {
                world.setBlockState(pos, state.with(Properties.LIT, true));
                world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            playFireSound(world, pos);
            return ActionResult.SUCCESS;
        }

        // Try to place fire on the clicked face
        BlockPos firePos = pos.offset(context.getSide());
        if (AbstractFireBlock.canPlaceAt(world, firePos, context.getHorizontalPlayerFacing())) {
            if (!world.isClient()) {
                BlockState fireState = AbstractFireBlock.getState(world, firePos);
                world.setBlockState(firePos, fireState);
                world.emitGameEvent(player, GameEvent.BLOCK_PLACE, firePos);
            }
            playFireSound(world, firePos);
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }

    private void playFireSound(World world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0f, world.getRandom().nextFloat() * 0.4f + 0.8f);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
