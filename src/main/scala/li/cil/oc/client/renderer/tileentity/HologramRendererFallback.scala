package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import li.cil.oc.common.tileentity.Hologram
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.MultiBufferSource

object HologramRendererFallback {
  var text = "Requires OpenGL 1.5"

  def render(hologram: Hologram, f: Float, stack: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    RenderSystem.setShaderColor(1, 1, 1, 1)

    val fontRenderer = Minecraft.getInstance.font

    stack.pushPose()
    stack.translate(0.5, 0.75, 0.5)
    stack.scale(1 / 128f, -1 / 128f, 1 / 128f)

    fontRenderer.drawInBatch(text, -fontRenderer.width(text) / 2, 0, 0xFFFFFFFF,
      false, stack.last.pose, buffer, Font.DisplayMode.NORMAL, 0, light)
    stack.mulPose(Axis.YP.rotationDegrees(180))
    fontRenderer.drawInBatch(text, -fontRenderer.width(text) / 2, 0, 0xFFFFFFFF,
      false, stack.last.pose, buffer, Font.DisplayMode.NORMAL, 0, light)

    stack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
