package com.dreaming.client.modules;

import com.dreaming.client.module.Category;
import com.dreaming.client.module.Module;
import com.dreaming.client.render.RenderUtil;
import com.dreaming.client.setting.BoolSetting;
import com.dreaming.client.setting.NumSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class EspModule extends Module {
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting hostiles = add(new BoolSetting("Hostiles", true));
    private final BoolSetting passives = add(new BoolSetting("Passives", false));
    private final BoolSetting items = add(new BoolSetting("Items", false));
    private final NumSetting range = add(new NumSetting("Range", 64, 8, 256, 8));

    public EspModule() {
        super("ESP", "Boxes around entities, visible through walls.", Category.RENDER);
    }

    @Override
    public void onWorldRender(WorldRenderContext ctx) {
        float td = ctx.tickCounter().getTickDelta(false);
        Vec3d cam = ctx.camera().getPos();
        double r2 = range.value * range.value;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || e.squaredDistanceTo(cam) > r2) continue;
            int color = colorFor(e);
            if (color == 0) continue;

            Vec3d l = e.getLerpedPos(td);
            Box bb = e.getBoundingBox().offset(l.x - e.getX(), l.y - e.getY(), l.z - e.getZ());
            RenderUtil.add(bb, color);
        }
    }

    /** Returns 0 if this entity type is filtered out. */
    private int colorFor(Entity e) {
        if (e instanceof PlayerEntity) return players.value ? 0x00E5FF : 0;
        if (e instanceof HostileEntity) return hostiles.value ? 0xFF4D4D : 0;
        if (e instanceof ItemEntity) return items.value ? 0xFFD84D : 0;
        if (e instanceof LivingEntity && !(e instanceof ArmorStandEntity)) return passives.value ? 0x5CFF7A : 0;
        return 0;
    }
}
