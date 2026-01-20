package mugasofer.aerb.event;

import mugasofer.aerb.Aerb;
import mugasofer.aerb.entity.LesserUmbralUndeadEntity;
import mugasofer.aerb.entity.ModEntities;
import mugasofer.aerb.entity.UndeadEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Handles the formation of Lesser Umbral Undead when enough undead cluster together.
 * Formation is gradual - undead gather toward a central point before merging.
 */
public class UmbralFormationHandler {
    // Chunk-based cooldown to prevent immediate reformation
    private static final Map<ChunkPos, Long> formationCooldowns = new HashMap<>();

    // Active formations in progress
    private static final List<FormationInProgress> activeFormations = new ArrayList<>();

    // Constants
    public static final int FORMATION_THRESHOLD = 20; // Undead needed to form
    public static final int WATCHER_REQUIREMENT = 5; // Extra undead that must be nearby (not absorbed)
    public static final double FORMATION_RADIUS = 8.0;
    public static final long FORMATION_COOLDOWN_TICKS = 60 * 20; // 60 seconds
    private static final int CHECK_INTERVAL = 40; // Check every 2 seconds

    // Gradual formation settings - faster formation
    private static final double GATHER_SPEED = 0.12; // How fast undead move toward center
    private static final double MERGE_RADIUS = 3.0; // Distance at which undead start getting absorbed
    private static final int TICKS_BETWEEN_ABSORBS = 15; // Absorb one every 0.75 seconds
    private static final double SUCK_IN_SPEED = 0.5; // Pull speed when being absorbed
    private static final double ABSORB_DISTANCE = 0.8; // Distance at which absorbed entity disappears
    private static final int ABSORBS_BEFORE_SPAWN = 5; // How many absorbed before Umbral appears

    private static int tickCounter = 0;

    public static void init() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            tickCounter++;

            // Update active formations every tick
            long currentTime = server.getOverworld().getTime();
            updateActiveFormations(currentTime);

            if (tickCounter < CHECK_INTERVAL) {
                return;
            }
            tickCounter = 0;

            // Clean up old cooldowns
            formationCooldowns.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > FORMATION_COOLDOWN_TICKS);

            // Check each world for new formations
            for (ServerWorld world : server.getWorlds()) {
                checkFormations(world, currentTime);
            }
        });

        Aerb.LOGGER.info("Umbral formation handler initialized");
    }

    private static void updateActiveFormations(long currentTime) {
        Iterator<FormationInProgress> iter = activeFormations.iterator();
        while (iter.hasNext()) {
            FormationInProgress formation = iter.next();

            // Remove dead/removed undead from formation
            formation.members.removeIf(e -> e.isRemoved() || e.isDead());
            formation.watchers.removeIf(e -> e.isRemoved() || e.isDead());

            // Check if growing Umbral died
            if (formation.growingUmbral != null && (formation.growingUmbral.isRemoved() || formation.growingUmbral.isDead())) {
                // Umbral died during formation - cancel
                iter.remove();
                Aerb.LOGGER.info("Umbral formation cancelled - Umbral was killed");
                continue;
            }

            // Cancel if not enough watchers remain
            if (formation.watchers.size() < WATCHER_REQUIREMENT) {
                // If Umbral already spawned, let it live but stop formation
                if (formation.growingUmbral != null) {
                    iter.remove();
                    Aerb.LOGGER.info("Umbral formation ended early - watchers dispersed");
                    continue;
                }
                iter.remove();
                Aerb.LOGGER.info("Umbral formation cancelled - not enough watchers");
                continue;
            }

            // Cancel formation if not enough members left to reach threshold
            int totalAvailable = formation.absorbedCount + formation.members.size();
            if (totalAvailable < FORMATION_THRESHOLD && formation.growingUmbral == null) {
                iter.remove();
                Aerb.LOGGER.info("Umbral formation cancelled - not enough undead remaining");
                continue;
            }

            // Update centroid based on current positions
            formation.updateCentroid();

            // Make entities face center periodically
            formation.makeEntitiesFaceCenter();

            // Move members toward centroid (but not the one being sucked in)
            for (LivingEntity member : formation.members) {
                if (member == formation.beingSuckedIn) continue;

                Vec3d memberPos = new Vec3d(member.getX(), member.getY(), member.getZ());
                Vec3d toCenter = formation.centroid.subtract(memberPos);
                double dist = toCenter.length();

                if (dist > 0.5) {
                    Vec3d moveDir = toCenter.normalize().multiply(GATHER_SPEED);
                    member.setVelocity(moveDir.x, member.getVelocity().y, moveDir.z);
                }
            }

            // Handle entity currently being sucked in
            if (formation.beingSuckedIn != null) {
                LivingEntity sucking = formation.beingSuckedIn;

                if (sucking.isRemoved() || sucking.isDead()) {
                    formation.beingSuckedIn = null;
                } else {
                    // Pull toward centroid
                    Vec3d suckPos = new Vec3d(sucking.getX(), sucking.getY(), sucking.getZ());
                    Vec3d toCenter = formation.centroid.subtract(suckPos);
                    double dist = toCenter.length();

                    if (dist <= ABSORB_DISTANCE) {
                        // Absorb it
                        formation.members.remove(sucking);
                        formation.absorbedCount++;
                        sucking.discard();
                        formation.beingSuckedIn = null;
                        formation.lastAbsorbTime = currentTime;

                        // Spawn Umbral after enough absorptions
                        if (formation.absorbedCount == ABSORBS_BEFORE_SPAWN && formation.growingUmbral == null) {
                            spawnGrowingUmbral(formation);
                        }

                        // Update Umbral's corpse count as it absorbs more
                        if (formation.growingUmbral != null && !formation.growingUmbral.isRemoved()) {
                            formation.growingUmbral.setCorpseCount(
                                LesserUmbralUndeadEntity.MIN_CORPSES + formation.absorbedCount - ABSORBS_BEFORE_SPAWN
                            );
                        }

                        // Check if formation is complete
                        if (formation.absorbedCount >= FORMATION_THRESHOLD) {
                            // Formation complete - Umbral is already there
                            if (formation.growingUmbral != null) {
                                formation.growingUmbral.setCorpseCount(
                                    Math.min(formation.absorbedCount, LesserUmbralUndeadEntity.MAX_CORPSES)
                                );
                            }
                            // Absorb any remaining members
                            for (LivingEntity entity : formation.members) {
                                entity.discard();
                            }
                            iter.remove();
                            Aerb.LOGGER.info("Umbral formation complete with {} corpses", formation.absorbedCount);
                            continue;
                        }
                    } else {
                        // Pull it in
                        Vec3d pullDir = toCenter.normalize().multiply(SUCK_IN_SPEED);
                        sucking.setVelocity(pullDir.x, pullDir.y * 0.3, pullDir.z);
                    }
                }
            }
            // Pick next entity to absorb
            else if (currentTime - formation.lastAbsorbTime >= TICKS_BETWEEN_ABSORBS) {
                LivingEntity toAbsorb = null;
                double closestDist = Double.MAX_VALUE;

                for (LivingEntity member : formation.members) {
                    Vec3d memberPos = new Vec3d(member.getX(), member.getY(), member.getZ());
                    double dist = memberPos.distanceTo(formation.centroid);
                    if (dist <= MERGE_RADIUS && dist < closestDist) {
                        toAbsorb = member;
                        closestDist = dist;
                    }
                }

                if (toAbsorb != null) {
                    formation.beingSuckedIn = toAbsorb;
                }
            }
        }
    }

    private static void spawnGrowingUmbral(FormationInProgress formation) {
        LesserUmbralUndeadEntity umbral = ModEntities.LESSER_UMBRAL_UNDEAD.create(formation.world, SpawnReason.MOB_SUMMONED);
        if (umbral == null) {
            Aerb.LOGGER.error("Failed to create growing Lesser Umbral Undead!");
            return;
        }

        // Start with minimum corpse count (resets full size flag so it can grow)
        umbral.setCorpseCountForFormation(LesserUmbralUndeadEntity.MIN_CORPSES);

        // Spawn at centroid
        double spawnX = formation.centroid.x;
        double spawnY = formation.centroid.y;
        double spawnZ = formation.centroid.z;

        // Ensure above ground
        int groundY = formation.world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,
            (int) spawnX, (int) spawnZ);
        if (spawnY < groundY) {
            spawnY = groundY;
        }

        umbral.refreshPositionAndAngles(spawnX, spawnY, spawnZ, 0, 0);
        formation.world.spawnEntity(umbral);
        formation.growingUmbral = umbral;

        Aerb.LOGGER.info("Growing Umbral spawned at ({}, {}, {})", spawnX, spawnY, spawnZ);
    }

    private static void checkFormations(ServerWorld world, long currentTime) {
        // Get all formable undead entities
        List<LivingEntity> allUndead = new ArrayList<>();

        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class,
                new Box(-30000000, -64, -30000000, 30000000, 320, 30000000),
                e -> isFormableUndead(e) && !e.isRemoved() && !isInActiveFormation(e))) {
            allUndead.add(entity);
        }

        if (allUndead.size() < FORMATION_THRESHOLD) {
            return;
        }

        // Find clusters of undead
        Set<LivingEntity> processed = new HashSet<>();

        for (LivingEntity leader : allUndead) {
            if (processed.contains(leader)) {
                continue;
            }

            // Check chunk cooldown
            ChunkPos chunkPos = new ChunkPos(leader.getBlockPos());
            if (formationCooldowns.containsKey(chunkPos)) {
                continue;
            }

            // Find nearby undead within radius
            List<LivingEntity> cluster = new ArrayList<>();
            cluster.add(leader);
            processed.add(leader);

            Box searchBox = leader.getBoundingBox().expand(FORMATION_RADIUS);

            for (LivingEntity other : allUndead) {
                if (processed.contains(other)) {
                    continue;
                }

                if (searchBox.intersects(other.getBoundingBox()) ||
                    leader.squaredDistanceTo(other) <= FORMATION_RADIUS * FORMATION_RADIUS) {
                    cluster.add(other);
                    processed.add(other);
                }
            }

            // Check if cluster is large enough for formation (need threshold + watchers)
            if (cluster.size() >= FORMATION_THRESHOLD + WATCHER_REQUIREMENT) {
                startFormation(world, cluster, currentTime);
                return; // Only start one formation per check
            }
        }
    }

    private static boolean isInActiveFormation(LivingEntity entity) {
        for (FormationInProgress formation : activeFormations) {
            if (formation.members.contains(entity) || formation.watchers.contains(entity)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFormableUndead(LivingEntity entity) {
        if (entity instanceof LesserUmbralUndeadEntity) {
            return false;
        }
        if (entity instanceof UndeadEntity) {
            return true;
        }
        return false;
    }

    private static void startFormation(ServerWorld world, List<LivingEntity> cluster, long currentTime) {
        FormationInProgress formation = new FormationInProgress();
        formation.world = world;

        // Sort by distance to center to pick which become members vs watchers
        Vec3d tempCentroid = Vec3d.ZERO;
        for (LivingEntity e : cluster) {
            tempCentroid = tempCentroid.add(e.getX(), e.getY(), e.getZ());
        }
        tempCentroid = tempCentroid.multiply(1.0 / cluster.size());
        final Vec3d center = tempCentroid;

        // Sort closest to center first
        cluster.sort((a, b) -> {
            double distA = a.squaredDistanceTo(center.x, center.y, center.z);
            double distB = b.squaredDistanceTo(center.x, center.y, center.z);
            return Double.compare(distA, distB);
        });

        // Closest become members (will be absorbed), rest are watchers
        formation.members = new ArrayList<>(cluster.subList(0, FORMATION_THRESHOLD));
        formation.watchers = new ArrayList<>(cluster.subList(FORMATION_THRESHOLD, cluster.size()));

        formation.updateCentroid();
        formation.lastAbsorbTime = currentTime;
        formation.absorbedCount = 0;
        formation.growingUmbral = null;

        // Make all undead face the center
        formation.makeEntitiesFaceCenter();

        activeFormations.add(formation);

        // Set cooldown for this chunk immediately to prevent overlapping formations
        ChunkPos chunkPos = new ChunkPos((int)(formation.centroid.x) >> 4, (int)(formation.centroid.z) >> 4);
        formationCooldowns.put(chunkPos, currentTime);

        Aerb.LOGGER.info("Umbral formation started with {} members and {} watchers",
            formation.members.size(), formation.watchers.size());
    }

    /**
     * Tracks a formation in progress
     */
    private static class FormationInProgress {
        ServerWorld world;
        List<LivingEntity> members; // Undead being absorbed into formation
        List<LivingEntity> watchers; // Nearby undead watching (not absorbed)
        Vec3d centroid;
        long lastAbsorbTime;
        int absorbedCount;
        LivingEntity beingSuckedIn; // Entity currently being pulled in
        LesserUmbralUndeadEntity growingUmbral; // The Umbral spawning in the center

        void updateCentroid() {
            if (members.isEmpty() && growingUmbral == null) {
                return;
            }
            // Once Umbral spawns, centroid is its position
            if (growingUmbral != null && !growingUmbral.isRemoved()) {
                centroid = new Vec3d(growingUmbral.getX(), growingUmbral.getY(), growingUmbral.getZ());
                return;
            }
            centroid = Vec3d.ZERO;
            for (LivingEntity entity : members) {
                centroid = centroid.add(entity.getX(), entity.getY(), entity.getZ());
            }
            centroid = centroid.multiply(1.0 / members.size());
        }

        void makeEntitiesFaceCenter() {
            for (LivingEntity entity : members) {
                faceToward(entity, centroid);
            }
            for (LivingEntity entity : watchers) {
                faceToward(entity, centroid);
            }
        }

        private void faceToward(LivingEntity entity, Vec3d target) {
            double dx = target.x - entity.getX();
            double dz = target.z - entity.getZ();
            float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
            entity.setYaw(yaw);
            entity.setHeadYaw(yaw);
            entity.setBodyYaw(yaw);
        }
    }
}
