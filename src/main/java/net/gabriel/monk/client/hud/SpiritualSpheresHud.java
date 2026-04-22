package net.gabriel.monk.client.hud;

import net.gabriel.monk.effect.ModEffects;
import net.gabriel.monk.util.MonkSpiritSpheres;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;

public final class SpiritualSpheresHud {

    private static final int MAX_DURATION_TICKS = 280;

    private static final int ORB_SIZE = 12;
    private static final int ORB_GAP = 4;

    private static final int LEFT_PADDING = 4;
    private static final int BOTTOM_PADDING = 26;

    private static final int TIMER_BAR_WIDTH = 4;
    private static final int TIMER_BAR_GAP = 4;

    private static final int COLOR_EMPTY_BORDER = 0x22000000;
    private static final int COLOR_EMPTY_FILL = 0x66FFFFFF;

    private static final int COLOR_FULL_BORDER = 0xFF8E4FFF;
    private static final int COLOR_FULL_FILL = 0xFFF2E35F;

    private static final int COLOR_GLOW = 0x447E4DFF;
    private static final int COLOR_HIGHLIGHT = 0xCCFFFFFF;
    private static final int COLOR_SPARKLE = 0xAAFFFFFF;

    private static final int TIMER_BG = 0x66000000;
    private static final int TIMER_BORDER = 0x99FFFFFF;
    private static final int TIMER_FILL = 0xFF8E4FFF;

    private SpiritualSpheresHud() {}

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.options.hudHidden) return;

        StatusEffectInstance spheres = client.player.getStatusEffect(ModEffects.SPIRITUAL_SPHERES);
        if (spheres == null) return;

        int maxSlots = MonkSpiritSpheres.getMaxSpheres(client.player);

        int stacks = Math.min(maxSlots, spheres.getAmplifier() + 1);
        if (stacks < 0) stacks = 0;

        int duration = spheres.getDuration();
        float remainingFrac = clamp01(Math.min(duration, MAX_DURATION_TICKS) / (float) MAX_DURATION_TICKS);

        int screenH = client.getWindow().getScaledHeight();

        int totalHeight = (maxSlots * (ORB_SIZE + ORB_GAP)) - ORB_GAP;

        int yBottom = screenH - BOTTOM_PADDING;
        int yTop = yBottom - totalHeight;

        int xOrbs = LEFT_PADDING;
        int xTimer = xOrbs + ORB_SIZE + TIMER_BAR_GAP;

        // Timer fundo + borda
        context.fill(xTimer, yTop, xTimer + TIMER_BAR_WIDTH, yTop + totalHeight, TIMER_BG);
        drawBorder(context, xTimer, yTop, TIMER_BAR_WIDTH, totalHeight, TIMER_BORDER);

        // Preenchimento do timer (de baixo pra cima)
        int innerHeight = totalHeight - 2;
        int fillH = Math.round(innerHeight * remainingFrac);

        if (fillH > 0) {
            int yFillTop = (yTop + 1) + (innerHeight - fillH);
            int yFillBottom = (yTop + 1) + innerHeight;

            context.fill(
                    xTimer + 1,
                    yFillTop,
                    xTimer + TIMER_BAR_WIDTH - 1,
                    yFillBottom,
                    TIMER_FILL
            );
        }

        float t = client.player.age + tickCounter.getTickDelta(false);

        // ✅ desenha 5 ou 6 orbes, preenchendo de baixo para cima
        for (int i = 0; i < maxSlots; i++) {
            int y = yTop + i * (ORB_SIZE + ORB_GAP);

            int indexFromBottom = (maxSlots - 1) - i;
            boolean filled = indexFromBottom < stacks;

            drawOrb(context, xOrbs, y, filled, t, indexFromBottom);
        }
    }

    private static void drawOrb(DrawContext context, int x, int y, boolean filled, float t, int indexFromBottom) {
        int r = ORB_SIZE / 2;
        int cx = x + r;
        int cy = y + r;

        if (filled) {
            drawCircle(context, cx, cy, r + 3, COLOR_GLOW);
        }

        int fillColor = filled ? COLOR_FULL_FILL : COLOR_EMPTY_FILL;
        int borderColor = filled ? COLOR_FULL_BORDER : COLOR_EMPTY_BORDER;

        drawCircle(context, cx, cy, r, fillColor);
        drawCircle(context, cx, cy, r - 1, borderColor);

        if (!filled) return;

        // highlights
        context.fill(cx, cy - 2, cx + 1, cy - 1, COLOR_HIGHLIGHT);
        context.fill(cx - 1, cy - 1, cx + 2, cy, COLOR_HIGHLIGHT);
        context.fill(cx, cy, cx + 1, cy + 1, COLOR_HIGHLIGHT);

        // sparkles orbitando
        float phase = t * 0.12f + indexFromBottom * 1.7f;
        sparkle(context, cx, cy, r + 2, phase, COLOR_SPARKLE);
        sparkle(context, cx, cy, r + 1, phase + 2.1f, COLOR_SPARKLE);
        sparkle(context, cx, cy, r + 2, phase + 4.2f, COLOR_SPARKLE);
    }

    private static void sparkle(DrawContext context, int cx, int cy, int dist, float phase, int color) {
        int sx = cx + (int) Math.round(Math.cos(phase) * dist);
        int sy = cy + (int) Math.round(Math.sin(phase) * dist);
        context.fill(sx, sy, sx + 1, sy + 1, color);
    }

    private static void drawCircle(DrawContext context, int cx, int cy, int r, int color) {
        if (r <= 0) return;

        int rr = r * r;
        for (int dy = -r; dy <= r; dy++) {
            int y = cy + dy;
            int dx = (int) Math.floor(Math.sqrt(Math.max(0, rr - dy * dy)));

            int x1 = cx - dx;
            int x2 = cx + dx + 1;

            context.fill(x1, y, x2, y + 1, color);
        }
    }

    private static void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        // top
        context.fill(x, y, x + w, y + 1, color);
        // bottom
        context.fill(x, y + h - 1, x + w, y + h, color);
        // left
        context.fill(x, y, x + 1, y + h, color);
        // right
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
