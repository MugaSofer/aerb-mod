package mugasofer.aerb.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Tattoo needle - a tool used to apply tattoos.
 * Right-click to open the tattoo application screen.
 * Has durability that decreases with each tattoo application.
 */
public class TattooNeedleItem extends Item implements DescribedItem {
    // Client-side callback set by AerbClient to open the tattoo screen
    public static Runnable openScreenCallback = null;

    public TattooNeedleItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient() && openScreenCallback != null) {
            openScreenCallback.run();
        }
        return ActionResult.SUCCESS;
    }

    /**
     * Damage the needle after a successful tattoo application.
     * Called from the server when a tattoo is applied.
     */
    public static void damageNeedle(ItemStack stack, PlayerEntity player) {
        stack.damage(1, player);
    }
}
