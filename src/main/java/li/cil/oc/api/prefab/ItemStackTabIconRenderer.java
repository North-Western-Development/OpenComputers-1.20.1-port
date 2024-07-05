package li.cil.oc.api.prefab;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL13;

/**
 * Simple implementation of a tab icon renderer using an item stack as its graphic.
 */
@SuppressWarnings("UnusedDeclaration")
public class ItemStackTabIconRenderer implements TabIconRenderer {
    private final ItemStack stack;

    public ItemStackTabIconRenderer(ItemStack stack) {
        this.stack = stack;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(PoseStack matrix) {
        // Translate manually because ItemRenderer generally can't take a PoseStack.
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(matrix.last().pose());
        RenderSystem.enableRescaleNormal();
        RenderSystem.glMultiTexCoord2f(GL13.GL_TEXTURE1, 240, 240);
        Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, 0, 0);
        RenderSystem.popMatrix();
    }
}
