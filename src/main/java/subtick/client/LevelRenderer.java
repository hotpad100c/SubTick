package subtick.client;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
//#if MC < 11900
import com.mojang.math.Quaternion;
//#endif
import com.mojang.math.Transformation;

import fi.dy.masa.malilib.util.Color4f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
//#if MC >= 11900
//$$ import org.joml.Matrix4fStack;
//$$ import org.joml.Quaternionf;
//#endif

public class LevelRenderer
{
  private static final Minecraft mc = Minecraft.getInstance();
  private static final Font font = mc.font;
  private static final HashSet<Pos> hlPos = new HashSet<>();
  private static final HashSet<Text> texts = new HashSet<>();

  public static synchronized void render(PoseStack poseStack, OutlineBufferSource outlineBufferSource, boolean renderText)
  {
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.disableDepthTest();
    Camera camera = mc.gameRenderer.getMainCamera();
    Vec3 cpos = camera.getPosition();

    Tesselator tesselator = Tesselator.getInstance();
    BufferBuilder buffer = tesselator.getBuilder();
    if (!renderText) {
      Map<Integer, List<Outline>> groupedOutlines = hlPos.stream()
              .filter(p -> p instanceof Outline)
              .map(o -> (Outline) o)
              .collect(Collectors.groupingBy(o -> o.color().intValue));

      for (Map.Entry<Integer, List<Outline>> entry : groupedOutlines.entrySet()) {
        int color = entry.getKey();
        setOutlineColor(outlineBufferSource, color);
        for (Outline o : entry.getValue()) {
          o.render(poseStack, camera, outlineBufferSource, mc.level);
        }
        outlineBufferSource.endOutlineBatch();
      }
    } else {
      //#if MC < 12006
      poseStack = RenderSystem.getModelViewStack();
      //#endif
      //#if MC >= 11900
      //$$ Quaternionf rot = camera.rotation();
      //#else
      Quaternion rot = camera.rotation();
      //#endif
      for (Text text : texts)
        text.render(buffer, poseStack, rot, cpos.x, cpos.y, cpos.z);
    }
  }

  public static synchronized void clear()
  {
    hlPos.clear();
    texts.clear();
  }

  public static synchronized boolean hasOutline(){
    return !hlPos.isEmpty();
  }

  public static synchronized void addOutline(BlockPos pos, Color4f color) {
    Outline o = new Outline(pos, color);
    if(!hlPos.add(o)) {
      hlPos.remove(o);
      hlPos.add(o);
    }
  }

  public static synchronized void addText(String text, int x, int y, int z, Color4f color)
  {
    TextBasic o = new TextBasic(text, x + 0.5, y + 0.5, z + 0.5, color);
    if(!texts.add(o))
    {
      texts.remove(o);
      texts.add(o);
    }
  }

  public static synchronized void addLabel(int index, int depth, int x, int y, int z, Color4f color1, Color4f color2)
  {
    DepthLabel o = new DepthLabel(String.valueOf(index), String.valueOf(depth), x + 0.5, y + 0.5, z + 0.5, color1.intValue, color2.intValue);
    if(!texts.add(o))
    {
      texts.remove(o);
      texts.add(o);
    }
  }

  private static interface Pos
  {
    public void render(PoseStack poseStack, Camera camera, OutlineBufferSource outlineBufferSource, Level level);
  }

  private static record Outline(BlockPos pos, Color4f color) implements Pos
  {
    @Override
    public boolean equals(Object b)
    {
      return b instanceof Outline o && o.pos.getX() == pos.getX() && o.pos.getY() == pos.getY() && o.pos.getZ() == pos.getZ();
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(pos);
    }

    @Override
    public void render(PoseStack poseStack, Camera camera, OutlineBufferSource outlineBufferSource, Level level)
    {
      BlockState state = level.getBlockState(pos);
      BlockRenderDispatcher blockRenderManager = mc.getBlockRenderer();
      BlockEntity blockEntity = level.getBlockEntity(pos);
      poseStack.pushPose();
      Vec3 cpos = camera.getPosition();
      poseStack.translate(pos.getX() - cpos.x, pos.getY() - cpos.y, pos.getZ() - cpos.z);

      if (blockEntity != null) {
        BlockEntityRenderDispatcher blockEntityRenderDispatcher = mc.getBlockEntityRenderDispatcher();
        blockEntityRenderDispatcher.render(blockEntity, 0.0f, poseStack, outlineBufferSource);
      } else {
        if (state.getRenderShape() != RenderShape.MODEL) {
          poseStack.popPose();
          return;
        }
        BakedModel model = blockRenderManager.getBlockModel(state);
        VertexConsumer vertexConsumer = outlineBufferSource.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        InvisibleVertexConsumer invisibleConsumer = new InvisibleVertexConsumer(vertexConsumer);
        blockRenderManager.getModelRenderer().renderModel(
                poseStack.last(),
                invisibleConsumer,
                state,
                model,
                color.r, color.g, color.b,
                net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos),
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
      }
    }
  }

  public static void setOutlineColor(OutlineBufferSource outlineProvider, int color) {
    int red = (color >> 16) & 0xFF;
    int green = (color >> 8) & 0xFF;
    int blue = color & 0xFF;
    int alpha = (color >> 24) & 0xFF;

    if (alpha == 0) {
      alpha = 255;
    }

    outlineProvider.setColor(red, green, blue, alpha);
  }

  private static class InvisibleVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;

    public InvisibleVertexConsumer(VertexConsumer delegate) {
      this.delegate = delegate;
    }

    @Override
    public @NotNull VertexConsumer vertex(double d, double e, double f) {
      this.delegate.vertex(d, e, f);
      return this;
    }

    @Override
    public @NotNull VertexConsumer color(int red, int green, int blue, int alpha) {
      this.delegate.color(red, green, blue, 0);
      return this;
    }

    @Override
    public @NotNull VertexConsumer uv(float u, float v) {
      this.delegate.uv(u, v);
      return this;
    }

    @Override
    public @NotNull VertexConsumer overlayCoords(int u, int v) {
      this.delegate.overlayCoords(u, v);
      return this;
    }

    @Override
    public @NotNull VertexConsumer uv2(int u, int v) {
      this.delegate.uv2(u, v);
      return this;
    }

    @Override
    public @NotNull VertexConsumer normal(float x, float y, float z) {
     this.delegate.normal(x, y, z);
      return this;
    }

    @Override
    public void endVertex() {
      this.delegate.endVertex();
    }

    @Override
    public void defaultColor(int i, int j, int k, int l) {
      this.delegate.defaultColor(i, j, k, l);
    }

    @Override
    public void unsetDefaultColor() {
      this.delegate.unsetDefaultColor();
    }
  }

  private static interface Text
  {
    //#if MC >= 11900
    //$$ public void render(BufferBuilder builder, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz);
    //#else
    public void render(BufferBuilder buffer, PoseStack poseStack, Quaternion rotation, double cx, double cy, double cz);
    //#endif
  }

  private static record TextBasic(String text, double x, double y, double z, Color4f color) implements Text
  {
    @Override
    public boolean equals(Object b)
    {
      return b instanceof TextBasic o && o.x == x && o.y == y && o.z == z;
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(x, y, z);
    }

    @Override
    //#if MC >= 11900
    //$$ public void render(BufferBuilder buffer, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz)
    //#else
    public void render(BufferBuilder buffer, PoseStack poseStack, Quaternion rotation, double cx, double cy, double cz)
    //#endif
    {
      poseStack.pushPose();
      poseStack.translate((float)(x - cx), (float)(y - cy), (float)(z - cz));
      poseStack.mulPose(rotation);
      poseStack.scale(-0.07F, -0.07F, 0.07F);
      RenderSystem.applyModelViewMatrix();
      MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(buffer);
      //#if MC >= 12006
      //$$ font.drawInBatch(text, -font.width(text)/2F, -font.lineHeight * 0.5F, color.intValue, false, poseStack.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      //#if MC >= 11904
      //$$ font.drawInBatch(text, -font.width(text)/2F, -font.lineHeight * 0.5F, color.intValue, false, Transformation.identity().getMatrix(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      font.drawInBatch(text, -font.width(text)/2F, -font.lineHeight * 0.5F, color.intValue, false, Transformation.identity().getMatrix(), immediate, true, 0x00000000, 0x00000000);
      //#endif
      //#endif
      immediate.endBatch();
      poseStack.popPose();
    }
  }

  private static record DepthLabel(String index, String depth, double x, double y, double z, int color1, int color2) implements Text
  {
    @Override
    public boolean equals(Object b)
    {
      return b instanceof DepthLabel o && o.x == x && o.y == y && o.z == z;
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(x, y, z);
    }

    @Override
    //#if MC >= 11900
    //$$ public void render(BufferBuilder buffer, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz)
    //#else
    public void render(BufferBuilder buffer, PoseStack poseStack, Quaternion rotation, double cx, double cy, double cz)
    //#endif
    {
      poseStack.pushPose();
      poseStack.translate((float)(x - cx), (float)(y - cy), (float)(z - cz));
      poseStack.mulPose(rotation);
      poseStack.scale(-0.07F, -0.07F, 0.08F);
      RenderSystem.applyModelViewMatrix();
      MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(buffer);
      //#if MC >= 12006
      //$$ font.drawInBatch(index, -font.width(index)/2F, -font.lineHeight * 0.5F, color1, false, poseStack.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      //#if MC >= 11904
      //$$ font.drawInBatch(index, -font.width(index)/2F, -font.lineHeight * 0.5F, color1, false, Transformation.identity().getMatrix(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      font.drawInBatch(index, -font.width(index)/2F, -font.lineHeight * 0.5F, color1, false, Transformation.identity().getMatrix(), immediate, true, 0x00000000, 0x00000000);
      //#endif
      //#endif
      immediate.endBatch();

      poseStack.translate(font.width(index)/2F, 0, 0);
      poseStack.scale(0.5F, 0.5F, 0.5F);
      RenderSystem.applyModelViewMatrix();
      immediate = MultiBufferSource.immediate(buffer);
      //#if MC >= 12006
      //$$ font.drawInBatch(depth, -font.width(depth)/2F, font.lineHeight + 1, color2, false, poseStack.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      //#if MC >= 11904
      //$$ font.drawInBatch(depth, -font.width(depth)/2F, font.lineHeight + 1, color2, false, Transformation.identity().getMatrix(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      //#else
      font.drawInBatch(depth, -font.width(depth)/2F, font.lineHeight + 1, color2, false, Transformation.identity().getMatrix(), immediate, true, 0x00000000, 0x00000000);
      //#endif
      //#endif
      immediate.endBatch();

      poseStack.popPose();
    }
  }
}