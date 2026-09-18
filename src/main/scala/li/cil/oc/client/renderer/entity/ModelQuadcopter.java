package li.cil.oc.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
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

    private final Vec3 up;

    private Drone drone;
    private float time;

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

        up = new Vec3(0, 1, 0);
    }

    @Override
    public void setupAnim(Drone drone, float v, float v1, float v2, float v3, float v4) {}

    @Override
    public void prepareMobModel(Drone drone, float limbSwing, float limbSwingAmount, float partialTick) {
        this.drone = drone;
        this.time = partialTick;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int i1, float v, float v1, float v2, float v3) {
        poseStack.pushPose();

        if (drone.isRunning()) {
            int timeJitter = drone.hashCode() ^ 0xFF;
            double hoverOffset = Math.sin(timeJitter + (drone.level.getGameTime() + time) / 20.0) * (1.0f / 16.0f);
            poseStack.translate(0.0, hoverOffset, 0.0);
        }

        Vec3 velocity = drone.getDeltaMovement();
        Vec3 direction = velocity.normalize();

        if (direction.dot(up) < 0.99) {
            // Flying sideways.
            Vec3 rotationAxis = direction.cross(up);
            float relativeSpeed = ((float)velocity.length()) / drone.maxVelocity();

            poseStack.mulPose(
                    new Vector3f(
                            (float)rotationAxis.x,
                            (float)rotationAxis.y,
                            (float)rotationAxis.z)
                            .rotationDegrees(relativeSpeed * -20.0f)
            );
        }

        poseStack.mulPose(Vector3f.YP.rotationDegrees(drone.bodyAngle()));

        body.render(poseStack, vertexConsumer, i, i1, v, v1, v2, v3);

        setWingsRotation();
        renderWings(poseStack, vertexConsumer, i, i1, v, v1, v2, v3);

        if (drone.isRunning()) {
            setWingsRotation();

            int lightColor = drone.lightColor();
            float rr = v * ((lightColor >>> 16) & 0xFF) / 255.0f;
            float gg = v1 * ((lightColor >>> 8) & 0xFF) / 255.0f;
            float bb = v2 * (lightColor & 0xFF) / 255.0f;
            float fullLight = LightTexture.pack(15, 15);

            renderLights(poseStack, vertexConsumer, i, rr, gg, bb, fullLight);
        }

        poseStack.popPose();
    }

    private void setWingsRotation() {
        wing0.xRot = drone.flapAngles()[0][0];
        wing0.yRot = drone.flapAngles()[0][1];
        wing1.xRot = drone.flapAngles()[1][0];
        wing1.yRot = drone.flapAngles()[1][1];
        wing2.xRot = drone.flapAngles()[2][0];
        wing2.yRot = drone.flapAngles()[2][1];
        wing3.xRot = drone.flapAngles()[3][0];
        wing3.yRot = drone.flapAngles()[3][1];
    }

    private void renderWings(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int i1, float v, float v1, float v2, float v3) {
        wing0.render(poseStack, vertexConsumer, i, i1, v, v1, v2, v3);
        wing1.render(poseStack, vertexConsumer, i, i1, v, v1, v2, v3);
        wing2.render(poseStack, vertexConsumer, i, i1, v, v1, v2, v3);
        wing3.render(poseStack, vertexConsumer, i, i1, v, v1, v2, v3);
    }

    private void renderLights(PoseStack poseStack, VertexConsumer vertexConsumer, int i, float v, float v1, float v2, float v3) {
        light0.render(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY, v, v1, v2, v3);
        light1.render(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY, v, v1, v2, v3);
        light2.render(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY, v, v1, v2, v3);
        light3.render(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY, v, v1, v2, v3);
    }


    private static void addWing(PartDefinition root, String name, float flapX, float flapZ, float pinX, float pinZ) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(0, 9)
                        .addBox(flapX, 0.0f, flapZ, 6.0f, 1.0f, 6.0f)
                        .texOffs(0, 27)
                        .addBox(pinX, -1.0f, pinZ, 1.0f, 3.0f, 1.0f),
                PartPose.ZERO
        );
    }

    private static void addLight(PartDefinition root, String name, float flapX, float flapZ) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(24, 0)
                        .addBox(flapX, 0.0f, flapZ, 6.0f, 1.0f, 6.0f),
                PartPose.ZERO
        );
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        body.addOrReplaceChild("top",
                CubeListBuilder.create()
                        .texOffs(0, 23)
                        .addBox(-3.0f, 1.0f, -3.0f, 6.0f, 1.0f, 6.0f),
                PartPose.rotation(0.0f, (float)Math.toRadians(45.0), 0.0f)
        );

        body.addOrReplaceChild("middle",
                CubeListBuilder.create()
                        .texOffs(0, 1)
                        .addBox(-1.0f, 0.0f, -1.0f, 2.0f, 1.0f, 2.0f),
                PartPose.rotation(0.0f, (float)Math.toRadians(45.0), 0.0f)
        );

        body.addOrReplaceChild("bottom",
                CubeListBuilder.create()
                        .texOffs(0, 1)
                        .addBox(-2.0f, -1.0f, -2.0f, 4.0f, 1.0f, 4.0f),
                PartPose.rotation(0.0f, (float)Math.toRadians(45.0), 0.0f)
        );

        addWing(root, "wing0", 1.0f, -7.0f, 2.0f, 3.0f);
        addWing(root, "wing1", 1.0f, 1.0f, 2.0f, 2.0f);
        addWing(root, "wing2", -7.0f, 1.0f, -3.0f, 2.0f);
        addWing(root, "wing3", -7.0f, -7.0f, -3.0f, -3.0f);

        addLight(root, "light0", 1.0f, -7.0f);
        addLight(root, "light1", 1.0f, 1.0f);
        addLight(root, "light2", -7.0f, 1.0f);
        addLight(root, "light3", -7.0f, -7.0f);

        return LayerDefinition.create(mesh, 64, 32);
    }
}
