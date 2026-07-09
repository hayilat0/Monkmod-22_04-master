package net.gabriel.monk.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;

public class SpiritualMeteorParticle extends SpriteBillboardParticle {

    private final SpriteProvider spriteProvider;
    private final float initialScale;

    protected SpiritualMeteorParticle(
            ClientWorld world,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            SpriteProvider spriteProvider
    ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);

        this.spriteProvider = spriteProvider;

        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;

        /*
         * Ajustado para ficar mais "reto" e sutil:
         * - sem gravidade;
         * - sem aceleração para cima;
         * - sem rotação contínua;
         * - vida curta, para não virar rastro exagerado.
         */
        this.maxAge = 4 + this.random.nextInt(3); // 4~6 ticks
        this.gravityStrength = 0.0f;
        this.velocityMultiplier = 1.0f;

        this.initialScale = 0.20f + this.random.nextFloat() * 0.08f;
        this.scale = this.initialScale;

        float colorVariant = this.random.nextFloat();

        if (colorVariant < 0.45f) {
            this.setColor(0.95f, 0.88f, 1.00f);
        } else if (colorVariant < 0.82f) {
            this.setColor(0.72f, 0.42f, 1.00f);
        } else {
            this.setColor(0.46f, 0.12f, 0.86f);
        }

        this.setAlpha(0.92f);

        this.angle = 0.0f;
        this.prevAngle = 0.0f;

        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }

        float lifeProgress = (float) this.age / (float) this.maxAge;
        float fade = 1.0f - lifeProgress;

        this.scale = this.initialScale * MathHelper.clamp(0.85f + fade * 0.25f, 0.0f, 1.0f);
        this.setAlpha(MathHelper.clamp(fade, 0.0f, 1.0f));

        // Movimento totalmente retilíneo.
        this.move(this.velocityX, this.velocityY, this.velocityZ);

        this.setSpriteForAge(this.spriteProvider);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {

        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientWorld world,
                double x,
                double y,
                double z,
                double velocityX,
                double velocityY,
                double velocityZ
        ) {
            return new SpiritualMeteorParticle(
                    world,
                    x,
                    y,
                    z,
                    velocityX,
                    velocityY,
                    velocityZ,
                    this.spriteProvider
            );
        }
    }
}