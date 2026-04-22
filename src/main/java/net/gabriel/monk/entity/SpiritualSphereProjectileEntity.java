package net.gabriel.monk.entity;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.gabriel.monk.effect.ModEffects;
import net.gabriel.monk.mixin.EntityIFramesAccessor;
import net.gabriel.monk.util.MonkSpiritSpheres;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.spell_engine.api.spell.fx.ParticleBatch;
import net.spell_engine.fx.ParticleHelper;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class SpiritualSphereProjectileEntity extends ThrownItemEntity {

    public enum BehaviorMode {
        OFFENSIVE,
        SPIRIT_TRANSFER,
        VISUAL_RETURN
    }

    private static final TrackedData<Integer> TRACKED_BEHAVIOR_MODE =
            DataTracker.registerData(SpiritualSphereProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final TrackedData<Integer> TRACKED_MAX_LIFE =
            DataTracker.registerData(SpiritualSphereProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final int SPHERES_DURATION_TICKS = 14 * 20;

    private int life = 0;
    private int maxLife = 60;

    private float directDamage = 3.0f;
    private float splashMultiplier = 0.7f;
    private float splashRadius = 2.0f;
    private float healAmount = 2.0f;

    private BehaviorMode behaviorMode = BehaviorMode.OFFENSIVE;
    private int spiritTransferBuffDurationTicks = 80;

    private int homingTargetId = -1;
    private int homingTicks = 0;

    private boolean visualPathInitialized = false;
    private double visualStartX;
    private double visualStartY;
    private double visualStartZ;

    private float visualArcHeight = 1.0f;
    private float visualLateralArc = 0.0f;
    private float visualSpiralRadius = 0.5f;
    private float visualSpiralTurns = 2.0f;
    private float visualSpiralPhase = 0.0f;
    private int visualSideSign = 1;

    private double visualArrivalOffsetX = 0.0;
    private double visualArrivalOffsetY = 0.0;
    private double visualArrivalOffsetZ = 0.0;

    private static final int HOMING_MAX_TICKS = 14;
    private static final int HOMING_DELAY_TICKS = 2;
    private static final float HOMING_TURN_DEG_PER_TICK = 8.0f;

    private static final Vector3f ARCANE_PURPLE = new Vector3f(0.55f, 0.18f, 0.92f);
    private static final Vector3f ARCANE_LILAC = new Vector3f(0.92f, 0.75f, 1.00f);
    private static final Vector3f ARCANE_WHITE = new Vector3f(0.98f, 0.96f, 1.00f);

    private static final ParticleEffect TRAIL_PARTICLE =
            new DustParticleEffect(new Vector3f(ARCANE_PURPLE), 0.85f);

    private static final ParticleEffect IMPACT_PARTICLE =
            new DustColorTransitionParticleEffect(
                    new Vector3f(ARCANE_PURPLE),
                    new Vector3f(ARCANE_LILAC),
                    1.55f
            );

    private static final long ARCANE_PURPLE_RGBA = 2974679039L;
    private static final long ARCANE_WHITE_RGBA = 4294967295L;

    private static final float AURA_415_COUNT = 2.0f;
    private static final float AURA_415_BASE_SCALE = 1.6f;
    private static final float SPLASH_SCALE_MUL = 0.8f;
    private static final float AURA_415_MAX_AGE = 1.4f;
    private static final float AURA_415_EXTENT = 0.85f;

    public SpiritualSphereProjectileEntity(EntityType<? extends ThrownItemEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
    }

    public SpiritualSphereProjectileEntity(EntityType<? extends ThrownItemEntity> type, LivingEntity owner, World world) {
        super(type, owner, world);
        this.setNoGravity(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(TRACKED_BEHAVIOR_MODE, BehaviorMode.OFFENSIVE.ordinal());
        builder.add(TRACKED_MAX_LIFE, 60);
    }

    private void syncLocalStateFromTracker() {
        int rawMode = this.dataTracker.get(TRACKED_BEHAVIOR_MODE);
        if (rawMode >= 0 && rawMode < BehaviorMode.values().length) {
            this.behaviorMode = BehaviorMode.values()[rawMode];
        } else {
            this.behaviorMode = BehaviorMode.OFFENSIVE;
        }

        this.maxLife = this.dataTracker.get(TRACKED_MAX_LIFE);
        if (this.maxLife <= 0) {
            this.maxLife = 60;
        }
    }

    private void setTrackedBehaviorMode(BehaviorMode mode) {
        this.behaviorMode = mode;
        this.dataTracker.set(TRACKED_BEHAVIOR_MODE, mode.ordinal());
    }

    private void setTrackedMaxLife(int maxLife) {
        this.maxLife = maxLife;
        this.dataTracker.set(TRACKED_MAX_LIFE, maxLife);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AMETHYST_SHARD;
    }

    public void configure(float directDamage, float healAmount, float splashMultiplier, float splashRadius, int maxLifeTicks) {
        setTrackedBehaviorMode(BehaviorMode.OFFENSIVE);
        this.directDamage = directDamage;
        this.healAmount = healAmount;
        this.splashMultiplier = splashMultiplier;
        this.splashRadius = splashRadius;
        setTrackedMaxLife(Math.max(10, maxLifeTicks));
    }

    public void configureSpiritTransfer(float healAmount, float splashRadius, int buffDurationTicks, int maxLifeTicks) {
        setTrackedBehaviorMode(BehaviorMode.SPIRIT_TRANSFER);
        this.healAmount = healAmount;
        this.splashRadius = splashRadius;
        this.spiritTransferBuffDurationTicks = buffDurationTicks;
        setTrackedMaxLife(Math.max(10, maxLifeTicks));
    }

    public void configureVisualReturn(int maxLifeTicks) {
        configureVisualReturn(0.0f, maxLifeTicks);
    }

    public void configureVisualReturn(float healAmount, int maxLifeTicks) {
        setTrackedBehaviorMode(BehaviorMode.VISUAL_RETURN);
        this.directDamage = 0.0f;
        this.healAmount = healAmount;
        this.splashMultiplier = 0.0f;
        this.splashRadius = 0.0f;
        setTrackedMaxLife(Math.max(30, maxLifeTicks));
        this.visualPathInitialized = false;
        this.setNoGravity(true);
        this.setVelocity(Vec3d.ZERO);
    }

    public void setHomingTarget(LivingEntity target) {
        if (target == null) {
            this.homingTargetId = -1;
            return;
        }
        this.homingTargetId = target.getId();
        this.homingTicks = 0;
    }

    @Override
    public void tick() {
        syncLocalStateFromTracker();

        if (this.behaviorMode == BehaviorMode.VISUAL_RETURN) {
            tickVisualReturn();
            return;
        }

        super.tick();

        if (!this.getWorld().isClient) {
            tickHoming();
        }

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            spawnTrailParticles(serverWorld);
        }

        if (!this.getWorld().isClient) {
            life++;
            if (life > maxLife) {
                this.discard();
            }
        }
    }

    private void tickVisualReturn() {
        this.baseTick();
        this.setNoGravity(true);
        this.noClip = true;

        Entity owner = this.getOwner();
        if (!(owner instanceof LivingEntity livingOwner) || !livingOwner.isAlive()) {
            this.discard();
            return;
        }

        initializeVisualPathIfNeeded(livingOwner);

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            spawnTrailParticles(serverWorld);
        }

        float rawT = MathHelper.clamp(this.age / (float) this.maxLife, 0.0f, 1.0f);
        float t = smoothStep(rawT);

        Vec3d start = new Vec3d(visualStartX, visualStartY, visualStartZ);
        Vec3d end = livingOwner.getPos().add(
                this.visualArrivalOffsetX,
                this.visualArrivalOffsetY,
                this.visualArrivalOffsetZ
        );

        Vec3d line = end.subtract(start);
        if (line.lengthSquared() < 1.0E-6) {
            line = new Vec3d(0.0, 0.001, 0.0);
        }

        Vec3d forward = line.normalize();

        Vec3d flatPerp = new Vec3d(-forward.z, 0.0, forward.x);
        if (flatPerp.lengthSquared() < 1.0E-6) {
            flatPerp = new Vec3d(1.0, 0.0, 0.0);
        } else {
            flatPerp = flatPerp.normalize();
        }

        Vec3d secondPerp = forward.crossProduct(flatPerp);
        if (secondPerp.lengthSquared() < 1.0E-6) {
            secondPerp = new Vec3d(0.0, 1.0, 0.0);
        } else {
            secondPerp = secondPerp.normalize();
        }

        Vec3d control = start.add(end).multiply(0.5)
                .add(flatPerp.multiply(this.visualLateralArc))
                .add(0.0, this.visualArcHeight, 0.0);

        Vec3d base = quadraticBezier(start, control, end, t);

        double spiralEnvelope = Math.pow(Math.sin(Math.PI * rawT), 0.90);
        double spiralRadius = this.visualSpiralRadius * spiralEnvelope;

        double angle = this.visualSpiralPhase
                + (this.visualSpiralTurns * (Math.PI * 2.0) * rawT * this.visualSideSign);

        Vec3d spiralOffset =
                flatPerp.multiply(Math.cos(angle) * spiralRadius)
                        .add(secondPerp.multiply(Math.sin(angle) * spiralRadius));

        Vec3d pos = base.add(spiralOffset);

        Vec3d prev = this.getPos();
        this.setPosition(pos.x, pos.y, pos.z);
        this.setVelocity(pos.subtract(prev));

        if (rawT >= 1.0f || this.squaredDistanceTo(end) <= 0.08 * 0.08) {
            applyVisualReturnArrival(livingOwner);

            if (this.getWorld() instanceof ServerWorld serverWorld) {
                spawnReturnArrivalParticlesForOthers(serverWorld, end, livingOwner);
            }

            this.setPosition(end.x, end.y, end.z);
            this.discard();
        }
    }

    private void applyVisualReturnArrival(LivingEntity owner) {
        if (this.healAmount > 0.0f) {
            owner.heal(this.healAmount);
        }

        grantDelayedSpiritualSphere(owner);
    }

    private static void grantDelayedSpiritualSphere(LivingEntity owner) {
        int maxAllowed = MonkSpiritSpheres.getMaxSpheres(owner);
        if (maxAllowed <= 0) {
            return;
        }

        StatusEffectInstance inst = owner.getStatusEffect(ModEffects.SPIRITUAL_SPHERES);
        int currentStacks = (inst == null) ? 0 : (inst.getAmplifier() + 1);
        int newStacks = Math.min(maxAllowed, currentStacks + 1);

        boolean ambient = inst != null && inst.isAmbient();
        boolean showParticles = inst == null || inst.shouldShowParticles();
        boolean showIcon = inst == null || inst.shouldShowIcon();

        owner.removeStatusEffect(ModEffects.SPIRITUAL_SPHERES);

        owner.addStatusEffect(
                new StatusEffectInstance(
                        ModEffects.SPIRITUAL_SPHERES,
                        SPHERES_DURATION_TICKS,
                        newStacks - 1,
                        ambient,
                        showParticles,
                        showIcon
                ),
                owner
        );
    }

    private void initializeVisualPathIfNeeded(LivingEntity owner) {
        if (visualPathInitialized) {
            return;
        }

        visualPathInitialized = true;
        visualStartX = this.getX();
        visualStartY = this.getY();
        visualStartZ = this.getZ();

        float r1 = deterministicUnit(17);
        float r2 = deterministicUnit(31);
        float r3 = deterministicUnit(47);
        float r4 = deterministicUnit(63);
        float r5 = deterministicUnit(79);
        float r6 = deterministicUnit(97);
        float r7 = deterministicUnit(113);
        float r8 = deterministicUnit(131);
        float r9 = deterministicUnit(149);

        visualArcHeight = 1.6f + (r1 * 3.0f);

        visualSideSign = r2 < 0.5f ? -1 : 1;
        visualLateralArc = visualSideSign * (0.22f + (r3 * 2.05f));

        visualSpiralRadius = 0.05f + (r4 * 0.08f);
        visualSpiralTurns = 0.25f + (r5 * 0.35f);
        visualSpiralPhase = deterministicUnit(95) * (float) (Math.PI * 2.0);

        Vec3d facing = owner.getRotationVec(1.0f);
        Vec3d flatForward = new Vec3d(facing.x, 0.0, facing.z);

        if (flatForward.lengthSquared() < 1.0E-6) {
            flatForward = new Vec3d(0.0, 0.0, 1.0);
        } else {
            flatForward = flatForward.normalize();
        }

        Vec3d right = new Vec3d(-flatForward.z, 0.0, flatForward.x).normalize();

        double halfWidth = Math.max(0.18, (owner.getWidth() * 0.5) - 0.03);

        double sideSign = r6 < 0.5f ? -1.0 : 1.0;
        double maxSideInsideBody = Math.max(0.16, halfWidth * 0.82);
        double sideAmount = 0.10 + (r7 * (maxSideInsideBody - 0.10));

        double forwardAmount = -0.05 + (r8 * 0.10);
        double upAmount = owner.getHeight() * (0.42 + (r9 * 0.18));

        Vec3d arrivalOffset = right.multiply(sideSign * sideAmount)
                .add(flatForward.multiply(forwardAmount))
                .add(0.0, upAmount, 0.0);

        this.visualArrivalOffsetX = arrivalOffset.x;
        this.visualArrivalOffsetY = arrivalOffset.y;
        this.visualArrivalOffsetZ = arrivalOffset.z;
    }

    private float deterministicUnit(int salt) {
        long x = this.getId();
        x = x * 73428767L + salt * 912931L + 1234567L;
        x ^= (x >>> 33);
        x *= 0xff51afd7ed558ccdL;
        x ^= (x >>> 33);
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= (x >>> 33);

        long positive = x & 0x7fffffffL;
        return positive / (float) 0x7fffffffL;
    }

    private static float smoothStep(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    private static Vec3d quadraticBezier(Vec3d p0, Vec3d p1, Vec3d p2, float t) {
        double u = 1.0 - t;
        return p0.multiply(u * u)
                .add(p1.multiply(2.0 * u * t))
                .add(p2.multiply(t * t));
    }

    private void tickHoming() {
        if (homingTargetId < 0) return;

        homingTicks++;
        if (homingTicks < HOMING_DELAY_TICKS) return;
        if (homingTicks > HOMING_MAX_TICKS) return;

        Entity e = this.getWorld().getEntityById(homingTargetId);
        if (!(e instanceof LivingEntity target) || !target.isAlive()) {
            homingTargetId = -1;
            return;
        }

        Vec3d vel = this.getVelocity();
        double speed = vel.length();
        if (speed < 0.001) return;

        Vec3d currentDir = vel.normalize();

        Vec3d targetPos = target.getPos().add(0.0, target.getHeight() * 0.55, 0.0);
        Vec3d desiredDir = targetPos.subtract(this.getPos()).normalize();

        Vec3d newDir = rotateTowards(currentDir, desiredDir, HOMING_TURN_DEG_PER_TICK);

        this.setVelocity(newDir.multiply(speed));
        this.velocityModified = true;
    }

    private static Vec3d rotateTowards(Vec3d currentDir, Vec3d desiredDir, float maxTurnDeg) {
        double dot = MathHelper.clamp(currentDir.dotProduct(desiredDir), -1.0, 1.0);
        double angle = Math.acos(dot);

        if (angle < 1.0E-6) {
            return desiredDir;
        }

        double maxTurnRad = Math.toRadians(maxTurnDeg);
        double t = Math.min(1.0, maxTurnRad / angle);

        return currentDir.multiply(1.0 - t)
                .add(desiredDir.multiply(t))
                .normalize();
    }

    private void spawnTrailParticles(ServerWorld world) {
        Vec3d vel = this.getVelocity();

        double x = this.getX() - vel.x * 0.25d;
        double y = this.getY() - vel.y * 0.25d;
        double z = this.getZ() - vel.z * 0.25d;

        world.spawnParticles(
                TRAIL_PARTICLE,
                x, y + 0.05d, z,
                1,
                0.02d, 0.02d, 0.02d,
                0.0d
        );

        if (this.random.nextFloat() < 0.25f) {
            world.spawnParticles(
                    ParticleTypes.PORTAL,
                    x, y + 0.05d, z,
                    1,
                    0.03d, 0.03d, 0.03d,
                    0.0d
            );
        }
    }

    private void spawnReturnArrivalParticlesForOthers(ServerWorld world, Vec3d pos, LivingEntity excludedViewer) {
        Collection<ServerPlayerEntity> viewers = new HashSet<>(PlayerLookup.tracking(excludedViewer));
        if (excludedViewer instanceof ServerPlayerEntity excludedPlayer) {
            viewers.remove(excludedPlayer);
        }

        if (viewers.isEmpty()) {
            return;
        }

        ParticleBatch purpleBurst = new ParticleBatch(
                "spell_engine:magic_arcane_burst",
                ParticleBatch.Shape.SPHERE,
                ParticleBatch.Origin.CENTER,
                7.0f,
                0.02f,
                0.08f
        )
                .color(ARCANE_PURPLE_RGBA)
                .scale(1.0f)
                .maxAge(0.9f)
                .extent(0.22f);

        ParticleBatch whiteBurst = new ParticleBatch(
                "spell_engine:magic_arcane_float",
                ParticleBatch.Shape.SPHERE,
                ParticleBatch.Origin.CENTER,
                5.0f,
                0.01f,
                0.05f
        )
                .color(ARCANE_WHITE_RGBA)
                .scale(0.85f)
                .maxAge(0.8f)
                .extent(0.18f);

        ParticleHelper.sendBatches(excludedViewer, new ParticleBatch[]{purpleBurst, whiteBurst}, 1.0f, viewers);
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);

        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        Entity hitEntity = hit.getEntity();
        Entity ownerEntity = this.getOwner();

        if (!(ownerEntity instanceof LivingEntity owner)) {
            this.discard();
            return;
        }

        if (!(hitEntity instanceof LivingEntity target) || !target.isAlive()) {
            this.discard();
            return;
        }

        if (this.behaviorMode == BehaviorMode.VISUAL_RETURN) {
            if (target == owner) {
                applyVisualReturnArrival(owner);
                spawnReturnArrivalParticlesForOthers(serverWorld, this.getPos(), owner);
            }
            this.discard();
            return;
        }

        if (this.behaviorMode == BehaviorMode.SPIRIT_TRANSFER) {
            handleSpiritTransferHit(serverWorld, owner, target);
            return;
        }

        handleOffensiveHit(serverWorld, owner, target);
    }

    private void handleOffensiveHit(ServerWorld serverWorld, LivingEntity owner, LivingEntity target) {
        if (isAlly(owner, target)) {
            this.discard();
            return;
        }

        resetIFrames(target);

        DamageSource arcane = arcaneDamage(serverWorld, this, owner);
        target.damage(arcane, this.directDamage);

        spawnAura415SpellEngine(owner, target, 1.0f);

        float splashDamage = this.directDamage * this.splashMultiplier;

        Box box = target.getBoundingBox().expand(this.splashRadius);

        List<LivingEntity> splashTargets = serverWorld.getEntitiesByClass(
                LivingEntity.class,
                box,
                e -> e.isAlive()
                        && e != owner
                        && e != target
                        && !isAlly(owner, e)
        );

        double radiusSq = this.splashRadius * this.splashRadius;

        for (LivingEntity e : splashTargets) {
            if (e.squaredDistanceTo(target) <= radiusSq) {
                resetIFrames(e);
                e.damage(arcane, splashDamage);
                spawnAura415SpellEngine(owner, e, SPLASH_SCALE_MUL);
            }
        }

        Vec3d impactPos = target.getPos().add(0.0d, target.getHeight() * 0.55d, 0.0d);
        spawnImpactParticles(serverWorld, impactPos);

        this.discard();
    }

    private void handleSpiritTransferHit(ServerWorld serverWorld, LivingEntity owner, LivingEntity target) {
        if (!isAlly(owner, target)) {
            this.discard();
            return;
        }

        Box box = target.getBoundingBox().expand(this.splashRadius);

        List<LivingEntity> splashTargets = serverWorld.getEntitiesByClass(
                LivingEntity.class,
                box,
                e -> e.isAlive() && isAlly(owner, e)
        );

        double radiusSq = this.splashRadius * this.splashRadius;

        for (LivingEntity e : splashTargets) {
            if (e.squaredDistanceTo(target) <= radiusSq) {
                e.heal(this.healAmount);
                applyVigorChakra(e, this.spiritTransferBuffDurationTicks);
                spawnAura415SpellEngine(owner, e, 1.0f);
            }
        }

        Vec3d impactPos = target.getPos().add(0.0d, target.getHeight() * 0.55d, 0.0d);
        spawnImpactParticles(serverWorld, impactPos);

        this.discard();
    }

    private static void applyVigorChakra(LivingEntity target, int durationTicks) {
        target.addStatusEffect(
                new StatusEffectInstance(
                        ModEffects.VIGOR_CHAKRA,
                        durationTicks,
                        0,
                        true,
                        true,
                        true
                ),
                target
        );
    }

    private static void resetIFrames(LivingEntity entity) {
        if (entity instanceof EntityIFramesAccessor accessor) {
            accessor.monkmod$setTimeUntilRegen(0);
        }
    }

    private static DamageSource arcaneDamage(ServerWorld world, Entity source, LivingEntity attacker) {
        DamageSource se = tryDamageType(world, source, attacker, Identifier.of("spell_engine", "arcane"));
        if (se != null) return se;

        DamageSource sp = tryDamageType(world, source, attacker, Identifier.of("spell_power", "arcane"));
        if (sp != null) return sp;

        return world.getDamageSources().magic();
    }

    private static DamageSource tryDamageType(ServerWorld world, Entity source, LivingEntity attacker, Identifier id) {
        try {
            RegistryKey<DamageType> key = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, id);
            RegistryEntry.Reference<DamageType> entry = world.getRegistryManager()
                    .get(RegistryKeys.DAMAGE_TYPE)
                    .entryOf(key);

            return new DamageSource(entry, source, attacker);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void spawnImpactParticles(ServerWorld world, Vec3d pos) {
        world.spawnParticles(
                IMPACT_PARTICLE,
                pos.x, pos.y, pos.z,
                22,
                0.22d, 0.28d, 0.22d,
                0.0d
        );

        world.spawnParticles(
                TRAIL_PARTICLE,
                pos.x, pos.y, pos.z,
                18,
                0.35d, 0.35d, 0.35d,
                0.0d
        );

        world.spawnParticles(
                ParticleTypes.END_ROD,
                pos.x, pos.y, pos.z,
                14,
                0.25d, 0.25d, 0.25d,
                0.02d
        );

        world.spawnParticles(
                ParticleTypes.PORTAL,
                pos.x, pos.y, pos.z,
                12,
                0.20d, 0.25d, 0.20d,
                0.08d
        );
    }

    private static void spawnAura415SpellEngine(LivingEntity owner, LivingEntity victim, float scaleMul) {
        ParticleBatch aura = new ParticleBatch(
                "spell_engine:aura_effect_415",
                ParticleBatch.Shape.SPHERE,
                ParticleBatch.Origin.CENTER,
                AURA_415_COUNT,
                0.0f,
                0.0f
        )
                .color(ARCANE_PURPLE_RGBA)
                .scale(AURA_415_BASE_SCALE * scaleMul)
                .maxAge(AURA_415_MAX_AGE)
                .extent(AURA_415_EXTENT * scaleMul);

        Collection<ServerPlayerEntity> viewers = new HashSet<>(PlayerLookup.tracking(victim));
        if (owner instanceof ServerPlayerEntity sp) viewers.add(sp);
        if (victim instanceof ServerPlayerEntity sp2) viewers.add(sp2);

        ParticleHelper.sendBatches(victim, new ParticleBatch[]{aura}, 1.0f, viewers);
    }

    private static boolean isAlly(LivingEntity a, LivingEntity b) {
        if (a.isTeammate(b) || b.isTeammate(a)) {
            return true;
        }

        if (a instanceof PlayerEntity p1 && b instanceof PlayerEntity p2) {
            return !p1.shouldDamagePlayer(p2);
        }

        if (b instanceof TameableEntity tame && tame.isTamed() && tame.getOwnerUuid() != null) {
            return tame.getOwnerUuid().equals(a.getUuid());
        }

        return false;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        if (this.behaviorMode == BehaviorMode.VISUAL_RETURN) {
            return;
        }

        super.onCollision(hitResult);

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Vec3d p = this.getPos();

            serverWorld.spawnParticles(
                    TRAIL_PARTICLE,
                    p.x, p.y + 0.1d, p.z,
                    4,
                    0.1d, 0.08d, 0.1d,
                    0.0d
            );
        }

        if (!this.getWorld().isClient) {
            this.discard();
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

        nbt.putInt("Life", this.life);
        nbt.putInt("MaxLife", this.maxLife);

        nbt.putFloat("DirectDamage", this.directDamage);
        nbt.putFloat("HealAmount", this.healAmount);
        nbt.putFloat("SplashMultiplier", this.splashMultiplier);
        nbt.putFloat("SplashRadius", this.splashRadius);

        nbt.putString("BehaviorMode", this.behaviorMode.name());
        nbt.putInt("SpiritTransferBuffDurationTicks", this.spiritTransferBuffDurationTicks);

        nbt.putInt("HomingTargetId", this.homingTargetId);
        nbt.putInt("HomingTicks", this.homingTicks);

        nbt.putBoolean("VisualPathInitialized", this.visualPathInitialized);
        nbt.putDouble("VisualStartX", this.visualStartX);
        nbt.putDouble("VisualStartY", this.visualStartY);
        nbt.putDouble("VisualStartZ", this.visualStartZ);
        nbt.putFloat("VisualArcHeight", this.visualArcHeight);
        nbt.putFloat("VisualLateralArc", this.visualLateralArc);
        nbt.putFloat("VisualSpiralRadius", this.visualSpiralRadius);
        nbt.putFloat("VisualSpiralTurns", this.visualSpiralTurns);
        nbt.putFloat("VisualSpiralPhase", this.visualSpiralPhase);
        nbt.putInt("VisualSideSign", this.visualSideSign);
        nbt.putDouble("VisualArrivalOffsetX", this.visualArrivalOffsetX);
        nbt.putDouble("VisualArrivalOffsetY", this.visualArrivalOffsetY);
        nbt.putDouble("VisualArrivalOffsetZ", this.visualArrivalOffsetZ);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);

        this.life = nbt.getInt("Life");
        this.maxLife = nbt.getInt("MaxLife");

        this.directDamage = nbt.getFloat("DirectDamage");
        this.healAmount = nbt.getFloat("HealAmount");
        this.splashMultiplier = nbt.getFloat("SplashMultiplier");
        this.splashRadius = nbt.getFloat("SplashRadius");

        try {
            this.behaviorMode = BehaviorMode.valueOf(nbt.getString("BehaviorMode"));
        } catch (Exception ignored) {
            this.behaviorMode = BehaviorMode.OFFENSIVE;
        }

        this.spiritTransferBuffDurationTicks = nbt.getInt("SpiritTransferBuffDurationTicks");

        this.homingTargetId = nbt.getInt("HomingTargetId");
        this.homingTicks = nbt.getInt("HomingTicks");

        this.visualPathInitialized = nbt.getBoolean("VisualPathInitialized");
        this.visualStartX = nbt.getDouble("VisualStartX");
        this.visualStartY = nbt.getDouble("VisualStartY");
        this.visualStartZ = nbt.getDouble("VisualStartZ");
        this.visualArcHeight = nbt.getFloat("VisualArcHeight");
        this.visualLateralArc = nbt.getFloat("VisualLateralArc");
        this.visualSpiralRadius = nbt.getFloat("VisualSpiralRadius");
        this.visualSpiralTurns = nbt.getFloat("VisualSpiralTurns");
        this.visualSpiralPhase = nbt.getFloat("VisualSpiralPhase");
        this.visualSideSign = nbt.getInt("VisualSideSign");
        this.visualArrivalOffsetX = nbt.getDouble("VisualArrivalOffsetX");
        this.visualArrivalOffsetY = nbt.getDouble("VisualArrivalOffsetY");
        this.visualArrivalOffsetZ = nbt.getDouble("VisualArrivalOffsetZ");
    }
}