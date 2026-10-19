package com.dreaming.client.modules;

import com.dreaming.client.module.Category;
import com.dreaming.client.module.Module;
import com.dreaming.client.setting.BoolSetting;
import com.dreaming.client.setting.NumSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class NametagsModule extends Module {
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", false));
    private final BoolSetting health = add(new BoolSetting("Health", true));
    private final BoolSetting distance = add(new BoolSetting("Distance", true));
    private final NumSetting scale = add(new NumSetting("Scale", 1.0, 0.5, 3.0, 0.1));
    private final NumSetting range = add(new NumSetting("Range", 64, 8, 256, 8));

    public NametagsModule() {
        super("Nametags", "Large name labels with health, visible through walls.", Category.RENDER);
    }

    @Override
    public void onWorldRender(WorldRenderContext ctx) {
        float td = ctx.tickCounter().getTickDelta(false);
        Vec3d cam = ctx.camera().getPos();
        double r2 = range.value * range.value;
        TextRenderer tr = mc.textRenderer;
        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider.Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity le) || e instanceof ArmorStandEntity) continue;
            boolean isPlayer = e instanceof PlayerEntity;
            if (isPlayer ? !players.value : !mobs.value) continue;
            if (e.squaredDistanceTo(cam) > r2) continue;

            Vec3d p = e.getLerpedPos(td);
            double dist = cam.distanceTo(p);

            StringBuilder sb = new StringBuilder(e.getDisplayName().getString());
            if (health.value) sb.append(String.format(" %.1f\u2764", le.getHealth() + le.getAbsorptionAmount()));
            if (distance.value) sb.append(String.format(" [%.0fm]", dist));
            String text = sb.toString();

            int color = isPlayer ? 0xFFFFFFFF : (e instanceof HostileEntity ? 0xFFFF7777 : 0xFFAAFFAA);
            float s = (float) (Math.max(1.0, dist * 0.35) * 0.025 * scale.value);

            ms.push();
            ms.translate(p.x - cam.x, p.y + e.getHeight() + 0.45 - cam.y, p.z - cam.z);
            ms.multiply(ctx.camera().getRotation());
            ms.scale(-s, -s, s);
            float w = tr.getWidth(text);
            tr.draw(text, -w / 2f, 0, color, false, ms.peek().getPositionMatrix(), imm,
                    TextRenderer.TextLayerType.SEE_THROUGH, 0x90000000,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE);
            ms.pop();
        }
        imm.draw();
    }
}
