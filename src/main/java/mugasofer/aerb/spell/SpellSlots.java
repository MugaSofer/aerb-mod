package mugasofer.aerb.spell;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import mugasofer.aerb.Aerb;

import java.util.ArrayList;
import java.util.List;

public class SpellSlots {
    public static final int MAX_SLOTS = 3;

    public static final AttachmentType<SpellSlots> ATTACHMENT = AttachmentRegistry.<SpellSlots>builder()
        .persistent(new com.mojang.serialization.Codec<SpellSlots>() {
            @Override
            public <T> com.mojang.serialization.DataResult<com.mojang.datafixers.util.Pair<SpellSlots, T>> decode(
                    com.mojang.serialization.DynamicOps<T> ops, T input) {
                // Simple decode - just create empty slots for now
                return com.mojang.serialization.DataResult.success(
                    com.mojang.datafixers.util.Pair.of(new SpellSlots(), input));
            }

            @Override
            public <T> com.mojang.serialization.DataResult<T> encode(
                    SpellSlots slots, com.mojang.serialization.DynamicOps<T> ops, T prefix) {
                // Simple encode
                return com.mojang.serialization.DataResult.success(prefix);
            }
        })
        .initializer(SpellSlots::new)
        .buildAndRegister(Identifier.of(Aerb.MOD_ID, "spell_slots"));

    private final List<ItemStack> slots;

    public SpellSlots() {
        this.slots = new ArrayList<>(MAX_SLOTS);
        for (int i = 0; i < MAX_SLOTS; i++) {
            slots.add(ItemStack.EMPTY);
        }
    }

    public ItemStack getSlot(int index) {
        if (index < 0 || index >= MAX_SLOTS) return ItemStack.EMPTY;
        return slots.get(index);
    }

    public void setSlot(int index, ItemStack stack) {
        if (index >= 0 && index < MAX_SLOTS) {
            slots.set(index, stack.copy());
        }
    }

    public boolean isEmpty(int index) {
        return getSlot(index).isEmpty();
    }

    public List<ItemStack> getAllSlots() {
        return new ArrayList<>(slots);
    }

    public static void init() {
        // Force class loading to register the attachment
    }
}
