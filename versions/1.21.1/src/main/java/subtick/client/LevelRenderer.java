package subtick.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
//#if MC >= 12105
//$$ import fi.dy.masa.malilib.util.data.Color4f;
//#else
import fi.dy.masa.malilib.util.Color4f;
//#endif
//#if MC >= 12110
//#if MC >= 12111
//$$ import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
//$$ import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
//#else
//$$ import subtick.client.substitute.WorldRenderContext;
//$$ import subtick.client.substitute.WorldRenderEvents;
//#endif
//#else
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
//#endif
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.HashSet;
import java.util.Objects;
//#if MC >= 12103
//#if MC < 12105
//$$import net.minecraft.client.renderer.CoreShaders;
//#endif
//#endif

public class LevelRenderer
{
  //#if MC >= 12105
  //$$ private static final com.mojang.blaze3d.pipeline.RenderPipeline WORLD_QUAD_PIPELINE = com.mojang.blaze3d.pipeline.RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.DEBUG_FILLED_SNIPPET)
  //$$         .withLocation(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("subtick", "world_quads"))
  //$$         .withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST)
  //$$         .withDepthWrite(false)
  //$$         .withCull(false)
  //$$         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
  //$$         .build();
  //$$ public static final net.minecraft.client.renderer.RenderType WORLD_QUADS = net.minecraft.client.renderer.RenderType.create(
  //$$         "subtick_world_quads",
  //#if MC >= 12111
  //$$ net.minecraft.client.renderer.rendertype.RenderSetup.builder(WORLD_QUAD_PIPELINE)
  //$$                .affectsCrumbling()
  //$$                .sortOnUpload()
  //$$                .bufferSize(256)
  //$$                .createRenderSetup()
  //#else
  //$$ 256, false, true, WORLD_QUAD_PIPELINE,
  //$$         net.minecraft.client.renderer.RenderType.CompositeState.builder()
  //$$                 .createCompositeState(false)
  //#endif
  //$$ );
  //$$ private static final com.mojang.blaze3d.pipeline.RenderPipeline WORLD_LINE_PIPELINE = com.mojang.blaze3d.pipeline.RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.LINES_SNIPPET)
  //$$         .withLocation(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("subtick", "world_lines"))
  //$$         .withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST)
  //$$         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.LINES)
  //$$         .build();
  //$$ public static final net.minecraft.client.renderer.RenderType WORLD_LINES = net.minecraft.client.renderer.RenderType.create(
  //$$        "subtick_world_lines",
  //#if MC >= 12111
  //$$ net.minecraft.client.renderer.rendertype.RenderSetup.builder(WORLD_LINE_PIPELINE)
  //$$                .affectsCrumbling()
  //$$                .sortOnUpload()
  //$$                .bufferSize(256)
  //$$                .createRenderSetup()
  //#else
  //$$ 256, false, true, WORLD_LINE_PIPELINE,
  //$$         net.minecraft.client.renderer.RenderType.CompositeState.builder()
  //$$              .setLineState(new net.minecraft.client.renderer.RenderStateShard.LineStateShard(java.util.OptionalDouble.empty()))
  //$$               .createCompositeState(false)
  //#endif
  //$$ );
  //#endif
  private static final HashSet<Line> lines = new HashSet<>();
  private static final HashSet<Quad> quads = new HashSet<>();
  private static final HashSet<Text> texts = new HashSet<>();

  public static void init(){
    //#if MC >= 12111
    //$$ WorldRenderEvents.BEFORE_TRANSLUCENT.register(LevelRenderer::render);
    //#elseif MC >= 12110
    //$$ WorldRenderEvents.AFTER_TRANSLUCENT.register(LevelRenderer::render);
    //#else
    WorldRenderEvents.LAST.register(LevelRenderer::render);
    //#endif
  }

  public static synchronized void render(WorldRenderContext context) {
    if (lines.isEmpty() && quads.isEmpty() && texts.isEmpty()) return;
    //#if MC >= 12111
    //$$ PoseStack ps = context.matrices();
    //#else
    PoseStack ps = context.matrixStack();
    //#endif

    Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
    //#if MC >= 12111
    //$$ Vec3 cpos = camera.position();
    //#else
    Vec3 cpos = camera.getPosition();
    //#endif
    if (!quads.isEmpty()) {
      //#if MC < 12105
      //#if MC >= 12103
      //$$ RenderSystem.setShader(CoreShaders.POSITION_COLOR);
      //#else
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      //#endif
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      //#endif

      BufferBuilder quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      for (Quad quad : quads) {
        quad.render(quadBuffer, cpos.x, cpos.y, cpos.z);
      }
      //#if MC >= 12105
      //$$ WORLD_QUADS.draw(quadBuffer.buildOrThrow());
      //#else
      BufferUploader.drawWithShader(quadBuffer.buildOrThrow());
      //#endif
    }
    if (!lines.isEmpty()) {
      //#if MC < 12105
      //#if MC >= 12103
      //$$ RenderSystem.setShader(CoreShaders.POSITION_COLOR);
      //#else
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      //#endif
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      //#endif

      BufferBuilder lineBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
      for (Line line : lines) {
        line.render(lineBuffer, cpos.x, cpos.y, cpos.z);
      }
      //#if MC >= 12105
      //$$ WORLD_LINES.draw(lineBuffer.buildOrThrow());
      //#else
      BufferUploader.drawWithShader(lineBuffer.buildOrThrow());
      //#endif
    }

    if (!texts.isEmpty()) {
      //#if MC < 12105
      //#if MC >= 12103
      //$$ RenderSystem.setShader(CoreShaders.POSITION_COLOR);
      //#else
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      //#endif
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      //#endif
      for (Text text : texts) {
        text.render(null, ps, camera.rotation(), cpos.x, cpos.y, cpos.z);
      }
    }
  }

  public static synchronized void clear()
  {
    lines.clear();
    quads.clear();
    texts.clear();
  }

  public static void addCuboid(int x, int y, int z, Color4f color)
  {
    addCuboidFaces(x, y, z, x+1, y+1, z+1, color);
    addCuboidEdges(x, y, z, x+1, y+1, z+1, color);
  }

  public static void addCuboidFaces(int x, int y, int z, Color4f color)
  {
    addCuboidFaces(x, y, z, x+1, y+1, z+1, color);
  }

  public static void addCuboidEdges(int x, int y, int z, Color4f color)
  {
    addCuboidEdges(x, y, z, x+1, y+1, z+1, color);
  }

  public static synchronized void addCuboidFaces(double x, double y, double z, double X, double Y, double Z, Color4f color)
  {
    QuadCuboid o = new QuadCuboid(x, y, z, X, Y, Z, color);
    if(!quads.add(o))
    {
      quads.remove(o);
      quads.add(o);
    }
  }

  public static synchronized void addCuboidEdges(double x, double y, double z, double X, double Y, double Z, Color4f color)
  {
    LineCuboid o = new LineCuboid(x, y, z, X, Y, Z, color);
    if(!lines.add(o))
    {
      lines.remove(o);
      lines.add(o);
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

  private static interface Line
  {
    public void render(BufferBuilder buffer, double cx, double cy, double cz);
  }

  private static record LineCuboid(double x, double y, double z, double X, double Y, double Z, Color4f color) implements Line
  {
    @Override
    public boolean equals(Object b)
    {
      return b instanceof LineCuboid o && o.x == x && o.y == y && o.z == z && o.X == X && o.Y == Y && o.Z == Z;
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(x, y, z, X, Y, Z);
    }

    public void render(BufferBuilder buffer, double cx, double cy, double cz)
    {
      double x = this.x - cx, y = this.y - cy, z = this.z - cz;
      double X = this.X - cx, Y = this.Y - cy, Z = this.Z - cz;
      buffer.addVertex((float)x,(float) y,(float) z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float)X,(float) y,(float) z).setColor(color.r, color.g, color.b, color.a);

      buffer.addVertex((float)X,(float) y,(float) z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float)X,(float) Y,(float) z).setColor(color.r, color.g, color.b, color.a);

      buffer.addVertex((float)X,(float) Y,(float) z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float)x,(float) Y,(float) z).setColor(color.r, color.g, color.b, color.a);

      buffer.addVertex((float)x,(float) Y,(float) z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float)x,(float) y,(float) z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float)x,(float) y,(float) z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float)x,(float) y,(float) Z).setColor(color.r, color.g, color.b, color.a);

      buffer.addVertex((float)X,(float) y,(float) z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float)X,(float) y,(float) Z).setColor(color.r, color.g, color.b, color.a);

      buffer.addVertex((float)x,(float) Y,(float) z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float)x,(float) Y,(float) Z).setColor(color.r, color.g, color.b, color.a);

      buffer.addVertex((float)X,(float) Y,(float) z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float)X,(float) Y,(float) Z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float)x,(float) y,(float) Z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float)X,(float) y,(float) Z).setColor(color.r, color.g, color.b, color.a);

      buffer.addVertex((float)X,(float) y,(float) Z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float)X,(float) Y,(float) Z).setColor(color.r, color.g, color.b, color.a);

      buffer.addVertex((float)X,(float) Y,(float) Z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float)x,(float) Y,(float) Z).setColor(color.r, color.g, color.b, color.a);

      buffer.addVertex((float)x,(float) Y,(float) Z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float)x,(float) y,(float) Z).setColor(color.r, color.g, color.b, color.a);
    }
  }

  private static interface Quad
  {
    public void render(BufferBuilder buffer, double cx, double cy, double cz);
  }

  private static record QuadCuboid(double x, double y, double z, double X, double Y, double Z, Color4f color) implements Quad
  {
    @Override
    public boolean equals(Object b)
    {
      return b instanceof QuadCuboid o && o.x == x && o.y == y && o.z == z && o.X == X && o.Y == Y && o.Z == Z;
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(x, y, z, X, Y, Z);
    }

    public void render(BufferBuilder buffer, double cx, double cy, double cz)
    {
      double x = this.x - cx, y = this.y - cy, z = this.z - cz;
      double X = this.X - cx, Y = this.Y - cy, Z = this.Z - cz;
      buffer.addVertex((float) x, (float) y, (float) z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) x, (float) Y, (float) z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) x, (float) Y, (float) Z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) x, (float) y, (float) Z).setColor(color.r, color.g, color.b, color.a);

      buffer.addVertex((float) X,(float) y,(float) z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) X,(float) y,(float) Z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) X,(float) Y,(float) Z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) X,(float) Y,(float) z).setColor(color.r, color.g, color.b, color.a);

      buffer.addVertex((float) x,(float)  y,(float)  z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) x,(float)  y,(float)  Z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) X,(float)  y,(float)  Z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) X,(float)  y,(float)  z).setColor(color.r, color.g, color.b, color.a);

      buffer.addVertex((float) x,(float)  Y,(float)  z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) X,(float)  Y,(float)  z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) X,(float)  Y,(float)  Z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) x,(float)  Y,(float)  Z).setColor(color.r, color.g, color.b, color.a);

      buffer.addVertex((float) x,(float)  y,(float)  z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) X,(float)  y,(float)  z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) X,(float)  Y,(float)  z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) x,(float)  Y,(float)  z).setColor(color.r, color.g, color.b, color.a);

      buffer.addVertex((float) x,(float)  y,(float)  Z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) x,(float)  Y,(float)  Z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) X,(float)  Y,(float)  Z).setColor(color.r, color.g, color.b, color.a);
      buffer.addVertex((float) X,(float)  y,(float)  Z).setColor(color.r, color.g, color.b, color.a);
    }
  }

  private static interface Text
  {
    public void render(BufferBuilder builder, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz);
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
    public void render(BufferBuilder buffer, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz)
    {
      Font font = Minecraft.getInstance().font;
      poseStack.pushPose();
      poseStack.translate((float)(x - cx), (float)(y - cy), (float)(z - cz));
      poseStack.mulPose(rotation);
      poseStack.scale(0.07F, -0.07F, 0.07F);
      //#if MC <= 12101
      RenderSystem.applyModelViewMatrix();
      //#endif
      MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
      font.drawInBatch(text, -font.width(text)/2F, -font.lineHeight * 0.5F, color.intValue, false, poseStack.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
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
    public void render(BufferBuilder buffer, PoseStack poseStack, Quaternionf rotation, double cx, double cy, double cz)
    {
      Font font = Minecraft.getInstance().font;
      poseStack.pushPose();
      poseStack.translate((float)(x - cx), (float)(y - cy), (float)(z - cz));
      poseStack.mulPose(rotation);
      poseStack.scale(0.07F, -0.07F, 0.08F);
      //#if MC <= 12101
      RenderSystem.applyModelViewMatrix();
      //#endif

      MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
      font.drawInBatch(index, -font.width(index)/2F, -font.lineHeight * 0.5F, color1, false, poseStack.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      immediate.endBatch();

      poseStack.translate(font.width(index)/2F, 0, 0);
      poseStack.scale(0.5F, 0.5F, 0.5F);
      //#if MC <= 12101
      RenderSystem.applyModelViewMatrix();
      //#endif
      immediate = Minecraft.getInstance().renderBuffers().bufferSource();
      font.drawInBatch(depth, -font.width(depth)/2F, font.lineHeight + 1, color2, false, poseStack.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      immediate.endBatch();
      poseStack.popPose();
    }
  }
}