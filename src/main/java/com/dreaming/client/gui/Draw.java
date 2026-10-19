package com.dreaming.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public final class Draw {
    private Draw() {}

    public static float clamp01(float v) { return v < 0 ? 0 : Math.min(v, 1); }

    /** Multiplies the alpha channel of a color. */
    public static int a(int argb, float alpha) {
        int base = (argb >>> 24) & 255;
        int na = Math.round(base * clamp01(alpha));
        return (na << 24) | (argb & 0xFFFFFF);
    }

    public static int mix(int c1, int c2, float t) {
        t = clamp01(t);
        int a = (int) (((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Rounded rectangle built from scanlines. */
    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if ((color >>> 24) < 3 || w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { c.fill(x, y, x + w, y + h, color); return; }
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    public static void text(DrawContext c, TextRenderer tr, String s, float x, float y, int color, float scale) {
        if ((color >>> 24) < 12) return; // the font renderer treats very low alpha as opaque
        MatrixStack ms = c.getMatrices();
        ms.push();
        ms.translate(x, y, 0);
        ms.scale(scale, scale, 1f);
        c.drawText(tr, s, 0, 0, color, false);
        ms.pop();
    }
}
