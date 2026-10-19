package com.dreaming.client.modules;

import com.dreaming.client.module.Category;
import com.dreaming.client.module.Module;
import com.dreaming.client.setting.BoolSetting;
import com.dreaming.client.setting.NumSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HudModules {
    private HudModules() {}

    public static class Info extends Module {
        private final BoolSetting coords = add(new BoolSetting("Coordinates", true));
        private final BoolSetting fps = add(new BoolSetting("FPS", true));
        private final BoolSetting speed = add(new BoolSetting("Speed", true));
        private final BoolSetting dir = add(new BoolSetting("Direction", true));
        private final BoolSetting biome = add(new BoolSetting("Biome", false));
        private final BoolSetting time = add(new BoolSetting("Clock", false));
        private final BoolSetting day = add(new BoolSetting("Day", false));
        private double px, pz, bps;

        public Info() { super("Info", "Coordinates, FPS, speed and more.", Category.HUD); }

        @Override public void onTick() {
            Vec3d p = mc.player.getPos();
            bps = bps * 0.8 + Math.hypot(p.x - px, p.z - pz) * 20 * 0.2;
            px = p.x; pz = p.z;
        }

        @Override public void onHud(DrawContext ctx) {
            List<String> l = new ArrayList<>();
            if (coords.value) l.add(String.format("XYZ  %.1f  %.1f  %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
            if (fps.value) l.add("FPS  " + mc.getCurrentFps());
            if (speed.value) l.add(String.format("Speed  %.1f b/s", bps));
            if (dir.value) {
                String d = mc.player.getHorizontalFacing().asString();
                l.add("Facing  " + Character.toUpperCase(d.charAt(0)) + d.substring(1));
            }
            if (biome.value) l.add("Biome  " + mc.world.getBiome(mc.player.getBlockPos()).getKey()
                    .map(k -> k.getValue().getPath()).orElse("?"));
            if (time.value) l.add("Time  " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            if (day.value) l.add("Day  " + (mc.world.getTimeOfDay() / 24000L));
            int y = 18;
            for (String s : l) { ctx.drawTextWithShadow(mc.textRenderer, s, 4, y, 0xFFE0D4FF); y += 10; }
        }
    }

    public static class Keystrokes extends Module {
        public Keystrokes() { super("Keystrokes", "Shows WASD and space on screen.", Category.HUD); }

        private void key(DrawContext c, String s, int x, int y, boolean on) {
            c.fill(x, y, x + 18, y + 18, on ? 0xC08B5CF6 : 0x80000000);
            c.drawCenteredTextWithShadow(mc.textRenderer, s, x + 9, y + 5, 0xFFFFFFFF);
        }

        @Override public void onHud(DrawContext ctx) {
            int x = 6, y = ctx.getScaledWindowHeight() - 64;
            key(ctx, "W", x + 20, y, mc.options.forwardKey.isPressed());
            key(ctx, "A", x, y + 20, mc.options.leftKey.isPressed());
            key(ctx, "S", x + 20, y + 20, mc.options.backKey.isPressed());
            key(ctx, "D", x + 40, y + 20, mc.options.rightKey.isPressed());
            ctx.fill(x, y + 40, x + 58, y + 52, mc.options.jumpKey.isPressed() ? 0xC08B5CF6 : 0x80000000);
            ctx.fill(x + 16, y + 45, x + 42, y + 47, 0xFFFFFFFF);
        }
    }

    public static class Potions extends Module {
        public Potions() { super("Potions", "Active effects with timers.", Category.HUD); }

        @Override public void onHud(DrawContext ctx) {
            TextRenderer tr = mc.textRenderer;
            int y = ctx.getScaledWindowHeight() - 30;
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                String name = e.getEffectType().value().getName().getString();
                if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
                int sec = e.getDuration() / 20;
                String t = e.isInfinite() ? "**" : String.format("%d:%02d", sec / 60, sec % 60);
                String line = name + "  " + t;
                int col = 0xFF000000 | e.getEffectType().value().getColor();
                ctx.drawTextWithShadow(tr, line, ctx.getScaledWindowWidth() - 4 - tr.getWidth(line), y, col);
                y -= 10;
            }
        }
    }

    public static class ArmorHud extends Module {
        public ArmorHud() { super("ArmorHud", "Armor pieces with durability bars.", Category.HUD); }

        @Override public void onHud(DrawContext ctx) {
            int x = 4, y = ctx.getScaledWindowHeight() / 2 - 40;
            for (int i = 3; i >= 0; i--) {
                ItemStack st = mc.player.getInventory().getArmorStack(i);
                if (!st.isEmpty()) {
                    ctx.drawItem(st, x, y);
                    ctx.drawItemInSlot(mc.textRenderer, st, x, y);
                }
                y += 18;
            }
        }
    }

    public static class Radar extends Module {
        private final NumSetting range = add(new NumSetting("Range", 32, 16, 96, 8));
        public Radar() { super("Radar", "Top-down minimap of nearby entities.", Category.HUD); }

        @Override public void onHud(DrawContext ctx) {
            int size = 64, half = size / 2;
            int x = ctx.getScaledWindowWidth() - size - 6, y = ctx.getScaledWindowHeight() / 2 - half;
            ctx.fill(x, y, x + size, y + size, 0x90000000);
            ctx.fill(x, y, x + size, y + 1, 0xFF8B5CF6);
            ctx.fill(x, y + size - 1, x + size, y + size, 0xFF8B5CF6);
            ctx.fill(x, y, x + 1, y + size, 0xFF8B5CF6);
            ctx.fill(x + size - 1, y, x + size, y + size, 0xFF8B5CF6);
            ctx.fill(x + half, y + 2, x + half + 1, y + size - 2, 0x30FFFFFF);
            ctx.fill(x + 2, y + half, x + size - 2, y + half + 1, 0x30FFFFFF);

            double yaw = Math.toRadians(mc.player.getYaw());
            double cos = Math.cos(yaw), sin = Math.sin(yaw);
            double scale = half / range.value;
            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player || !(e instanceof LivingEntity) || e instanceof ArmorStandEntity) continue;
                double dx = e.getX() - mc.player.getX(), dz = e.getZ() - mc.player.getZ();
                double sx = (-dx * cos - dz * sin) * scale, sy = (dx * sin - dz * cos) * scale;
                if (Math.abs(sx) > half - 2 || Math.abs(sy) > half - 2) continue;
                int col = e instanceof PlayerEntity ? 0xFF00E5FF : e instanceof HostileEntity ? 0xFFFF4D4D : 0xFF5CFF7A;
                int px = x + half + (int) sx, py = y + half + (int) sy;
                ctx.fill(px - 1, py - 1, px + 2, py + 2, col);
            }
            ctx.fill(x + half - 1, y + half - 1, x + half + 2, y + half + 2, 0xFFFFFFFF);
        }
    }
}
