package de.maexle.skyblockcounter.waypoint;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import static net.minecraft.client.gl.RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET;

public class PrimitiveCollector {

    private final VertexConsumerProvider vertexConsumers;
    private final MatrixStack matrices;
    private final Vec3d cameraPos;


    public PrimitiveCollector(
            VertexConsumerProvider vertexConsumers,
            MatrixStack matrices,
            Vec3d cameraPos
    ) {
        this.vertexConsumers = vertexConsumers;
        this.matrices = matrices;
        this.cameraPos = cameraPos;
    }

    public static final RenderPipeline CUSTOM_DEBUG_POINTS =
            RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/debug_points")
                    .withVertexShader("core/debug_point")
                    .withFragmentShader("core/position_color")
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.POINTS)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build();

    public void renderPoint(BlockPos pos) {
        VertexConsumer consumer =
                vertexConsumers.getBuffer(
                        RenderLayer.of(
                                "skycounter_debug_points",
                                RenderSetup.builder(CUSTOM_DEBUG_POINTS)
                                        .build()
                        )
                );


        Matrix4f matrix = matrices.peek().getPositionMatrix();


        float x = (float)(pos.getX() - cameraPos.x) + (float)0.5;
        float y = (float)(pos.getY() - cameraPos.y) + (float)0.5;
        float z = (float)(pos.getZ() - cameraPos.z) + (float)0.5;

        consumer.vertex(matrix, x, y, z)
                .color(0f,1f, 0f, 1f)
                .normal(matrices.peek(), 0, 1,0)
                .lineWidth(15f);
    }


    public void draw() {
        if(vertexConsumers instanceof VertexConsumerProvider.Immediate immediate)
        {
            immediate.draw();
        }
    }
}