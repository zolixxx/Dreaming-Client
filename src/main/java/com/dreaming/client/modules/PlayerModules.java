package com.dreaming.client.modules;

import com.dreaming.client.module.Category;
import com.dreaming.client.module.Module;
import com.dreaming.client.setting.BoolSetting;
import com.dreaming.client.setting.NumSetting;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public final class PlayerModules {
    private PlayerModules() {}

    public static class GodMode extends Module {
        private final BoolSetting fed = add(new BoolSetting("Keep fed", true));
        public GodMode() { super("GodMode", "Take no damage (singleplayer).", Category.PLAYER); }

        @Override public void onTick() {
            boolean feed = fed.value;
            Sp.run(p -> {
                p.setInvulnerable(true);
                p.setHealth(p.getMaxHealth());
                if (feed) p.getHungerManager().setFoodLevel(20);
            });
        }
        @Override public void onDisable() {
            if (mc.player != null && !mc.player.isCreative()) Sp.run(p -> p.setInvulnerable(false));
        }
    }

    public static class AutoRespawn extends Module {
        public AutoRespawn() { super("AutoRespawn", "Respawns instantly after dying.", Category.PLAYER); }
        @Override public void onTick() {
            if (mc.currentScreen instanceof DeathScreen) mc.player.requestRespawn();
        }
    }

    public static class AutoEat extends Module {
        private final NumSetting below = add(new NumSetting("Hunger below", 14, 2, 19, 1));
        private boolean eating;
        private int prevSlot;

        public AutoEat() { super("AutoEat", "Eats food from your hotbar when hungry.", Category.PLAYER); }

        private static boolean isFood(ItemStack s) {
            return s.contains(DataComponentTypes.FOOD)
                    && !s.isOf(Items.GOLDEN_APPLE) && !s.isOf(Items.ENCHANTED_GOLDEN_APPLE)
                    && !s.isOf(Items.ROTTEN_FLESH) && !s.isOf(Items.SPIDER_EYE)
                    && !s.isOf(Items.POISONOUS_POTATO) && !s.isOf(Items.PUFFERFISH);
        }

        private void stop() {
            eating = false;
            Sp.release(mc.options.useKey);
            mc.player.getInventory().selectedSlot = prevSlot;
        }

        @Override public void onTick() {
            int food = mc.player.getHungerManager().getFoodLevel();
            if (eating) {
                if (food >= 20 || !isFood(mc.player.getMainHandStack())) stop();
                return;
            }
            if (food > below.value || mc.currentScreen != null) return;
            for (int i = 0; i < 9; i++) {
                if (isFood(mc.player.getInventory().getStack(i))) {
                    prevSlot = mc.player.getInventory().selectedSlot;
                    mc.player.getInventory().selectedSlot = i;
                    mc.options.useKey.setPressed(true);
                    eating = true;
                    return;
                }
            }
        }
        @Override public void onDisable() { if (eating && mc.player != null) stop(); }
    }

    public static class Scaffold extends Module {
        private final BoolSetting autoSwitch = add(new BoolSetting("Auto switch", true));
        public Scaffold() { super("Scaffold", "Places blocks under your feet as you walk.", Category.PLAYER); }

        @Override public void onTick() {
            BlockPos below = mc.player.getBlockPos().down();
            if (!mc.world.getBlockState(below).isReplaceable()) return;
            if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
                if (!autoSwitch.value) return;
                boolean found = false;
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem) {
                        mc.player.getInventory().selectedSlot = i; found = true; break;
                    }
                }
                if (!found) return;
            }
            for (Direction d : Direction.values()) {
                if (d == Direction.UP) continue;
                BlockPos n = below.offset(d);
                if (mc.world.getBlockState(n).isReplaceable()) continue;
                Vec3d hit = Vec3d.ofCenter(n).offset(d.getOpposite(), 0.5);
                BlockHitResult r = new BlockHitResult(hit, d.getOpposite(), n, false);
                if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, r).isAccepted())
                    mc.player.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
    }

    public static class ChestStealer extends Module {
        private final NumSetting delay = add(new NumSetting("Delay ticks", 1, 0, 10, 1));
        private final BoolSetting autoClose = add(new BoolSetting("Auto close", true));
        private int cd;

        public ChestStealer() { super("ChestStealer", "Moves everything out of an open chest.", Category.PLAYER); }

        @Override public void onTick() {
            if (!(mc.currentScreen instanceof GenericContainerScreen s)) return;
            if (cd-- > 0) return;
            cd = (int) delay.value;
            GenericContainerScreenHandler h = s.getScreenHandler();
            int n = h.getRows() * 9;
            for (int i = 0; i < n; i++) {
                if (h.getSlot(i).hasStack()) {
                    mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                    return;
                }
            }
            if (autoClose.value) mc.player.closeHandledScreen();
        }
    }

    public static class AntiAFK extends Module {
        private final NumSetting seconds = add(new NumSetting("Every (sec)", 20, 5, 120, 5));
        private final Random rng = new Random();
        private int t;

        public AntiAFK() { super("AntiAFK", "Jumps and turns now and then so you never idle out.", Category.PLAYER); }

        @Override public void onTick() {
            if (++t < seconds.value * 20) return;
            t = 0;
            if (mc.player.isOnGround()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.42, v.z);
            }
            mc.player.setYaw(mc.player.getYaw() + (rng.nextFloat() - 0.5f) * 20f);
        }
    }

    public static class AutoMine extends Module {
        public AutoMine() { super("AutoMine", "Holds left click for you.", Category.PLAYER); }
        @Override public void onTick() { mc.options.attackKey.setPressed(true); }
        @Override public void onDisable() { Sp.release(mc.options.attackKey); }
    }
}
