package com.dreaming.client.modules;

import com.dreaming.client.module.Category;
import com.dreaming.client.module.Module;
import com.dreaming.client.setting.BoolSetting;
import com.dreaming.client.setting.NumSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class MovementModules {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private MovementModules() {}

    private static boolean moving() {
        return MC.player.input.movementForward != 0 || MC.player.input.movementSideways != 0;
    }

    /** Horizontal velocity vector for the current WASD input and view yaw. */
    private static Vec3d horizontal(double speed) {
        float yaw = MC.player.getYaw() * 0.017453292f;
        double f = MC.player.input.movementForward, s = MC.player.input.movementSideways;
        double len = Math.max(1.0, Math.sqrt(f * f + s * s));
        f /= len; s /= len;
        return new Vec3d((-Math.sin(yaw) * f + Math.cos(yaw) * s) * speed, 0, (Math.cos(yaw) * f + Math.sin(yaw) * s) * speed);
    }

    public static class Sprint extends Module {
        public Sprint() { super("Sprint", "Always sprint while moving forward.", Category.MOVEMENT); }
        @Override public void onTick() {
            if (mc.player.input.movementForward > 0 && !mc.player.isSneaking() && !mc.player.horizontalCollision
                    && mc.player.getHungerManager().getFoodLevel() > 6) mc.player.setSprinting(true);
        }
    }

    public static class Flight extends Module {
        private final NumSetting speed = add(new NumSetting("Speed", 1.0, 0.5, 5, 0.1));
        public Flight() { super("Flight", "Fly freely. Space goes up, Shift goes down.", Category.MOVEMENT); }

        @Override public void onTick() {
            double s = speed.value;
            Vec3d h = horizontal(s * 0.5);
            double y = 0;
            if (mc.options.jumpKey.isPressed()) y += s * 0.4;
            if (mc.options.sneakKey.isPressed()) y -= s * 0.4;
            mc.player.setVelocity(moving() ? h.x : 0, y, moving() ? h.z : 0);
            mc.player.fallDistance = 0;
            Sp.run(p -> { p.fallDistance = 0; p.getAbilities().allowFlying = true; });
        }

        @Override public void onDisable() {
            if (mc.player != null && !mc.player.isCreative() && !mc.player.isSpectator())
                Sp.run(p -> p.getAbilities().allowFlying = false);
        }
    }

    public static class Speed extends Module {
        private final NumSetting speed = add(new NumSetting("Speed", 0.35, 0.2, 1.0, 0.05));
        public Speed() { super("Speed", "Moves you faster along the ground and in the air.", Category.MOVEMENT); }
        @Override public void onTick() {
            if (!moving() || mc.player.isSneaking()) return;
            Vec3d h = horizontal(speed.value), v = mc.player.getVelocity();
            mc.player.setVelocity(h.x, v.y, h.z);
        }
    }

    public static class Step extends Module {
        private final NumSetting height = add(new NumSetting("Height", 1.5, 0.6, 3, 0.1));
        private int cd;
        public Step() { super("Step", "Walk up blocks without jumping.", Category.MOVEMENT); }

        private static void set(PlayerEntity p, double h) {
            EntityAttributeInstance a = p.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
            if (a != null) a.setBaseValue(h);
        }
        @Override public void onTick() {
            set(mc.player, height.value);
            if (cd-- <= 0) { cd = 20; Sp.run(p -> set(p, height.value)); }
        }
        @Override public void onDisable() {
            if (mc.player == null) return;
            set(mc.player, 0.6);
            Sp.run(p -> set(p, 0.6));
        }
    }

    public static class NoFall extends Module {
        public NoFall() { super("NoFall", "Cancels fall damage.", Category.MOVEMENT); }
        @Override public void onTick() {
            mc.player.fallDistance = 0;
            Sp.run(p -> p.fallDistance = 0);
        }
    }

    public static class Spider extends Module {
        public Spider() { super("Spider", "Climb walls by pushing into them.", Category.MOVEMENT); }
        @Override public void onTick() {
            if (mc.player.horizontalCollision && !mc.player.isSneaking()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.2, v.z);
            }
        }
    }

    public static class Glide extends Module {
        private final NumSetting fall = add(new NumSetting("Fall speed", 0.05, 0.01, 0.2, 0.01));
        public Glide() { super("Glide", "Slows your fall to a gentle drift.", Category.MOVEMENT); }
        @Override public void onTick() {
            Vec3d v = mc.player.getVelocity();
            if (!mc.player.isOnGround() && v.y < -fall.value && !mc.player.isTouchingWater()) {
                mc.player.setVelocity(v.x, -fall.value, v.z);
                mc.player.fallDistance = 0;
            }
        }
    }

    public static class Jesus extends Module {
        public Jesus() { super("Jesus", "Float on top of water.", Category.MOVEMENT); }
        @Override public void onTick() {
            if (mc.player.isTouchingWater() && !mc.player.isSneaking()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.11, v.z);
            }
        }
    }

    public static class HighJump extends Module {
        private final NumSetting power = add(new NumSetting("Power", 0.8, 0.5, 2.0, 0.1));
        public HighJump() { super("HighJump", "Jump much higher.", Category.MOVEMENT); }
        @Override public void onTick() {
            Vec3d v = mc.player.getVelocity();
            if (mc.options.jumpKey.isPressed() && !mc.player.isOnGround() && Math.abs(v.y - 0.42) < 0.03)
                mc.player.setVelocity(v.x, power.value, v.z);
        }
    }

    public static class Bhop extends Module {
        public Bhop() { super("Bhop", "Automatically hops while you move.", Category.MOVEMENT); }
        @Override public void onTick() {
            if (moving() && mc.player.isOnGround() && !mc.player.isSneaking()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.42, v.z);
            }
        }
    }

    public static class AirJump extends Module {
        private boolean prev;
        public AirJump() { super("AirJump", "Jump again while in the air.", Category.MOVEMENT); }
        @Override public void onTick() {
            boolean down = mc.options.jumpKey.isPressed();
            if (down && !prev && !mc.player.isOnGround()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.42, v.z);
                mc.player.fallDistance = 0;
            }
            prev = down;
        }
    }

    public static class AutoWalk extends Module {
        public AutoWalk() { super("AutoWalk", "Holds forward for you.", Category.MOVEMENT); }
        @Override public void onTick() { mc.options.forwardKey.setPressed(true); }
        @Override public void onDisable() { Sp.release(mc.options.forwardKey); }
    }

    public static class FastClimb extends Module {
        public FastClimb() { super("FastClimb", "Climb ladders and vines faster.", Category.MOVEMENT); }
        @Override public void onTick() {
            if (mc.player.isClimbing() && mc.player.horizontalCollision) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.25, v.z);
            }
        }
    }
}
