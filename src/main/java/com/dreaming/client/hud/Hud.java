package com.dreaming.client.hud;

import com.dreaming.client.DreamingClient;
import com.dreaming.client.module.Module;
import com.dreaming.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Hud {
    private Hud() {}

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        TextRenderer tr = mc.textRenderer;

        // Gradient watermark: purple -> pink
        String title = DreamingClient.NAME;
        int x = 4;
        for (int i = 0; i < title.length(); i++) {
            float t = i / (float) (title.length() - 1);
            int r = (int) (139 + (255 - 139) * t);
            int g = (int) (92 + (130 - 92) * t);
            int b = (int) (246 + (220 - 246) * t);
            String ch = String.valueOf(title.charAt(i));
            ctx.drawTextWithShadow(tr, ch, x, 4, 0xFF000000 | (r << 16) | (g << 8) | b);
            x += tr.getWidth(ch);
        }
        ctx.drawTextWithShadow(tr, "v" + DreamingClient.VERSION, x + 4, 4, 0xFF888899);

        if (!DreamingClient.allowed()) {
            ctx.drawTextWithShadow(tr, "Singleplayer only - modules disabled", 4, 16, 0xFFFF6666);
            return;
        }

        for (Module hm : ModuleManager.all()) if (hm.enabled) hm.onHud(ctx);

        // Enabled-modules list (right side, longest first)
        List<Module> on = new ArrayList<>();
        for (Module m : ModuleManager.all()) if (m.enabled && !m.settingsOnly) on.add(m);
        on.sort(Comparator.comparingInt((Module m) -> tr.getWidth(m.name)).reversed());

        int y = 4;
        for (Module m : on) {
            int w = tr.getWidth(m.name);
            int rx = ctx.getScaledWindowWidth() - w - 6;
            ctx.fill(rx - 2, y - 1, ctx.getScaledWindowWidth(), y + 10, 0x80000000);
            ctx.fill(ctx.getScaledWindowWidth() - 2, y - 1, ctx.getScaledWindowWidth(), y + 10, 0xFF8B5CF6);
            ctx.drawTextWithShadow(tr, m.name, rx, y, 0xFFC4B5FD);
            y += 11;
        }
    }
}
