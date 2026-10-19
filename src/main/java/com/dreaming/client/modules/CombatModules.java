package com.dreaming.client.modules;

import com.dreaming.client.module.Category;
import com.dreaming.client.module.Module;
import com.dreaming.client.setting.BoolSetting;
import com.dreaming.client.setting.NumSetting;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

public final class CombatModules {
    private CombatModules() {}

    public static class KillAura extends Module {
        private final NumSetting range = add(new NumSetting("Range", 3.8, 2, 6, 0.1));
        private final BoolSetting hostiles = add(new BoolSetting("Hostiles", true));
        private final BoolSetting passives = add(new BoolSetting("Passives", false));
        private final BoolSetting rotate = add(new BoolSetting("Face target", true));

        public KillAura() { super("KillAura", "Attacks nearby mobs when your cooldown is ready.", Category.COMBAT); }

        @Override
        public void onTick() {
            if (mc.currentScreen != null || mc.player.getAttackCooldownProgress(0.5f) < 1f) return;
            Entity best = null;
            double bd = range.value * range.value;
            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player || !(e instanceof LivingEntity le) || !le.isAlive() || e instanceof ArmorStandEntity) continue;
                boolean hostile = e instanceof HostileEntity;
                if (e instanceof PlayerEntity) continue;
                if (hostile ? !hostiles.value : !passives.value) continue;
                double d = mc.player.squaredDistanceTo(e);
                if (d < bd) { bd = d; best = e; }
            }
            if (best == null) return;
            if (rotate.value) face(best);
            mc.interactionManager.attackEntity(mc.player, best);
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        private void face(Entity e) {
            Vec3d t = e.getBoundingBox().getCenter(), eye = mc.player.getEyePos();
            double dx = t.x - eye.x, dy = t.y - eye.y, dz = t.z - eye.z;
            double h = Math.sqrt(dx * dx + dz * dz);
            mc.player.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90));
            mc.player.setPitch((float) -Math.toDegrees(Math.atan2(dy, h)));
        }
    }

    public static class TriggerBot extends Module {
        private final BoolSetting hostilesOnly = add(new BoolSetting("Hostiles only", true));

        public TriggerBot() { super("TriggerBot", "Attacks whatever your crosshair is on.", Category.COMBAT); }

        @Override
        public void onTick() {
            if (mc.currentScreen != null || mc.player.getAttackCooldownProgress(0.5f) < 1f) return;
            if (mc.crosshairTarget instanceof EntityHitResult r && r.getEntity() instanceof LivingEntity le && le.isAlive()) {
                if (hostilesOnly.value && !(le instanceof HostileEntity)) return;
                mc.interactionManager.attackEntity(mc.player, le);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    public static class Reach extends Module {
        private final NumSetting block = add(new NumSetting("Block reach", 6, 4.5, 8, 0.5));
        private final NumSetting entity = add(new NumSetting("Entity reach", 5, 3, 8, 0.5));
        private int cd;

        public Reach() { super("Reach", "Extends how far you can hit and mine.", Category.COMBAT); }

        private static void apply(PlayerEntity p, double b, double e) {
            EntityAttributeInstance bi = p.getAttributeInstance(EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE);
            if (bi != null) bi.setBaseValue(b);
            EntityAttributeInstance ei = p.getAttributeInstance(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE);
            if (ei != null) ei.setBaseValue(e);
        }

        @Override
        public void onTick() {
            apply(mc.player, block.value, entity.value);
            if (cd-- <= 0) { cd = 20; Sp.run(p -> apply(p, block.value, entity.value)); }
        }

        @Override
        public void onDisable() {
            if (mc.player == null) return;
            apply(mc.player, 4.5, 3.0);
            Sp.run(p -> apply(p, 4.5, 3.0));
        }
    }

    public static class AutoTotem extends Module {
        private final NumSetting delay = add(new NumSetting("Delay ticks", 4, 0, 20, 1));
        private int cd;

        public AutoTotem() { super("AutoTotem", "Keeps a totem in your offhand.", Category.COMBAT); }

        @Override
        public void onTick() {
            if (cd > 0) { cd--; return; }
            if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
            if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;
            ScreenHandler h = mc.player.playerScreenHandler;
            for (int i = 9; i <= 44; i++) {
                if (h.getSlot(i).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                    mc.interactionManager.clickSlot(h.syncId, i, 40, SlotActionType.SWAP, mc.player);
                    cd = (int) delay.value;
                    return;
                }
            }
        }
    }

    public static class AutoArmor extends Module {
        private int cd;

        public AutoArmor() { super("AutoArmor", "Equips armor from your inventory into empty slots.", Category.COMBAT); }

        private static int armorIdx(EquipmentSlot s) {
            return switch (s) { case HEAD -> 5; case CHEST -> 6; case LEGS -> 7; case FEET -> 8; default -> -1; };
        }

        @Override
        public void onTick() {
            if (cd-- > 0) return;
            if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;
            ScreenHandler h = mc.player.playerScreenHandler;
            for (int idx = 5; idx <= 8; idx++) {
                if (h.getSlot(idx).hasStack()) continue;
                for (int i = 9; i <= 44; i++) {
                    ItemStack st = h.getSlot(i).getStack();
                    if (st.getItem() instanceof ArmorItem ai && armorIdx(ai.getSlotType()) == idx) {
                        mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                        cd = 6;
                        return;
                    }
                }
            }
        }
    }
}
