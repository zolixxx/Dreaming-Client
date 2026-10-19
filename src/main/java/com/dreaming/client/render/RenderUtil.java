package com.dreaming.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Queues world-space boxes and lines during the frame and draws them all through walls in one flush. */
public final class RenderUtil {
    private record BoxEntry(Box box, int argb) {}
    private record LineEntry(Vec3d a, Vec3d b, int argb) {}
    private static final List<BoxEntry> BOXES = new ArrayList<>();
    private static final List<LineEntry> LINES = new ArrayList<>();

    private RenderUtil() {}

    public static void add(Box worldBox, int argb) { BOXES.add(new BoxEntry(worldBox, argb)); }
    public static void line(Vec3d a, Vec3d b, int argb) { LINES.add(new LineEntry(a, b, argb)); }

    public static void flush(WorldRenderContext ctx) {
        if (BOXES.isEmpty() && LINES.isEmpty()) return;

        Vec3d cam = ctx.camera().getPos();
        Matrix4f m = ctx.matrixStack().peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        if (!BOXES.isEmpty()) {
            BufferBuilder fill = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (BoxEntry e : BOXES) {
                Box b = e.box().offset(-cam.x, -cam.y, -cam.z);
                fillBox(fill, m, b, (0x2A << 24) | (e.argb() & 0xFFFFFF));
            }
            BufferRenderer.drawWithGlobalProgram(fill.end());
        }

        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (BoxEntry e : BOXES) {
            Box b = e.box().offset(-cam.x, -cam.y, -cam.z);
            lineBox(lines, m, b, 0xFF000000 | (e.argb() & 0xFFFFFF));
        }
        for (LineEntry l : LINES) {
            line(lines, m, l.a().x - cam.x, l.a().y - cam.y, l.a().z - cam.z,
                    l.b().x - cam.x, l.b().y - cam.y, l.b().z - cam.z, 0xFF000000 | (l.argb() & 0xFFFFFF));
        }
        BufferRenderer.drawWithGlobalProgram(lines.end());

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        BOXES.clear();
        LINES.clear();
    }

    private static void v(BufferBuilder b, Matrix4f m, double x, double y, double z, int argb) {
        b.vertex(m, (float) x, (float) y, (float) z)
                .color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255);
    }

    private static void fillBox(BufferBuilder b, Matrix4f m, Box bx, int c) {
        double x0 = bx.minX, y0 = bx.minY, z0 = bx.minZ, x1 = bx.maxX, y1 = bx.maxY, z1 = bx.maxZ;
        v(b, m, x0, y0, z0, c); v(b, m, x1, y0, z0, c); v(b, m, x1, y0, z1, c); v(b, m, x0, y0, z1, c);
        v(b, m, x0, y1, z0, c); v(b, m, x0, y1, z1, c); v(b, m, x1, y1, z1, c); v(b, m, x1, y1, z0, c);
        v(b, m, x0, y0, z0, c); v(b, m, x0, y1, z0, c); v(b, m, x1, y1, z0, c); v(b, m, x1, y0, z0, c);
        v(b, m, x0, y0, z1, c); v(b, m, x1, y0, z1, c); v(b, m, x1, y1, z1, c); v(b, m, x0, y1, z1, c);
        v(b, m, x0, y0, z0, c); v(b, m, x0, y0, z1, c); v(b, m, x0, y1, z1, c); v(b, m, x0, y1, z0, c);
        v(b, m, x1, y0, z0, c); v(b, m, x1, y1, z0, c); v(b, m, x1, y1, z1, c); v(b, m, x1, y0, z1, c);
    }

    private static void line(BufferBuilder b, Matrix4f m, double ax, double ay, double az,
                             double bx, double by, double bz, int c) {
        v(b, m, ax, ay, az, c);
        v(b, m, bx, by, bz, c);
    }

    private static void lineBox(BufferBuilder b, Matrix4f m, Box bx, int c) {
        double x0 = bx.minX, y0 = bx.minY, z0 = bx.minZ, x1 = bx.maxX, y1 = bx.maxY, z1 = bx.maxZ;
        line(b, m, x0, y0, z0, x1, y0, z0, c); line(b, m, x1, y0, z0, x1, y0, z1, c);
        line(b, m, x1, y0, z1, x0, y0, z1, c); line(b, m, x0, y0, z1, x0, y0, z0, c);
        line(b, m, x0, y1, z0, x1, y1, z0, c); line(b, m, x1, y1, z0, x1, y1, z1, c);
        line(b, m, x1, y1, z1, x0, y1, z1, c); line(b, m, x0, y1, z1, x0, y1, z0, c);
        line(b, m, x0, y0, z0, x0, y1, z0, c); line(b, m, x1, y0, z0, x1, y1, z0, c);
        line(b, m, x1, y0, z1, x1, y1, z1, c); line(b, m, x0, y0, z1, x0, y1, z1, c);
    }
}
