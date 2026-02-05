package mugasofer.aerb;

import mugasofer.aerb.command.ModCommands;
import mugasofer.aerb.config.DescriptionConfig;
import mugasofer.aerb.config.HypertensionConfig;
import mugasofer.aerb.config.XpConfig;
import mugasofer.aerb.entity.LesserUmbralUndeadEntity;
import mugasofer.aerb.entity.ModEntities;
import mugasofer.aerb.event.UmbralFormationHandler;
import mugasofer.aerb.item.ModItems;
import mugasofer.aerb.sound.ModSounds;
import mugasofer.aerb.item.SpellItem;
import mugasofer.aerb.item.VirtueItem;
import mugasofer.aerb.network.ModNetworking;
import mugasofer.aerb.screen.ModScreenHandlers;
import mugasofer.aerb.skill.PlayerSkills;
import mugasofer.aerb.spell.SpellInventory;
import mugasofer.aerb.tattoo.FallRuneHandler;
import mugasofer.aerb.tattoo.PlayerTattoos;
import mugasofer.aerb.virtue.VirtueEffects;
import mugasofer.aerb.virtue.VirtueInventory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.ZombieEntity;
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
		HypertensionConfig.load();
		DescriptionConfig.load();
		XpConfig.load();
		ModSounds.init();
		ModEntities.initialize();
		FabricDefaultAttributeRegistry.register(ModEntities.UNDEAD, ZombieEntity.createZombieAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.LESSER_UMBRAL_UNDEAD, LesserUmbralUndeadEntity.createAttributes());
		UmbralFormationHandler.init();
		ModItems.initialize();
		SpellInventory.init();
		VirtueInventory.init();
		VirtueEffects.init();
		PlayerSkills.init();
		PlayerTattoos.init();
		FallRuneHandler.init();
		ModScreenHandlers.init();
		ModNetworking.init();
		ModCommands.init();

		// Sync skills and tattoos to client when player joins
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			ModNetworking.syncSkillsToClient(player);
			ModNetworking.syncTattoosToClient(player);
		});

		// Preserve spell/virtue inventories and skills on death/respawn
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			// Copy spell inventory
			SpellInventory oldSpellInv = oldPlayer.getAttachedOrCreate(SpellInventory.ATTACHMENT);
			SpellInventory newSpellInv = newPlayer.getAttachedOrCreate(SpellInventory.ATTACHMENT);
			for (int i = 0; i < oldSpellInv.size(); i++) {
				newSpellInv.setStack(i, oldSpellInv.getStack(i).copy());
			}

			// Copy virtue inventory
			VirtueInventory oldVirtueInv = oldPlayer.getAttachedOrCreate(VirtueInventory.ATTACHMENT);
			VirtueInventory newVirtueInv = newPlayer.getAttachedOrCreate(VirtueInventory.ATTACHMENT);
			for (int i = 0; i < oldVirtueInv.size(); i++) {
				newVirtueInv.setStack(i, oldVirtueInv.getStack(i).copy());
			}

			// Copy skills (including discovered spells)
			PlayerSkills oldSkills = oldPlayer.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
			PlayerSkills newSkills = newPlayer.getAttachedOrCreate(PlayerSkills.ATTACHMENT);
			newSkills.copyFrom(oldSkills);

			// Copy tattoos
			PlayerTattoos oldTattoos = oldPlayer.getAttachedOrCreate(PlayerTattoos.ATTACHMENT);
			PlayerTattoos newTattoos = newPlayer.getAttachedOrCreate(PlayerTattoos.ATTACHMENT);
			newTattoos.copyFrom(oldTattoos);

			// Also preserve any spells/virtues that were in hotbar/offhand
			for (int i = 0; i < 9; i++) {
				ItemStack stack = oldPlayer.getInventory().getStack(i);
				if (SpellItem.isSpell(stack) || VirtueItem.isVirtue(stack)) {
					newPlayer.getInventory().setStack(i, stack.copy());
				}
			}
			ItemStack offhand = oldPlayer.getOffHandStack();
			if (SpellItem.isSpell(offhand) || VirtueItem.isVirtue(offhand)) {
				newPlayer.setStackInHand(net.minecraft.util.Hand.OFF_HAND, offhand.copy());
			}
		});

		// Prevent spell/virtue items from being dropped - return them to nearest player
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof ItemEntity itemEntity) {
				ItemStack stack = itemEntity.getStack();
				boolean isSpell = SpellItem.isSpell(stack);
				boolean isVirtue = VirtueItem.isVirtue(stack);

				if (isSpell || isVirtue) {
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

						// Try to put in appropriate special inventory first
						boolean placed = false;
						if (isSpell) {
							SpellInventory spellInv = closest.getAttachedOrCreate(SpellInventory.ATTACHMENT);
							for (int i = 0; i < spellInv.size(); i++) {
								if (spellInv.getStack(i).isEmpty()) {
									spellInv.setStack(i, stack.copy());
									placed = true;
									break;
								}
							}
						} else if (isVirtue) {
							VirtueInventory virtueInv = closest.getAttachedOrCreate(VirtueInventory.ATTACHMENT);
							for (int i = 0; i < virtueInv.size(); i++) {
								if (virtueInv.getStack(i).isEmpty()) {
									virtueInv.setStack(i, stack.copy());
									placed = true;
									break;
								}
							}
						}

						// If special inventory is full, try hotbar (only for non-passive)
						if (!placed && !VirtueItem.isPassiveVirtue(stack)) {
							closest.giveItemStack(stack.copy());
						}
					}

					// Remove the dropped item entity
					itemEntity.discard();
				}
			}
		});
	}
}