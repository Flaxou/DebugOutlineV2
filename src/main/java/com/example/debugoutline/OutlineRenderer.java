package com.example.debugoutline;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

/**
 * Dessine les contours et les tracers, sans test de profondeur (visibles à travers les murs).
 * Les coordonnées sont relatives à la caméra : on décale la pile de matrices de -cam.
 */
public final class OutlineRenderer {

    /** Layer de lignes identique à la vanille, mais sans test de profondeur. */
    static final RenderLayer XRAY_LAYER = RenderLayer.of(
            "debugoutline_xray",
            VertexFormats.LINES,
            VertexFormat.DrawMode.LINES,
            1536,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.LINES_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .target(RenderPhase.ITEM_ENTITY_TARGET)
                    .writeMaskState(RenderPhase.ALL_MASK)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .build(false)
    );

    private OutlineRenderer() {}

    /** Retourne le nombre de conteneurs dessinés (diagnostic). */
    public static int render(WorldRenderContext ctx, Set<ContainerType> enabled, boolean tracers, double maxDistance) {
        if (enabled.isEmpty() || ContainerCache.size() == 0) {
            return 0;
        }

        MatrixStack matrices = ctx.matrixStack();
        Vec3d cam = ctx.camera().getPos();
        double maxSq = maxDistance * maxDistance;
        int[] drawn = {0};

        VertexConsumerProvider.Immediate consumers =
                MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buf = consumers.getBuffer(XRAY_LAYER);

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        ContainerCache.forEach((pos, type) -> {
            if (!enabled.contains(type)) {
                return;
            }
            double dx = pos.getX() + 0.5 - cam.x;
            double dy = pos.getY() + 0.5 - cam.y;
            double dz = pos.getZ() + 0.5 - cam.z;
            if (dx * dx + dy * dy + dz * dz > maxSq) {
                return;
            }

            WorldRenderer.drawBox(matrices, buf, new Box(pos), type.r, type.g, type.b, 1.0f);
            drawn[0]++;
            if (tracers) {
                drawTracer(matrices, buf, cam, pos, type);
            }
        });

        matrices.pop();
        consumers.draw(XRAY_LAYER);
        return drawn[0];
    }

    private static void drawTracer(MatrixStack matrices, VertexConsumer buf, Vec3d cam, BlockPos pos, ContainerType type) {
        MatrixStack.Entry entry = matrices.peek();
        float x2 = pos.getX() + 0.5f;
        float y2 = pos.getY() + 0.5f;
        float z2 = pos.getZ() + 0.5f;

        float dx = (float) (x2 - cam.x);
        float dy = (float) (y2 - cam.y);
        float dz = (float) (z2 - cam.z);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0f) {
            return;
        }
        float nx = dx / len, ny = dy / len, nz = dz / len;
        int color = type.argb();

        buf.vertex(entry.getPositionMatrix(), (float) cam.x, (float) cam.y, (float) cam.z)
                .color(color)
                .normal(entry, nx, ny, nz);
        buf.vertex(entry.getPositionMatrix(), x2, y2, z2)
                .color(color)
                .normal(entry, nx, ny, nz);
    }
}
