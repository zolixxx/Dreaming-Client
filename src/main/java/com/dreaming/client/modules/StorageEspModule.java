package com.dreaming.client.modules;

import com.dreaming.client.module.Category;
import com.dreaming.client.module.Module;
import com.dreaming.client.render.RenderUtil;
import com.dreaming.client.setting.BoolSetting;
import com.dreaming.client.setting.NumSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.entity.*;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.chunk.WorldChunk;

public class StorageEspModule extends Module {
    private final BoolSetting chests = add(new BoolSetting("Chests", true));
    private final BoolSetting trapped = add(new BoolSetting("Trapped", true));
    private final BoolSetting ender = add(new BoolSetting("Ender", true));
    private final BoolSetting barrels = add(new BoolSetting("Barrels", true));
    private final BoolSetting shulkers = add(new BoolSetting("Shulkers", true));
    private final BoolSetting furnaces = add(new BoolSetting("Furnaces", false));
    private final BoolSetting misc = add(new BoolSetting("Hopper/Disp", false));
    private final NumSetting range = add(new NumSetting("Range", 64, 16, 256, 16));

    public StorageEspModule() {
        super("StorageESP", "Highlights containers through walls.", Category.RENDER);
    }

    @Override
    public void onWorldRender(WorldRenderContext ctx) {
        ChunkPos pc = mc.player.getChunkPos();
        int cr = (int) Math.ceil(range.value / 16.0);
        double r2 = range.value * range.value;

        for (int dx = -cr; dx <= cr; dx++) {
            for (int dz = -cr; dz <= cr; dz++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(pc.x + dx, pc.z + dz);
                if (chunk == null) continue;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getPos();
                    double ddx = pos.getX() + 0.5 - mc.player.getX(), ddy = pos.getY() + 0.5 - mc.player.getY(), ddz = pos.getZ() + 0.5 - mc.player.getZ();
                    if (ddx * ddx + ddy * ddy + ddz * ddz > r2) continue;

                    int color = colorFor(be);
                    if (color == 0) continue;

                    BlockState state = mc.world.getBlockState(pos);
                    VoxelShape shape = state.getOutlineShape(mc.world, pos);
                    Box box = shape.isEmpty() ? new Box(pos) : shape.getBoundingBox().offset(pos);
                    RenderUtil.add(box, color);
                }
            }
        }
    }

    private int colorFor(BlockEntity be) {
        // TrappedChestBlockEntity extends ChestBlockEntity, so check it first
        if (be instanceof TrappedChestBlockEntity) return trapped.value ? 0xFF5555 : 0;
        if (be instanceof ChestBlockEntity) return chests.value ? 0xFFAA00 : 0;
        if (be instanceof EnderChestBlockEntity) return ender.value ? 0xB45CFF : 0;
        if (be instanceof BarrelBlockEntity) return barrels.value ? 0xC98A4B : 0;
        if (be instanceof ShulkerBoxBlockEntity) return shulkers.value ? 0xFF6EF0 : 0;
        if (be instanceof AbstractFurnaceBlockEntity) return furnaces.value ? 0xAAAAAA : 0;
        if (be instanceof HopperBlockEntity || be instanceof DispenserBlockEntity) return misc.value ? 0x55FFFF : 0;
        return 0;
    }
}
