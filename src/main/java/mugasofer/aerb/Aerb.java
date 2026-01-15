package mugasofer.aerb;

import mugasofer.aerb.command.ModCommands;
import mugasofer.aerb.item.ModItems;
import mugasofer.aerb.item.SpellItem;
import mugasofer.aerb.network.ModNetworking;
import mugasofer.aerb.screen.ModScreenHandlers;
import mugasofer.aerb.skill.PlayerSkills;
import mugasofer.aerb.spell.SpellInventory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Aerb implements ModInitializer {
	public static final String MOD_ID = "aerb";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");
		ModItems.initialize();
		SpellInventory.init();
		PlayerSkills.init();
		ModScreenHandlers.init();
		ModNetworking.init();
		ModCommands.init();

		// Sync skills to client when player joins
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			ModNetworking.syncSkillsToClient(player);
		});

		// Prevent spell items from being dropped - return them to nearest player
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof ItemEntity itemEntity) {
				ItemStack stack = itemEntity.getStack();
				if (SpellItem.isSpell(stack)) {
					// Find the nearest player within 10 blocks
					Box searchBox = entity.getBoundingBox().expand(10.0);
					List<PlayerEntity> nearbyPlayers = world.getEntitiesByClass(
						PlayerEntity.class, searchBox, p -> true);

					if (!nearbyPlayers.isEmpty()) {
						PlayerEntity closest = nearbyPlayers.get(0);
						double closestDist = closest.squaredDistanceTo(entity);
						for (PlayerEntity p : nearbyPlayers) {
							double dist = p.squaredDistanceTo(entity);
							if (dist < closestDist) {
								closest = p;
								closestDist = dist;
							}
						}

						// Give the item back to the player
						if (!closest.giveItemStack(stack.copy())) {
							// If inventory is full, put in spell inventory
							SpellInventory spellInv = closest.getAttachedOrCreate(SpellInventory.ATTACHMENT);
							for (int i = 0; i < spellInv.size(); i++) {
								if (spellInv.getStack(i).isEmpty()) {
									spellInv.setStack(i, stack.copy());
									break;
								}
							}
						}
					}

					// Remove the dropped item entity
					itemEntity.discard();
				}
			}
		});
	}
}