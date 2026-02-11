package subtick.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import fi.dy.masa.malilib.util.Color4f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.HashSet;
import java.util.Objects;

public class LevelRenderer
{
  private static final Minecraft mc = Minecraft.getInstance();
  private static final Font font = mc.font;
  private static final HashSet<Line> lines = new HashSet<>();
  private static final HashSet<Quad> quads = new HashSet<>();
  private static final HashSet<Text> texts = new HashSet<>();

  public static synchronized void render(PoseStack ps) {
    if (lines.isEmpty() && quads.isEmpty() && texts.isEmpty()) return;

    Camera camera = mc.gameRenderer.getMainCamera();
    Vec3 cpos = camera.getPosition();
    if (!quads.isEmpty()) {
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();

      BufferBuilder quadBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      for (Quad quad : quads) {
        quad.render(quadBuffer, cpos.x, cpos.y, cpos.z);
      }
      BufferUploader.drawWithShader(quadBuffer.buildOrThrow());
    }
    if (!lines.isEmpty()) {
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();

      BufferBuilder lineBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
      for (Line line : lines) {
        line.render(lineBuffer, cpos.x, cpos.y, cpos.z);
      }
      BufferUploader.drawWithShader(lineBuffer.buildOrThrow());
    }

    if (!texts.isEmpty()) {
      RenderSystem.setShader(GameRenderer::getRendertypeTextShader);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
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
      poseStack.pushPose();
      poseStack.translate((float)(x - cx), (float)(y - cy), (float)(z - cz));
      poseStack.mulPose(rotation);
      poseStack.scale(-0.07F, -0.07F, 0.07F);
      RenderSystem.applyModelViewMatrix();
      MultiBufferSource.BufferSource immediate = mc.renderBuffers().bufferSource();
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
      poseStack.pushPose();
      poseStack.translate((float)(x - cx), (float)(y - cy), (float)(z - cz));
      poseStack.mulPose(rotation);
      poseStack.scale(-0.07F, -0.07F, 0.08F);
      RenderSystem.applyModelViewMatrix();

      MultiBufferSource.BufferSource immediate = mc.renderBuffers().bufferSource();
      font.drawInBatch(index, -font.width(index)/2F, -font.lineHeight * 0.5F, color1, false, poseStack.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      immediate.endBatch();

      poseStack.translate(font.width(index)/2F, 0, 0);
      poseStack.scale(0.5F, 0.5F, 0.5F);
      RenderSystem.applyModelViewMatrix();
      immediate = mc.renderBuffers().bufferSource();
      font.drawInBatch(depth, -font.width(depth)/2F, font.lineHeight + 1, color2, false, poseStack.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0x00000000, 0x00000000);
      immediate.endBatch();
      poseStack.popPose();
    }
  }
}