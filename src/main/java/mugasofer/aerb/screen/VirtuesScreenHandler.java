package mugasofer.aerb.screen;

import mugasofer.aerb.virtue.VirtueInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class VirtuesScreenHandler extends ScreenHandler {
    private final Inventory virtueInventory;

    // Client constructor
    public VirtuesScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new VirtueInventory());
    }

    // Server constructor
    public VirtuesScreenHandler(int syncId, PlayerInventory playerInventory, Inventory virtueInventory) {
        super(ModScreenHandlers.VIRTUES_SCREEN_HANDLER, syncId);
        this.virtueInventory = virtueInventory;
        virtueInventory.onOpen(playerInventory.player);

        // Virtue inventory slots (27 slots, 3 rows of 9) - only accept virtues
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new VirtueSlot(virtueInventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Player hotbar only (slots 0-8) - no main inventory
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 76));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            // Slots 0-26 are virtue inventory, 27-35 are hotbar
            if (slotIndex < 27) {
                // From virtue inventory -> try to move to hotbar
                if (!this.insertItem(originalStack, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From hotbar -> try to move to virtue inventory
                if (!this.insertItem(originalStack, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.virtueInventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.virtueInventory.onClose(player);
    }
}
