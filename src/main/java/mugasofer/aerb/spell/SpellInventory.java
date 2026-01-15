package mugasofer.aerb.spell;

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
 * A spell inventory that works like a second player inventory.
 * 27 main slots + 1 offhand slot = 28 total slots.
 */
public class SpellInventory implements Inventory {
    public static final int MAIN_SLOTS = 27;
    public static final int OFFHAND_SLOT = 27; // Index of offhand slot
    public static final int TOTAL_SLOTS = 28;

    // Codec that serializes the list of ItemStacks
    public static final Codec<SpellInventory> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(inv -> new ArrayList<>(inv.items))
        ).apply(instance, SpellInventory::fromList)
    );

    public static final AttachmentType<SpellInventory> ATTACHMENT = AttachmentRegistry.<SpellInventory>builder()
        .persistent(CODEC)
        .initializer(SpellInventory::new)
        .buildAndRegister(Identifier.of(Aerb.MOD_ID, "spell_inventory"));

    private final DefaultedList<ItemStack> items;

    public SpellInventory() {
        this.items = DefaultedList.ofSize(TOTAL_SLOTS, ItemStack.EMPTY);
    }

    // Constructor for deserialization
    private static SpellInventory fromList(List<ItemStack> itemList) {
        SpellInventory inv = new SpellInventory();
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

    public ItemStack getOffhandSpell() {
        return getStack(OFFHAND_SLOT);
    }

    public void setOffhandSpell(ItemStack stack) {
        setStack(OFFHAND_SLOT, stack);
    }

    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    public static void init() {
        // Force class loading to register the attachment
    }
}
