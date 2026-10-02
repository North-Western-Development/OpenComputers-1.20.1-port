package li.cil.oc.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.client.renderer.tileentity.RenderUtil;
import li.cil.oc.common.entity.Drone;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

public final class ModelQuadcopter extends EntityModel<Drone> {
    private final ModelPart body;
    private final ModelPart wing0;
    private final ModelPart wing1;
    private final ModelPart wing2;
    private final ModelPart wing3;
    private final ModelPart light0;
    private final ModelPart light1;
    private final ModelPart light2;
    private final ModelPart light3;

    private static final Vec3 up = new Vec3(0, 1, 0);

    public ModelQuadcopter() {
        this(createLayer().bakeRoot());
    }

    public ModelQuadcopter(ModelPart root) {
        body = root.getChild("body");
        wing0 = root.getChild("wing0");
        wing1 = root.getChild("wing1");
        wing2 = root.getChild("wing2");
        wing3 = root.getChild("wing3");
        light0 = root.getChild("light0");
        light1 = root.getChild("light1");
        light2 = root.getChild("light2");
        light3 = root.getChild("light3");
    }

    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 23).addBox(-3, 1, -3, 6, 1, 6) // top
            .texOffs(0, 1).addBox(-1, 0, -1, 2, 1, 2) // middle
            .texOffs(0, 17).addBox(-2, -1, -2, 4, 1, 4), // bottom
            PartPose.rotation(0, (float) Math.toRadians(45), 0));
        root.addOrReplaceChild("wing0", CubeListBuilder.create()
            .texOffs(0, 9).addBox(1, 0, -7, 6, 1, 6) // flap0
            .texOffs(0, 27).addBox(2, -1, -3, 1, 3, 1), PartPose.ZERO); // pin0
        root.addOrReplaceChild("wing1", CubeListBuilder.create()
            .texOffs(0, 9).addBox(1, 0, 1, 6, 1, 6) // flap1
            .texOffs(0, 27).addBox(2, -1, 2, 1, 3, 1), PartPose.ZERO); // pin1
        root.addOrReplaceChild("wing2", CubeListBuilder.create()
            .texOffs(0, 9).addBox(-7, 0, 1, 6, 1, 6) // flap2
            .texOffs(0, 27).addBox(-3, -1, 2, 1, 3, 1), PartPose.ZERO); // pin2
        root.addOrReplaceChild("wing3", CubeListBuilder.create()
            .texOffs(0, 9).addBox(-7, 0, -7, 6, 1, 6) // flap3
            .texOffs(0, 27).addBox(-3, -1, -3, 1, 3, 1), PartPose.ZERO); // pin3

        root.addOrReplaceChild("light0", CubeListBuilder.create().texOffs(24, 0).addBox(1, 0, -7, 6, 1, 6), PartPose.ZERO); // flap0
        root.addOrReplaceChild("light1", CubeListBuilder.create().texOffs(24, 0).addBox(1, 0, 1, 6, 1, 6), PartPose.ZERO); // flap1
        root.addOrReplaceChild("light2", CubeListBuilder.create().texOffs(24, 0).addBox(-7, 0, 1, 6, 1, 6), PartPose.ZERO); // flap2
        root.addOrReplaceChild("light3", CubeListBuilder.create().texOffs(24, 0).addBox(-7, 0, -7, 6, 1, 6), PartPose.ZERO); // flap3

        return LayerDefinition.create(mesh, 64, 32);
    }

    private void doRender(Drone drone, float dt, PoseStack stack, VertexConsumer builder, int light, int overlay, float r, float g, float b, float a) {
        stack.pushPose();
        if (drone.isRunning()) {
            final int timeJitter = drone.hashCode() ^ 0xFF;
            stack.translate(0, (float) (Math.sin(timeJitter + (drone.level().getGameTime() + dt) / 20.0) * (1 / 16f)), 0);
        }

        final Vec3 direction = drone.getDeltaMovement().normalize();
        if (direction.dot(up) < 0.99) {
            // Flying sideways.
            final Vec3 rotationAxis = direction.cross(up);
            final float relativeSpeed = (float) drone.getDeltaMovement().length() / drone.maxVelocity;
            RenderUtil.rotate(stack, relativeSpeed * -20, (float) rotationAxis.x, (float) rotationAxis.y, (float) rotationAxis.z);
        }

        stack.mulPose(Axis.YP.rotationDegrees(drone.bodyAngle));
        body.render(stack, builder, light, overlay, r, g, b, a);

        wing0.xRot = drone.flapAngles[0][0];
        wing0.zRot = drone.flapAngles[0][1];
        wing1.xRot = drone.flapAngles[1][0];
        wing1.zRot = drone.flapAngles[1][1];
        wing2.xRot = drone.flapAngles[2][0];
        wing2.zRot = drone.flapAngles[2][1];
        wing3.xRot = drone.flapAngles[3][0];
        wing3.zRot = drone.flapAngles[3][1];

        wing0.render(stack, builder, light, overlay, r, g, b, a);
        wing1.render(stack, builder, light, overlay, r, g, b, a);
        wing2.render(stack, builder, light, overlay, r, g, b, a);
        wing3.render(stack, builder, light, overlay, r, g, b, a);

        if (drone.isRunning()) {
            light0.xRot = drone.flapAngles[0][0];
            light0.zRot = drone.flapAngles[0][1];
            light1.xRot = drone.flapAngles[1][0];
            light1.zRot = drone.flapAngles[1][1];
            light2.xRot = drone.flapAngles[2][0];
            light2.zRot = drone.flapAngles[2][1];
            light3.xRot = drone.flapAngles[3][0];
            light3.zRot = drone.flapAngles[3][1];

            final int lightColor = drone.lightColor();
            final float rr = r * ((lightColor >>> 16) & 0xFF) / 255f;
            final float gg = g * ((lightColor >>> 8) & 0xFF) / 255f;
            final float bb = b * ((lightColor) & 0xFF) / 255f;
            final int fullLight = LightTexture.pack(15, 15);

            light0.render(stack, builder, fullLight, OverlayTexture.NO_OVERLAY, rr, gg, bb, a);
            light1.render(stack, builder, fullLight, OverlayTexture.NO_OVERLAY, rr, gg, bb, a);
            light2.render(stack, builder, fullLight, OverlayTexture.NO_OVERLAY, rr, gg, bb, a);
            light3.render(stack, builder, fullLight, OverlayTexture.NO_OVERLAY, rr, gg, bb, a);
        }
        stack.popPose();
    }

    private Drone cachedEntity = null;
    private float cachedDt = 0.0f;

    @Override
    public void setupAnim(Drone drone, float f1, float f2, float f3, float f4, float f5) {
    }

    @Override
    public void prepareMobModel(Drone drone, float f1, float f2, float dt) {
        cachedEntity = drone;
        cachedDt = dt;
    }

    @Override
    public void renderToBuffer(PoseStack stack, VertexConsumer builder, int light, int overlay, float r, float g, float b, float a) {
        if (cachedEntity != null) {
            doRender(cachedEntity, cachedDt, stack, builder, light, overlay, r, g, b, a);
        }
    }
}
