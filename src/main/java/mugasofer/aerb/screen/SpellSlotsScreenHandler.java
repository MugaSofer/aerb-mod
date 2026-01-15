package mugasofer.aerb.screen;

import mugasofer.aerb.spell.SpellInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class SpellSlotsScreenHandler extends ScreenHandler {
    private final Inventory spellInventory;

    // Client constructor
    public SpellSlotsScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SpellInventory());
    }

    // Server constructor
    public SpellSlotsScreenHandler(int syncId, PlayerInventory playerInventory, Inventory spellInventory) {
        super(ModScreenHandlers.SPELL_SLOTS_SCREEN_HANDLER, syncId);
        this.spellInventory = spellInventory;
        spellInventory.onOpen(playerInventory.player);

        // Spell inventory slots (27 slots, 3 rows of 9) - only accept spells
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new SpellSlot(spellInventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Offhand spell slot (slot 27) - centered between spell grid and hotbar
        this.addSlot(new SpellSlot(spellInventory, SpellInventory.OFFHAND_SLOT, 80, 76));

        // Player hotbar only (slots 0-8) - no main inventory
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 98));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            // Slots 0-27 are spell inventory, 28-36 are hotbar
            if (slotIndex < 28) {
                // From spell inventory -> try to move to hotbar
                if (!this.insertItem(originalStack, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From hotbar -> try to move to spell inventory
                if (!this.insertItem(originalStack, 0, 28, false)) {
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
        return this.spellInventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.spellInventory.onClose(player);
    }
}
