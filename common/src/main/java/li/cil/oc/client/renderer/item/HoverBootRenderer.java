package li.cil.oc.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.Settings;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Armor model of the hover boots: the legs carry a small drone each.
 */
public final class HoverBootRenderer extends HumanoidModel<LivingEntity> {
    public static final HoverBootRenderer INSTANCE = new HoverBootRenderer(createLayer().bakeRoot());

    public final ResourceLocation texture = new ResourceLocation(Settings.resourceDomain, "textures/model/drone.png");

    public int lightColor = 0x66DD55;

    private final ModelPart[] lights;

    private HoverBootRenderer(ModelPart root) {
        super(root);

        lights = new ModelPart[]{
            leftLeg.getChild("boot").getChild("wing0").getChild("light0"),
            leftLeg.getChild("boot").getChild("wing1").getChild("light1"),
            rightLeg.getChild("boot").getChild("wing2").getChild("light2"),
            rightLeg.getChild("boot").getChild("wing3").getChild("light3")
        };

        head.visible = false;
        hat.visible = false;
        body.visible = false;
        rightArm.visible = false;
        leftArm.visible = false;
    }

    public static LayerDefinition createLayer() {
        final MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.5f), 0);
        final PartDefinition root = mesh.getRoot();

        // No drone textured legs, thank you very much.
        final PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        final PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));

        final PartDefinition bootLeft = leftLeg.addOrReplaceChild("boot", CubeListBuilder.create(), PartPose.offset(0, 10.11f, 0));
        final PartDefinition bootRight = rightLeg.addOrReplaceChild("boot", CubeListBuilder.create(), PartPose.offset(0, 10.1f, 0));

        addDroneBody(bootLeft);
        addDroneBody(bootRight);

        final PartDefinition wing0 = bootLeft.addOrReplaceChild("wing0", CubeListBuilder.create()
            .texOffs(0, 9).addBox(-1, 0, -7, 6, 1, 6) // flap0
            .texOffs(0, 27).addBox(0, -1, -3, 1, 3, 1), PartPose.ZERO); // pin0
        final PartDefinition wing1 = bootLeft.addOrReplaceChild("wing1", CubeListBuilder.create()
            .texOffs(0, 9).addBox(-1, 0, 1, 6, 1, 6) // flap1
            .texOffs(0, 27).addBox(0, -1, 2, 1, 3, 1), PartPose.ZERO); // pin1
        final PartDefinition wing2 = bootRight.addOrReplaceChild("wing2", CubeListBuilder.create()
            .texOffs(0, 9).addBox(-5, 0, 1, 6, 1, 6) // flap2
            .texOffs(0, 27).addBox(-1, -1, 2, 1, 3, 1), PartPose.ZERO); // pin2
        final PartDefinition wing3 = bootRight.addOrReplaceChild("wing3", CubeListBuilder.create()
            .texOffs(0, 9).addBox(-5, 0, -7, 6, 1, 6) // flap3
            .texOffs(0, 27).addBox(-1, -1, -3, 1, 3, 1), PartPose.ZERO); // pin3

        wing0.addOrReplaceChild("light0", CubeListBuilder.create().texOffs(24, 0).addBox(-1, 0, -7, 6, 1, 6), PartPose.ZERO); // flap0
        wing1.addOrReplaceChild("light1", CubeListBuilder.create().texOffs(24, 0).addBox(-1, 0, 1, 6, 1, 6), PartPose.ZERO); // flap1
        wing2.addOrReplaceChild("light2", CubeListBuilder.create().texOffs(24, 0).addBox(-5, 0, 1, 6, 1, 6), PartPose.ZERO); // flap2
        wing3.addOrReplaceChild("light3", CubeListBuilder.create().texOffs(24, 0).addBox(-5, 0, -7, 6, 1, 6), PartPose.ZERO); // flap3

        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void addDroneBody(PartDefinition boot) {
        boot.addOrReplaceChild("drone_body", CubeListBuilder.create()
            .texOffs(0, 23).addBox(-3, 1, -3, 6, 1, 6) // top
            .texOffs(0, 1).addBox(-1, 0, -1, 2, 1, 2) // middle
            .texOffs(0, 17).addBox(-2, -1, -2, 4, 1, 4), // bottom
            PartPose.rotation(0, (float) Math.toRadians(45), 0));
    }

    @Override
    public void setupAnim(LivingEntity entity, float f1, float f2, float f3, float f4, float f5) {
        super.setupAnim(entity, f1, f2, f3, f4, f5);
        crouching = entity.isCrouching();
        young = false;
    }

    @Override
    public void renderToBuffer(PoseStack stack, VertexConsumer builder, int light, int overlay, float r, float g, float b, float a) {
        young = false;

        // Regular parts, without the lights.
        for (ModelPart part : lights) part.visible = false;
        rightLeg.render(stack, builder, light, overlay, r, g, b, a);
        leftLeg.render(stack, builder, light, overlay, r, g, b, a);
        for (ModelPart part : lights) part.visible = true;

        // Lights only: colored and full-bright.
        final float rm = ((lightColor >>> 16) & 0xFF) / 255f;
        final float gm = ((lightColor >>> 8) & 0xFF) / 255f;
        final float bm = ((lightColor) & 0xFF) / 255f;
        setSkipDraw(true);
        for (ModelPart part : lights) part.skipDraw = false;
        rightLeg.render(stack, builder, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY, r * rm, g * gm, b * bm, a);
        leftLeg.render(stack, builder, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY, r * rm, g * gm, b * bm, a);
        setSkipDraw(false);
    }

    private void setSkipDraw(boolean value) {
        setSkipDraw(rightLeg, value);
        setSkipDraw(leftLeg, value);
    }

    private static void setSkipDraw(ModelPart part, boolean value) {
        part.skipDraw = value;
        for (String name : new String[]{"boot", "drone_body", "wing0", "wing1", "wing2", "wing3", "light0", "light1", "light2", "light3"}) {
            if (part.hasChild(name)) setSkipDraw(part.getChild(name), value);
        }
    }
}
