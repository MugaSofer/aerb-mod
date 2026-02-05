package mugasofer.aerb;

import mugasofer.aerb.combat.ParrySkillCache;
import mugasofer.aerb.config.DescriptionConfig;
import mugasofer.aerb.entity.ModEntities;
import mugasofer.aerb.item.DescribedItem;
import mugasofer.aerb.network.ModNetworking;
import mugasofer.aerb.screen.CharacterSheetScreen;
import mugasofer.aerb.screen.ModScreenHandlers;
import mugasofer.aerb.screen.SpellSlotsScreen;
import mugasofer.aerb.screen.VirtuesScreen;
import mugasofer.aerb.skill.ClientSkillCache;
import mugasofer.aerb.tattoo.ClientTattooCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import mugasofer.aerb.render.ClaretSpearEntityRenderer;
import mugasofer.aerb.render.LesserUmbralUndeadModel;
import mugasofer.aerb.render.LesserUmbralUndeadRenderer;
import mugasofer.aerb.render.UndeadEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class AerbClient implements ClientModInitializer {
	// Model layer for Lesser Umbral Undead
	public static final EntityModelLayer LESSER_UMBRAL_UNDEAD_LAYER =
		new EntityModelLayer(Identifier.of(Aerb.MOD_ID, "lesser_umbral_undead"), "main");

	@Override
	public void onInitializeClient() {
		// Register model layers
		EntityModelLayerRegistry.registerModelLayer(LESSER_UMBRAL_UNDEAD_LAYER, LesserUmbralUndeadModel::getTexturedModelData);
		// Register screens
		HandledScreens.register(ModScreenHandlers.SPELL_SLOTS_SCREEN_HANDLER, SpellSlotsScreen::new);
		HandledScreens.register(ModScreenHandlers.VIRTUES_SCREEN_HANDLER, VirtuesScreen::new);

		// Register entity renderers
		EntityRendererRegistry.register(ModEntities.CLARET_SPEAR, ClaretSpearEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.UNDEAD, UndeadEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.LESSER_UMBRAL_UNDEAD, LesserUmbralUndeadRenderer::new);

		// Add custom descriptions to items that implement DescribedItem
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			if (DescribedItem.hasDescription(stack)) {
				// Get item ID from registry
				String itemId = Registries.ITEM.getId(stack.getItem()).getPath();
				List<String> descriptionLines = DescriptionConfig.get().getDescription(itemId);

				if (!descriptionLines.isEmpty()) {
					// Add a blank line before description if there's already content
					if (lines.size() > 1) {
						lines.add(Text.empty());
					}
					// Add each description line in gray italic
					for (String line : descriptionLines) {
						lines.add(Text.literal(line).formatted(Formatting.GRAY, Formatting.ITALIC));
					}
				}
			}
		});

		// Register client-side handler for skill sync
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SyncSkillsPayload.ID, (payload, context) -> {
			ClientSkillCache.update(
				payload.bloodMagic(), payload.boneMagic(), payload.oneHanded(), payload.parry(),
				payload.horticulture(), payload.art(), payload.skinMagic(),
				payload.bloodMagicXp(), payload.boneMagicXp(), payload.oneHandedXp(), payload.parryXp(),
				payload.horticultureXp(), payload.artXp(), payload.skinMagicXp()
			);
			// Also update the parry skill cache for animation timing
			ParrySkillCache.setParryLevel(payload.parry());
		});

		// Register client-side handler for setting selected hotbar slot (Prophetic Blade)
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SetSelectedSlotPayload.ID, (payload, context) -> {
			var client = context.client();
			var player = client.player;
			if (player != null && payload.slot() >= 0 && payload.slot() < 9) {
				// If swing requested, skip the equip animation
				if (payload.swingAfter()) {
					mugasofer.aerb.combat.ParryAnimationState.requestSkipEquipAnimation();
				}

				player.getInventory().setSelectedSlot(payload.slot());

				// If swing requested, swing immediately
				if (payload.swingAfter()) {
					player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
				}
			}
		});

		// Register client-side handler for tattoo sync
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SyncTattoosPayload.ID, (payload, context) -> {
			ClientTattooCache.update(payload.tattoos());
		});

		// Add navigation tabs to inventory screen (left side to match other screens)
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof InventoryScreen inventoryScreen) {
				// Tab dimensions - consistent across all screens
				int tabWidth = 40;
				int tabHeight = 20;
				int tabSpacing = 24;

				// Position tabs on the left side of inventory (inventory is 176 wide)
				int tabX = screen.width / 2 - 88 - tabWidth - 4; // Left of inventory panel
				int tabY = screen.height / 2 - 83;               // Top of inventory

				// Inventory tab (current - disabled)
				ButtonWidget invTab = ButtonWidget.builder(Text.literal("Inv"), button -> {
					// Already on this screen
				}).dimensions(tabX, tabY, tabWidth, tabHeight).build();
				invTab.active = false;
				Screens.getButtons(inventoryScreen).add(invTab);

				// Stats tab
				ButtonWidget statsTab = ButtonWidget.builder(Text.literal("Stats"), button -> {
					client.setScreen(new CharacterSheetScreen(client.player));
				}).dimensions(tabX, tabY + tabSpacing, tabWidth, tabHeight).build();
				Screens.getButtons(inventoryScreen).add(statsTab);

				// Spells tab - sends packet to server to open the screen
				ButtonWidget spellsTab = ButtonWidget.builder(Text.literal("Spells"), button -> {
					ClientPlayNetworking.send(new ModNetworking.OpenSpellInventoryPayload());
				}).dimensions(tabX, tabY + tabSpacing * 2, tabWidth, tabHeight).build();
				Screens.getButtons(inventoryScreen).add(spellsTab);

				// Virtues tab - sends packet to server to open the screen
				ButtonWidget virtuesTab = ButtonWidget.builder(Text.literal("Virtues"), button -> {
					ClientPlayNetworking.send(new ModNetworking.OpenVirtueInventoryPayload());
				}).dimensions(tabX, tabY + tabSpacing * 3, tabWidth, tabHeight).build();
				Screens.getButtons(inventoryScreen).add(virtuesTab);
			}
		});
	}
}