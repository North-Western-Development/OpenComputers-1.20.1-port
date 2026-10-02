package li.cil.oc.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.RackMountableRenderEvent;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.client.renderer.tileentity.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.List;

/**
 * Client only: {@link #register()} must only be called on the physical client.
 */
public final class RackMountableRenderHandler {
    private RackMountableRenderHandler() {
    }

    private static ItemInfo diskDriveMountable;
    private static List<ItemInfo> servers;
    private static ItemInfo terminalServer;

    private static ItemInfo DiskDriveMountable() {
        if (diskDriveMountable == null) diskDriveMountable = Items.get(Constants.ItemName.DiskDriveMountable);
        return diskDriveMountable;
    }

    private static List<ItemInfo> Servers() {
        if (servers == null) servers = Arrays.asList(
            Items.get(Constants.ItemName.ServerTier1),
            Items.get(Constants.ItemName.ServerTier2),
            Items.get(Constants.ItemName.ServerTier3),
            Items.get(Constants.ItemName.ServerCreative));
        return servers;
    }

    private static ItemInfo TerminalServer() {
        if (terminalServer == null) terminalServer = Items.get(Constants.ItemName.TerminalServer);
        return terminalServer;
    }

    public static void register() {
        EventBus.INSTANCE.register(RackMountableRenderEvent.TileEntity.class, RackMountableRenderHandler::onRackMountableRendering);
        EventBus.INSTANCE.register(RackMountableRenderEvent.Block.class, RackMountableRenderHandler::onRackMountableRendering);
    }

    public static void onRackMountableRendering(RackMountableRenderEvent.TileEntity e) {
        ItemInfo info = Items.get(e.rack.getItem(e.mountable));
        if (e.data != null && info != null && DiskDriveMountable() == info) {
            // Disk drive.

            if (e.data.contains("disk")) {
                ItemStack stack = ItemStack.of(e.data.getCompound("disk"));
                if (!stack.isEmpty()) {
                    PoseStack matrix = e.stack;
                    matrix.pushPose();
                    matrix.scale(1, -1, 1);
                    matrix.translate(10 / 16f, -(3.5f + e.mountable * 3f) / 16f, -2 / 16f);
                    matrix.mulPose(Axis.XN.rotationDegrees(90));
                    matrix.scale(0.5f, 0.5f, 0.5f);

                    Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, e.light, e.overlay, matrix, e.typeBuffer, e.rack.world(), 0);
                    matrix.popPose();
                }
            }

            if (System.currentTimeMillis() - e.data.getLong("lastAccess") < 400 && e.rack.world().random.nextDouble() > 0.1) {
                renderOverlayFromAtlas(e, Textures.Block.RackDiskDriveActivity);
            }
        }
        else if (e.data != null && info != null && Servers().contains(info)) {
            // Server.
            if (e.data.getBoolean("isRunning")) {
                renderOverlayFromAtlas(e, Textures.Block.RackServerOn);
            }
            if (e.data.getBoolean("hasErrored") && RenderUtil.shouldShowErrorLight(e.rack.hashCode() * (e.mountable + 1))) {
                renderOverlayFromAtlas(e, Textures.Block.RackServerError);
            }
            if (System.currentTimeMillis() - e.data.getLong("lastFileSystemAccess") < 400 && e.rack.world().random.nextDouble() > 0.1) {
                renderOverlayFromAtlas(e, Textures.Block.RackServerActivity);
            }
            if ((System.currentTimeMillis() - e.data.getLong("lastNetworkActivity") < 300 && System.currentTimeMillis() % 200 > 100) && e.data.getBoolean("isRunning")) {
                renderOverlayFromAtlas(e, Textures.Block.RackServerNetworkActivity);
            }
        }
        else if (e.data != null && info != null && TerminalServer() == info) {
            // Terminal server.
            renderOverlayFromAtlas(e, Textures.Block.RackTerminalServerOn);
            int countConnected = e.data.getList("keys", Tag.TAG_STRING).size();

            if (countConnected > 0) {
                float u0 = 7 / 16f;
                float u1 = u0 + (2 * countConnected - 1) / 16f;
                renderOverlayFromAtlas(e, Textures.Block.RackTerminalServerPresence, u0, u1);
            }
        }
    }

    private static void renderOverlayFromAtlas(RackMountableRenderEvent.TileEntity e, ResourceLocation texture) {
        renderOverlayFromAtlas(e, texture, 0, 1);
    }

    private static void renderOverlayFromAtlas(RackMountableRenderEvent.TileEntity e, ResourceLocation texture, float u0, float u1) {
        Matrix4f matrix = e.stack.last().pose();
        VertexConsumer r = e.typeBuffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
        TextureAtlasSprite icon = Textures.getSprite(texture);
        r.vertex(matrix, u0, e.v1, 0).uv(icon.getU(u0 * 16), icon.getV(e.v1 * 16)).endVertex();
        r.vertex(matrix, u1, e.v1, 0).uv(icon.getU(u1 * 16), icon.getV(e.v1 * 16)).endVertex();
        r.vertex(matrix, u1, e.v0, 0).uv(icon.getU(u1 * 16), icon.getV(e.v0 * 16)).endVertex();
        r.vertex(matrix, u0, e.v0, 0).uv(icon.getU(u0 * 16), icon.getV(e.v0 * 16)).endVertex();
    }

    public static void onRackMountableRendering(RackMountableRenderEvent.Block e) {
        ItemInfo info = Items.get(e.rack.getItem(e.mountable));
        if (info == null) return;
        if (DiskDriveMountable() == info) {
            // Disk drive.
            e.setFrontTextureOverride(Textures.getSprite(Textures.Block.RackDiskDrive));
        }
        else if (Servers().contains(info)) {
            // Server.
            e.setFrontTextureOverride(Textures.getSprite(Textures.Block.RackServer));
        }
        else if (TerminalServer() == info) {
            // Terminal server.
            e.setFrontTextureOverride(Textures.getSprite(Textures.Block.RackTerminalServer));
        }
    }
}
