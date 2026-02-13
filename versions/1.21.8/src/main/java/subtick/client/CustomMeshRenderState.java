package subtick.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public record CustomMeshRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        Vector2f[] vertices,
        int color,
        @Nullable ScreenRectangle scissorArea
) implements GuiElementRenderState {

    @Override
    public void buildVertices(VertexConsumer vertexConsumer, float z) {
        for (int i = 0; i < 4; i++) {
            Vector2f v = (i < vertices.length) ? vertices[i] : vertices[vertices.length - 1];
            vertexConsumer.addVertexWith2DPose(this.pose, v.x, v.y, z).setColor(this.color);
        }
    }

    @Override
    public ScreenRectangle bounds() {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (Vector2f v : vertices) {
            minX = Math.min(minX, v.x);
            minY = Math.min(minY, v.y);
            maxX = Math.max(maxX, v.x);
            maxY = Math.max(maxY, v.y);
        }

        return new ScreenRectangle((int)minX, (int)minY, (int)(maxX - minX), (int)(maxY - minY));
    }

    @Override
    public @NotNull RenderPipeline pipeline() { return pipeline; }

    @Override
    public @NotNull TextureSetup textureSetup() { return textureSetup; }

    @Override
    @Nullable
    public ScreenRectangle scissorArea() { return scissorArea; }
}