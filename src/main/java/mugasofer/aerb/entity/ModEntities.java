package mugasofer.aerb.entity;

import mugasofer.aerb.Aerb;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<ClaretSpearEntity> CLARET_SPEAR = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(Aerb.MOD_ID, "claret_spear"),
        EntityType.Builder.<ClaretSpearEntity>create(ClaretSpearEntity::new, SpawnGroup.MISC)
            .dimensions(0.5f, 0.5f)
            .maxTrackingRange(4)
            .trackingTickInterval(20)
            .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(Aerb.MOD_ID, "claret_spear")))
    );

    public static void initialize() {
        Aerb.LOGGER.info("Registering entities for " + Aerb.MOD_ID);
    }
}
