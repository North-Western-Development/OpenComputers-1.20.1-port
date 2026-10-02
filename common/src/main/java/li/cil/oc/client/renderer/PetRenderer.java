package li.cil.oc.client.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.client.renderer.tileentity.RobotRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Renders a little robot next to some players (contributors).
 * <p>
 * Port note: this used Forge's {@code RenderPlayerEvent.Pre}, which has no
 * Fabric equivalent. Pets are now rendered in world space after the level
 * (registered via {@link li.cil.oc.client.platform.RenderPlatform#registerLevelRenderer})
 * for all players whose model is visible.
 */
public final class PetRenderer {
    private PetRenderer() {
    }

    public static final Set<String> hidden = new HashSet<>();

    public static boolean isInitialized = false;

    private record Color(double r, double g, double b) {
    }

    // http://goo.gl/frLWYR
    private static final Map<String, Color> entitledPlayers = new HashMap<>();

    static {
        entitledPlayers.put("9f1f262f-0d68-4e13-9161-9eeaf4a0a1a8", new Color(0.3, 0.9, 0.6)); // Sangar
        entitledPlayers.put("18f8bed4-f027-44af-8947-6a3a2317645a", new Color(1.0, 0.0, 0.0)); // Jodarion
        entitledPlayers.put("36123742-2cf6-4cfc-8b65-278581b3caeb", new Color(0.5, 0.7, 1.0)); // DaKaTotal
        entitledPlayers.put("2c0c214b-96f4-4565-b513-de90d5fbc977", new Color(1.0, 0.0, 0.0)); // MichiRavencroft
        entitledPlayers.put("f3ba6ec8-c280-4950-bb08-1fcb2eab3a9c", new Color(0.18, 0.95, 0.922)); // Vexatos
        entitledPlayers.put("9d636bdd-b9f4-4b80-b9ce-586ca04bd4f3", new Color(0.8, 0.77, 0.75)); // StoneNomad
        entitledPlayers.put("23c7ed71-fb13-4abe-abe7-f355e1de6e62", new Color(0.3, 0.3, 1.0)); // LizzyTheSiren
        entitledPlayers.put("076541f1-f10a-46de-a127-dfab8adfbb75", new Color(0.2, 1.0, 0.1)); // vifino
        entitledPlayers.put("e7e90198-0ccf-4662-a827-192ec8f4419d", new Color(0.0, 0.2, 0.6)); // Izaya
        entitledPlayers.put("f514ee69-7bbb-4e46-9e94-d8176324cec2", new Color(0.098, 0.471, 0.784)); // Wobbo
        entitledPlayers.put("f812c043-78ba-4324-82ae-e8f05c52ae6e", new Color(0.1, 0.8, 0.5)); // payonel
        entitledPlayers.put("1db17ee7-8830-4bac-8018-de154340aae6", new Color(0.0, 0.5, 1.0)); // Kosmos
    }

    private static final Cache<Entity, PetLocation> petLocations = CacheBuilder.newBuilder()
        .expireAfterAccess(5, TimeUnit.SECONDS)
        .weakKeys()
        .build();

    private static Color rendering = null;

    public static void onRenderLevel(PoseStack stack, float partialTicks, Camera camera) {
        final Minecraft mc = Minecraft.getInstance();
        final ClientLevel level = mc.level;
        if (level == null) return;

        boolean renderedAny = false;
        final MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (Player player : level.players()) {
            final String uuid = player.getUUID().toString();
            if (hidden.contains(uuid) || !entitledPlayers.containsKey(uuid)) continue;
            if (player.isInvisible()) continue;
            // In first person the local player's model isn't rendered.
            if (player == camera.getEntity() && !camera.isDetached()) continue;

            renderPet(stack, buffers, player, partialTicks, camera);
            renderedAny = true;
        }
        if (renderedAny) {
            buffers.endBatch(RenderTypes.ROBOT_CHASSIS);
            buffers.endBatch(RenderTypes.ROBOT_LIGHT);
        }
    }

    private static void renderPet(PoseStack stack, MultiBufferSource buffers, Player player, float partialTicks, Camera camera) {
        rendering = entitledPlayers.get(player.getUUID().toString());

        final long worldTime = player.level().getGameTime();
        final int timeJitter = player.hashCode() ^ 0xFF;
        final double offset = timeJitter + worldTime / 20.0;
        final float hover = (float) (Math.sin(timeJitter + (worldTime + partialTicks) / 20.0) * 0.03);

        final PetLocation location;
        try {
            location = petLocations.get(player, () -> new PetLocation(player));
        } catch (ExecutionException e) {
            rendering = null;
            return;
        }

        final Vec3 cam = camera.getPosition();
        final double px = player.xOld + (player.getX() - player.xOld) * partialTicks;
        final double py = player.yOld + (player.getY() - player.yOld) * partialTicks + player.getEyeHeight(player.getPose());
        final double pz = player.zOld + (player.getZ() - player.zOld) * partialTicks;

        stack.pushPose();
        stack.translate(px - cam.x, py - cam.y, pz - cam.z);

        location.applyInterpolatedTransformations(stack, partialTicks);

        stack.scale(0.3f, 0.3f, 0.3f);
        stack.translate(0, hover, 0);

        final int light = LevelRenderer.getLightColor(player.level(), BlockPos.containing(px, py, pz));
        RobotRenderer.renderChassis(stack, buffers, light, offset, true);

        stack.popPose();

        rendering = null;
    }

    /**
     * Registered on the OC event bus (lowest priority in 1.16; the OC bus has no
     * priorities, so it is registered last).
     */
    public static void onRobotRender(RobotRenderEvent e) {
        final Color color = rendering;
        if (color != null) {
            e.setLightColor((float) color.r(), (float) color.g(), (float) color.b());
            e.multiplyColors((float) color.r(), (float) color.g(), (float) color.b());
        }
    }

    private static final class PetLocation {
        final Entity owner;
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        float yaw;

        double lastX = x;
        double lastY = y;
        double lastZ = z;
        float lastYaw;

        PetLocation(Entity owner) {
            this.owner = owner;
            this.yaw = owner.getYRot();
            this.lastYaw = yaw;
        }

        void update() {
            final double dx = owner.xOld - owner.getX();
            final double dy = owner.yOld - owner.getY();
            final double dz = owner.zOld - owner.getZ();
            final float dYaw = owner.getYRot() - yaw;
            lastX = x;
            lastY = y;
            lastZ = z;
            lastYaw = yaw;
            x += dx;
            y += dy;
            z += dz;
            x *= 0.05;
            y *= 0.05;
            z *= 0.05;
            yaw += dYaw * 0.2f;
        }

        void applyInterpolatedTransformations(PoseStack stack, float dt) {
            final double ix = lastX + (x - lastX) * dt;
            final double iy = lastY + (y - lastY) * dt;
            final double iz = lastZ + (z - lastZ) * dt;
            final float iYaw = lastYaw + (yaw - lastYaw) * dt;

            stack.translate(ix, iy, iz);
            stack.mulPose(Axis.YP.rotationDegrees(-iYaw));
            stack.translate(0.3, -0.1, -0.2);
        }
    }

    /**
     * Called at the start of every client tick (registered in {@link ClientRenderers}).
     */
    public static void tickStart() {
        petLocations.cleanUp();
        for (PetLocation pet : petLocations.asMap().values()) {
            pet.update();
        }
    }
}
