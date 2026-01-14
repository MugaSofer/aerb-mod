package mugasofer.aerb;

import mugasofer.aerb.screen.CharacterSheetScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AerbClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Add character sheet button to inventory screen
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof InventoryScreen inventoryScreen) {
				ButtonWidget charSheetButton = ButtonWidget.builder(Text.literal("Stats"), button -> {
					client.setScreen(new CharacterSheetScreen(client.player));
				}).dimensions(screen.width / 2 + 100, screen.height / 2 - 80, 40, 20).build();

				Screens.getButtons(inventoryScreen).add(charSheetButton);
			}
		});
	}
}