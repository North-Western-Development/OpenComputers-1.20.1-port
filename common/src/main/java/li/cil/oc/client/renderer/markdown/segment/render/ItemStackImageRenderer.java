package li.cil.oc.client.renderer.markdown.segment.render;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class ItemStackImageRenderer implements ImageRenderer {
    // How long to show individual stacks, in milliseconds, before switching to the next.
    public static final int cycleSpeed = 1000;

    public final ItemStack[] stacks;

    public ItemStackImageRenderer(ItemStack[] stacks) {
        this.stacks = stacks;
    }

    @Override
    public int getWidth() {
        return 32;
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (stacks.length == 0) return;
        final int index = (int) (System.currentTimeMillis() % ((long) cycleSpeed * stacks.length)) / cycleSpeed;
        final ItemStack stack = stacks[index];

        final PoseStack matrix = graphics.pose();
        matrix.pushPose();
        matrix.scale(getWidth() / 16, getHeight() / 16, getWidth() / 16);
        graphics.renderItem(stack, 0, 0);
        matrix.popPose();
    }
}
