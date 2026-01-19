package mugasofer.aerb.entity;

import mugasofer.aerb.item.ClaretSpearItem;
import mugasofer.aerb.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

/**
 * The thrown Claret Spear projectile.
 * Returns to the player's spell inventory after hitting something or timing out.
 */
public class ClaretSpearEntity extends PersistentProjectileEntity implements FlyingItemEntity {
    private final int bloodMagicLevel;

    public ClaretSpearEntity(EntityType<? extends ClaretSpearEntity> entityType, World world) {
        super(entityType, world);
        this.bloodMagicLevel = 0;
    }

    public ClaretSpearEntity(World world, LivingEntity owner, ItemStack stack, int bloodMagicLevel) {
        super(ModEntities.CLARET_SPEAR, owner, world, stack, null);
        this.bloodMagicLevel = bloodMagicLevel;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        LivingEntity owner = (LivingEntity) this.getOwner();
        if (owner == null) {
            return;
        }

        // Calculate damage based on Blood Magic level
        float damage = ClaretSpearItem.getDamage(bloodMagicLevel);

        // Create damage source
        DamageSource damageSource = this.getDamageSources().trident(this, owner);

        // Deal damage (1.21.11 requires ServerWorld)
        World world = this.getEntityWorld();
        if (world instanceof ServerWorld serverWorld) {
            if (entityHitResult.getEntity().damage(serverWorld, damageSource, damage)) {
                this.playSound(SoundEvents.ITEM_TRIDENT_HIT, 1.0f, 1.0f);
            }
        }

        // Return to owner after hitting
        returnToOwner();
    }

    @Override
    protected void age() {
        // After 60 seconds (1200 ticks), return to owner
        if (this.age > 1200) {
            returnToOwner();
        }
    }

    @Override
    public void tick() {
        super.tick();

        // If stuck in ground for a while, return to owner
        if (this.isInGround() && this.age > 20) {
            returnToOwner();
        }
    }

    /**
     * Return the spear to the owner's spell inventory.
     */
    private void returnToOwner() {
        World world = this.getEntityWorld();
        if (world.isClient()) {
            this.discard();
            return;
        }

        LivingEntity owner = (LivingEntity) this.getOwner();
        if (owner instanceof ServerPlayerEntity player) {
            // The spear returns to spell inventory automatically since it's a SpellItem
            // Just give it back - the spell restriction system will handle placement
            ItemStack spearStack = new ItemStack(ModItems.CLARET_SPEAR);
            if (!player.giveItemStack(spearStack)) {
                // If inventory is full, drop it (spell system will return it)
                player.dropItem(spearStack, false);
            }
        }

        this.discard();
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ModItems.CLARET_SPEAR);
    }

    @Override
    public ItemStack getStack() {
        return new ItemStack(ModItems.CLARET_SPEAR);
    }

    @Override
    protected SoundEvent getHitSound() {
        return SoundEvents.ITEM_TRIDENT_HIT_GROUND;
    }
}
