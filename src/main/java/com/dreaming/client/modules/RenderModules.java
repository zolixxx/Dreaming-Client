package com.dreaming.client.modules;

import com.dreaming.client.module.Category;
import com.dreaming.client.module.Module;
import com.dreaming.client.render.RenderUtil;
import com.dreaming.client.setting.BoolSetting;
import com.dreaming.client.setting.NumSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RenderModules {
    private RenderModules() {}

    public static class OreESP extends Module {
        private record Group(BoolSetting on, int color) {}
        private record Hit(BlockPos pos, int color) {}

        private final NumSetting range = add(new NumSetting("Range", 20, 8, 32, 2));
        private final Map<Block, Group> groups = new HashMap<>();
        private volatile List<Hit> hits = new ArrayList<>();
        private int cd;

        public OreESP() {
            super("OreESP", "Shows nearby ores through walls.", Category.RENDER);
            group("Diamond", true, 0x4DE6FF, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
            group("Emerald", true, 0x3DFF6B, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
            group("Gold", true, 0xFFD84D, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
            group("Iron", false, 0xE0C8B0, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
            group("Coal", false, 0x777777, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
            group("Copper", false, 0xE07B3C, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
            group("Redstone", false, 0xFF3B3B, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
            group("Lapis", false, 0x3B5BFF, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
            group("Debris", true, 0x8B5A3C, Blocks.ANCIENT_DEBRIS);
        }

        private void group(String name, boolean def, int color, Block... blocks) {
            Group g = new Group(add(new BoolSetting(name, def)), color);
            for (Block b : blocks) groups.put(b, g);
        }

        @Override public void onTick() {
            if (cd-- > 0) return;
            cd = 20;
            int r = (int) range.value;
            BlockPos c = mc.player.getBlockPos();
            BlockPos.Mutable m = new BlockPos.Mutable();
            List<Hit> found = new ArrayList<>();
            for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
                m.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                Group g = groups.get(mc.world.getBlockState(m).getBlock());
                if (g != null && g.on().value) {
                    found.add(new Hit(m.toImmutable(), g.color()));
                    if (found.size() >= 3000) { hits = found; return; }
                }
            }
            hits = found;
        }

        @Override public void onWorldRender(WorldRenderContext ctx) {
            for (Hit h : hits) RenderUtil.add(new Box(h.pos()), h.color());
        }
        @Override public void onDisable() { hits = new ArrayList<>(); }
    }

    public static class Tracers extends Module {
        private final BoolSetting players = add(new BoolSetting("Players", true));
        private final BoolSetting hostiles = add(new BoolSetting("Hostiles", true));
        private final BoolSetting passives = add(new BoolSetting("Passives", false));
        private final BoolSetting items = add(new BoolSetting("Items", false));
        private final NumSetting range = add(new NumSetting("Range", 64, 8, 256, 8));

        public Tracers() { super("Tracers", "Lines from your crosshair to entities.", Category.RENDER); }

        @Override public void onWorldRender(WorldRenderContext ctx) {
            float td = ctx.tickCounter().getTickDelta(false);
            Vec3d cam = ctx.camera().getPos();
            Vec3d from = cam.add(Vec3d.fromPolar(ctx.camera().getPitch(), ctx.camera().getYaw()).multiply(0.5));
            double r2 = range.value * range.value;
            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player || e.squaredDistanceTo(cam) > r2) continue;
                int col;
                if (e instanceof PlayerEntity) col = players.value ? 0x00E5FF : 0;
                else if (e instanceof HostileEntity) col = hostiles.value ? 0xFF4D4D : 0;
                else if (e instanceof ItemEntity) col = items.value ? 0xFFD84D : 0;
                else if (e instanceof LivingEntity && !(e instanceof ArmorStandEntity)) col = passives.value ? 0x5CFF7A : 0;
                else col = 0;
                if (col == 0) continue;
                RenderUtil.line(from, e.getLerpedPos(td).add(0, e.getHeight() / 2.0, 0), col);
            }
        }
    }

    public static class Hitboxes extends Module {
        public Hitboxes() { super("Hitboxes", "Vanilla hitbox outlines on every entity.", Category.RENDER); }
        @Override public void onEnable() { mc.getEntityRenderDispatcher().setRenderHitboxes(true); }
        @Override public void onDisable() { mc.getEntityRenderDispatcher().setRenderHitboxes(false); }
    }

    public static class Breadcrumbs extends Module {
        private final NumSetting length = add(new NumSetting("Length", 400, 50, 2000, 50));
        private final List<Vec3d> trail = new ArrayList<>();

        public Breadcrumbs() { super("Breadcrumbs", "Draws a trail behind you.", Category.RENDER); }

        @Override public void onTick() {
            Vec3d p = mc.player.getPos();
            if (trail.isEmpty() || trail.get(trail.size() - 1).squaredDistanceTo(p) > 0.16) trail.add(p);
            while (trail.size() > length.value) trail.remove(0);
        }
        @Override public void onWorldRender(WorldRenderContext ctx) {
            for (int i = 1; i < trail.size(); i++) RenderUtil.line(trail.get(i - 1), trail.get(i), 0xB388FF);
        }
        @Override public void onDisable() { trail.clear(); }
    }

    public static class Fullbright extends Module {
        public Fullbright() { super("Fullbright", "Everything is bright, even in caves.", Category.RENDER); }
        @Override public void onTick() {
            StatusEffectInstance cur = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (cur == null || cur.getDuration() < 220)
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
        }
        @Override public void onDisable() {
            if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    public static class Zoom extends Module {
        private final NumSetting fov = add(new NumSetting("Zoom FOV", 30, 30, 70, 5));
        private Integer prev;
        public Zoom() { super("Zoom", "Narrows your field of view. Bind it to a key.", Category.RENDER); }
        @Override public void onEnable() { prev = mc.options.getFov().getValue(); }
        @Override public void onTick() { mc.options.getFov().setValue((int) fov.value); }
        @Override public void onDisable() { if (prev != null) mc.options.getFov().setValue(prev); }
    }

    public static class FovChanger extends Module {
        private final NumSetting fov = add(new NumSetting("FOV", 100, 30, 110, 5));
        private Integer prev;
        public FovChanger() { super("FOV", "Sets your field of view.", Category.RENDER); }
        @Override public void onEnable() { prev = mc.options.getFov().getValue(); }
        @Override public void onTick() { mc.options.getFov().setValue((int) fov.value); }
        @Override public void onDisable() { if (prev != null) mc.options.getFov().setValue(prev); }
    }
}
