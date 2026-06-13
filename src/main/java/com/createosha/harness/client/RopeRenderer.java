package com.createosha.harness.client;

import com.createosha.harness.CreateOSHAHarness;
import com.createosha.harness.harness.ConnectionType;
import com.createosha.harness.harness.PlayerRopeState;
import com.createosha.harness.harness.RopeConnection;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws a simple rope line from the player's waist to each active anchor point.
 */
@EventBusSubscriber(modid = CreateOSHAHarness.MODID, value = Dist.CLIENT)
public final class RopeRenderer {

    private RopeRenderer() {}

    /** Rope colour: brown-ish, fully opaque. */
    private static final float R = 0.55f, G = 0.38f, B = 0.14f, A = 1.0f;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        PlayerRopeState state = ClientEventHandler.getLocalRopeState();
        if (state == null || state.connectionCount() == 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vec3 cameraPos = event.getCamera().getPosition();

        // Player waist is ~0.9 blocks above feet (half of 1.8-block player height)
        Vec3 waist = mc.player.position().add(0, 0.9, 0);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
        PoseStack poseStack = event.getPoseStack();
        PoseStack.Pose pose = poseStack.last();

        for (RopeConnection conn : state.getConnections()) {
            Vec3 anchor = resolveClientAnchor(conn, mc);
            if (anchor == null) continue;

            drawLine(lines, pose, cameraPos, waist, anchor);
        }

        bufferSource.endBatch(RenderType.lines());
    }

    private static void drawLine(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 cameraPos,
            Vec3 from,
            Vec3 to) {

        float x1 = (float)(from.x - cameraPos.x);
        float y1 = (float)(from.y - cameraPos.y);
        float z1 = (float)(from.z - cameraPos.z);
        float x2 = (float)(to.x   - cameraPos.x);
        float y2 = (float)(to.y   - cameraPos.y);
        float z2 = (float)(to.z   - cameraPos.z);

        // Direction normal (both vertices share the same normal for a straight line)
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;

        consumer.addVertex(pose.pose(), x1, y1, z1)
                .setColor(R, G, B, A)
                .setNormal(pose, nx, ny, nz);

        consumer.addVertex(pose.pose(), x2, y2, z2)
                .setColor(R, G, B, A)
                .setNormal(pose, nx, ny, nz);
    }

    private static Vec3 resolveClientAnchor(RopeConnection conn, Minecraft mc) {
        if (conn.type() == ConnectionType.ENTITY) {
            // We don't track other player positions client-side — skip rendering entity connections.
            return null;
        }
        if (conn.anchorBlock() == null) return null;
        return Vec3.atCenterOf(conn.anchorBlock());
    }
}
