package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;
import com.mojang.math.Axis;
import li.cil.oc.common.tileentity.Printer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PrinterRenderer implements BlockEntityRenderer<Printer> {
    public PrinterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Printer printer, float dt, PoseStack matrix, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        if (!printer.data.stateOff.isEmpty()) {
            final ItemStack stack = printer.data.createItemStack();

            matrix.pushPose();
            matrix.translate(0.5, 0.5 + 0.3, 0.5);

            matrix.mulPose(Axis.YP.rotationDegrees((System.currentTimeMillis() % 20000) / 20000f * 360));
            matrix.scale(0.75f, 0.75f, 0.75f);

            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, matrix, buffer, printer.getLevel(), 0);

            matrix.popPose();
        }

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
