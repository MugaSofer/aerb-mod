package mugasofer.aerb.virtue;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import mugasofer.aerb.Aerb;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.ArrayList;

/**
 * A virtue inventory for blade-bound items.
 * 27 main slots (same as spell inventory).
 */
public class VirtueInventory implements Inventory {
    public static final int TOTAL_SLOTS = 27;

    public static final Codec<VirtueInventory> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(inv -> new ArrayList<>(inv.items))
        ).apply(instance, VirtueInventory::fromList)
    );

    public static final AttachmentType<VirtueInventory> ATTACHMENT = AttachmentRegistry.<VirtueInventory>builder()
        .persistent(CODEC)
        .initializer(VirtueInventory::new)
        .buildAndRegister(Identifier.of(Aerb.MOD_ID, "virtue_inventory"));

    private final DefaultedList<ItemStack> items;

    public VirtueInventory() {
        this.items = DefaultedList.ofSize(TOTAL_SLOTS, ItemStack.EMPTY);
    }

    private static VirtueInventory fromList(List<ItemStack> itemList) {
        VirtueInventory inv = new VirtueInventory();
        for (int i = 0; i < Math.min(itemList.size(), TOTAL_SLOTS); i++) {
            inv.items.set(i, itemList.get(i));
        }
        return inv;
    }

    @Override
    public int size() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (slot < 0 || slot >= items.size() || items.get(slot).isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = items.get(slot).split(amount);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        markDirty();
        return stack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot >= 0 && slot < items.size()) {
            items.set(slot, stack);
            markDirty();
        }
    }

    @Override
    public void markDirty() {
        // Called when inventory changes - persistence handled by attachment system
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        items.clear();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            items.add(ItemStack.EMPTY);
        }
    }

    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    public static void init() {
        // Force class loading to register the attachment
    }
}
