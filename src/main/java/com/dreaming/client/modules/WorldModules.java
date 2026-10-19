package com.dreaming.client.modules;

import com.dreaming.client.module.Category;
import com.dreaming.client.module.Module;
import com.dreaming.client.setting.BoolSetting;
import com.dreaming.client.setting.NumSetting;
import net.minecraft.block.BlockState;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class WorldModules {
    private WorldModules() {}

    public static class TimeChanger extends Module {
        private final NumSetting time = add(new NumSetting("Time", 6000, 0, 24000, 500));
        public TimeChanger() { super("TimeChanger", "Locks the time of day (client side).", Category.WORLD); }
        @Override public void onTick() { mc.world.setTimeOfDay((long) time.value); }
    }

    public static class NoWeather extends Module {
        public NoWeather() { super("NoWeather", "Hides rain and thunder.", Category.WORLD); }
        @Override public void onTick() {
            mc.world.setRainGradient(0f);
            mc.world.setThunderGradient(0f);
        }
    }

    public static class AutoTool extends Module {
        public AutoTool() { super("AutoTool", "Picks the fastest hotbar tool for the block you mine.", Category.WORLD); }
        @Override public void onTick() {
            if (mc.currentScreen != null || !mc.options.attackKey.isPressed()) return;
            if (!(mc.crosshairTarget instanceof BlockHitResult r) || r.getType() != HitResult.Type.BLOCK) return;
            BlockState st = mc.world.getBlockState(r.getBlockPos());
            float best = 1f;
            int slot = -1;
            for (int i = 0; i < 9; i++) {
                float sp = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(st);
                if (sp > best) { best = sp; slot = i; }
            }
            if (slot >= 0) mc.player.getInventory().selectedSlot = slot;
        }
    }

    public static class Nuker extends Module {
        private final NumSetting radius = add(new NumSetting("Radius", 3, 1, 6, 1));
        private final BoolSetting skipLiquids = add(new BoolSetting("Skip liquids", true));
        public Nuker() { super("Nuker", "Breaks the nearest block around you.", Category.WORLD); }

        @Override public void onTick() {
            if (mc.currentScreen != null) return;
            int r = (int) radius.value;
            Vec3d eye = mc.player.getEyePos();
            BlockPos c = mc.player.getBlockPos();
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            BlockPos.Mutable m = new BlockPos.Mutable();
            for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
                m.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                BlockState st = mc.world.getBlockState(m);
                if (st.isAir() || st.getHardness(mc.world, m) < 0) continue;
                if (skipLiquids.value && !st.getFluidState().isEmpty()) continue;
                double d = Sp.dist2(m, eye);
                if (d <= r * r + 1 && d < bd) { bd = d; best = m.toImmutable(); }
            }
            if (best != null) {
                mc.interactionManager.updateBlockBreakingProgress(best, Direction.UP);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }
}
