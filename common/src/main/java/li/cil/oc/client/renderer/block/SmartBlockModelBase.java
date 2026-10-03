package li.cil.oc.client.renderer.block;

import li.cil.oc.client.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Base class for OpenComputers' code-generated models.
 * <p>
 * These are plain vanilla {@link BakedModel}s. Block models that depend on
 * block entity data implement {@link #getBlockQuads}; the platform layer
 * ({@code li.cil.oc.client.platform.RenderPlatform}) wraps them into a
 * loader specific model that looks up the block entity during chunk meshing
 * (Forge: {@code ModelData}, Fabric: {@code FabricBakedModel}) and calls it.
 * Item variants are provided through {@link #getOverrides()}.
 */
public abstract class SmartBlockModelBase implements BakedModel {
    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return Collections.emptyList();
    }

    /**
     * Block-entity aware quad generation used for block rendering. The block
     * entity may be {@code null} (e.g. when the model is rendered without a
     * level), in which case implementations should fall back to
     * {@link #getQuads(BlockState, Direction, RandomSource)}.
     */
    public List<BakedQuad> getBlockQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, @Nullable BlockEntity blockEntity) {
        return getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    // Note: we don't care about the actual texture here, we just need the block
    // texture atlas. So any of our textures we know is loaded into it will do.
    @Override
    public TextureAtlasSprite getParticleIcon() {
        return Textures.getSprite(Textures.Block.GenericTop);
    }

    @Override
    public ItemTransforms getTransforms() {
        return DefaultBlockCameraTransforms;
    }

    public static final ItemTransforms DefaultBlockCameraTransforms = createDefaultTransforms();

    private static ItemTransforms createDefaultTransforms() {
        // scale(0.0625f): see ItemTransform.Deserializer.
        final ItemTransform gui = new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0).mul(0.0625f), new Vector3f(0.625f, 0.625f, 0.625f));
        final ItemTransform ground = new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 3, 0).mul(0.0625f), new Vector3f(0.25f, 0.25f, 0.25f));
        final ItemTransform fixed = new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0).mul(0.0625f), new Vector3f(0.5f, 0.5f, 0.5f));
        final ItemTransform thirdpersonRighthand = new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 2.5f, 0).mul(0.0625f), new Vector3f(0.375f, 0.375f, 0.375f));
        final ItemTransform firstpersonRighthand = new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0).mul(0.0625f), new Vector3f(0.40f, 0.40f, 0.40f));
        final ItemTransform firstpersonLefthand = new ItemTransform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0).mul(0.0625f), new Vector3f(0.40f, 0.40f, 0.40f));

        return new ItemTransforms(
            ItemTransform.NO_TRANSFORM,
            thirdpersonRighthand,
            firstpersonLefthand,
            firstpersonRighthand,
            ItemTransform.NO_TRANSFORM,
            gui,
            ground,
            fixed);
    }

    protected BakedModel missingModel() {
        return Minecraft.getInstance().getModelManager().getMissingModel();
    }

    // Standard faces for a unit cube.
    protected static final Vec3[][] UnitCube = new Vec3[][]{
        {new Vec3(0, 0, 1), new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 0, 1)},
        {new Vec3(0, 1, 0), new Vec3(0, 1, 1), new Vec3(1, 1, 1), new Vec3(1, 1, 0)},
        {new Vec3(1, 1, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 0), new Vec3(0, 1, 0)},
        {new Vec3(0, 1, 1), new Vec3(0, 0, 1), new Vec3(1, 0, 1), new Vec3(1, 1, 1)},
        {new Vec3(0, 1, 0), new Vec3(0, 0, 0), new Vec3(0, 0, 1), new Vec3(0, 1, 1)},
        {new Vec3(1, 1, 1), new Vec3(1, 0, 1), new Vec3(1, 0, 0), new Vec3(1, 1, 0)}
    };

    // Planes perpendicular to facings. Negative values mean we mirror along that,
    // axis which is done to mirror back faces and the y axis (because up is
    // positive but for our texture coordinates down is positive).
    @SuppressWarnings("unchecked")
    protected static final Pair<Vec3, Vec3>[] Planes = new Pair[]{
        Pair.of(new Vec3(1, 0, 0), new Vec3(0, 0, -1)),
        Pair.of(new Vec3(1, 0, 0), new Vec3(0, 0, 1)),
        Pair.of(new Vec3(-1, 0, 0), new Vec3(0, -1, 0)),
        Pair.of(new Vec3(1, 0, 0), new Vec3(0, -1, 0)),
        Pair.of(new Vec3(0, 0, 1), new Vec3(0, -1, 0)),
        Pair.of(new Vec3(0, 0, -1), new Vec3(0, -1, 0))
    };

    protected static final int White = 0xFFFFFF;

    /**
     * Generates a list of arrays, each containing the four vertices making up a
     * face of the box with the specified size.
     */
    protected static Vec3[][] makeBox(Vec3 from, Vec3 to) {
        final double minX = Math.min(from.x, to.x);
        final double minY = Math.min(from.y, to.y);
        final double minZ = Math.min(from.z, to.z);
        final double maxX = Math.max(from.x, to.x);
        final double maxY = Math.max(from.y, to.y);
        final double maxZ = Math.max(from.z, to.z);
        final Vec3[][] result = new Vec3[UnitCube.length][];
        for (int i = 0; i < UnitCube.length; i++) {
            final Vec3[] face = UnitCube[i];
            result[i] = new Vec3[face.length];
            for (int j = 0; j < face.length; j++) {
                final Vec3 vertex = face[j];
                result[i][j] = new Vec3(
                    Math.max(minX, Math.min(maxX, vertex.x)),
                    Math.max(minY, Math.min(maxY, vertex.y)),
                    Math.max(minZ, Math.min(maxZ, vertex.z)));
            }
        }
        return result;
    }

    protected static Vec3[][] makeBox(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        return makeBox(new Vec3(fromX, fromY, fromZ), new Vec3(toX, toY, toZ));
    }

    protected static Vec3 rotateVector(Vec3 v, double angle, Vec3 axis) {
        // vrot = v * cos(angle) + (axis x v) * sin(angle) + axis * (axis dot v)(1 - cos(angle))
        final double cosAngle = Math.cos(angle);
        final double sinAngle = Math.sin(angle);
        return v.scale(cosAngle).
            add(axis.cross(v).scale(sinAngle)).
            add(axis.scale(axis.dot(v) * (1 - cosAngle)));
    }

    protected static Vec3[] rotateFace(Vec3[] face, double angle, Vec3 axis, Vec3 around) {
        final Vec3[] result = new Vec3[face.length];
        for (int i = 0; i < face.length; i++) {
            result[i] = rotateVector(face[i].subtract(around), angle, axis).add(around);
        }
        return result;
    }

    protected static Vec3[][] rotateBox(Vec3[][] box, double angle) {
        return rotateBox(box, angle, new Vec3(0, 1, 0), new Vec3(0.5, 0.5, 0.5));
    }

    protected static Vec3[][] rotateBox(Vec3[][] box, double angle, Vec3 axis, Vec3 around) {
        final Vec3[][] result = new Vec3[box.length][];
        for (int i = 0; i < box.length; i++) {
            result[i] = rotateFace(box[i], angle, axis, around);
        }
        return result;
    }

    protected static TextureAtlasSprite[] fill6(TextureAtlasSprite sprite) {
        return new TextureAtlasSprite[]{sprite, sprite, sprite, sprite, sprite, sprite};
    }

    /**
     * Create the BakedQuads for a set of quads defined by the specified vertices.
     * <p/>
     * Usually used to generate the quads for a cube previously generated using makeBox().
     */
    protected static BakedQuad[] bakeQuads(Vec3[][] box, TextureAtlasSprite[] texture, Optional<Integer> color) {
        return bakeQuads(box, texture, color.orElse(White));
    }

    /**
     * Create the BakedQuads for a set of quads defined by the specified vertices.
     * <p/>
     * Usually used to generate the quads for a cube previously generated using makeBox().
     */
    protected static BakedQuad[] bakeQuads(Vec3[][] box, TextureAtlasSprite[] texture, int colorRGB) {
        final Direction[] sides = Direction.values();
        final BakedQuad[] result = new BakedQuad[sides.length];
        for (Direction side : sides) {
            final Vec3[] vertices = box[side.get3DDataValue()];
            final int[] data = quadData(vertices, side, texture[side.get3DDataValue()], colorRGB, 0);
            result[side.ordinal()] = new BakedQuad(data, -1, side, texture[side.get3DDataValue()], true);
        }
        return result;
    }

    /**
     * Create a single BakedQuad of a unit cube's specified side.
     */
    protected static BakedQuad bakeQuad(Direction side, TextureAtlasSprite texture, Optional<Integer> color, int rotation) {
        final int colorRGB = color.orElse(White);
        final Vec3[] vertices = UnitCube[side.get3DDataValue()];
        final int[] data = quadData(vertices, side, texture, colorRGB, rotation);
        return new BakedQuad(data, -1, side, texture, true);
    }

    // Generate raw data used for a BakedQuad based on the specified facing, vertices, texture and rotation.
    // The UV coordinates are generated from the positions of the vertices, i.e. they are simply cube-
    // mapped. This is good enough for us.
    protected static int[] quadData(Vec3[] vertices, Direction facing, TextureAtlasSprite texture, int colorRGB, int rotation) {
        final Pair<Vec3, Vec3> plane = Planes[facing.get3DDataValue()];
        final Vec3 uAxis = plane.getLeft();
        final Vec3 vAxis = plane.getRight();
        final int rot = (rotation + 4) % 4;
        final int[] result = new int[vertices.length * 8];
        for (int n = 0; n < vertices.length; n++) {
            final Vec3 vertex = vertices[n];
            double u = vertex.dot(uAxis);
            double v = vertex.dot(vAxis);
            if (uAxis.x + uAxis.y + uAxis.z < 0) u = 1 + u;
            if (vAxis.x + vAxis.y + vAxis.z < 0) v = 1 + v;
            for (int i = 0; i < rot; i++) {
                // (u, v) = (v, -u)
                final double tmp = u;
                u = v;
                v = (-(tmp - 0.5)) + 0.5;
            }
            System.arraycopy(rawData(vertex.x, vertex.y, vertex.z, facing, texture, texture.getU(u * 16), texture.getV(v * 16), colorRGB), 0, result, n * 8, 8);
        }
        return result;
    }

    // See FaceBakery#fillVertex.
    protected static int[] rawData(double x, double y, double z, Direction face, TextureAtlasSprite texture, float u, float v, int colorRGB) {
        final int vx = (face.getStepX() * 127) & 0xFF;
        final int vy = (face.getStepY() * 127) & 0xFF;
        final int vz = (face.getStepZ() * 127) & 0xFF;

        final int r = (colorRGB >> 16) & 0xFF;
        final int g = (colorRGB >> 8) & 0xFF;
        final int b = colorRGB & 0xFF;

        return new int[]{
            Float.floatToRawIntBits((float) x),
            Float.floatToRawIntBits((float) y),
            Float.floatToRawIntBits((float) z),
            // The vertex color is RGBA in byte order, i.e. ABGR as a little-endian int. No manual face
            // shading: the quads are baked with shade = true, so the renderer already shades them
            // (shading here as well made e.g. 3D prints too dark, KosmosPrime fork f4864df5c).
            0xFF000000 | b << 16 | g << 8 | r,
            Float.floatToRawIntBits(u),
            Float.floatToRawIntBits(v),
            0, vx | (vy << 0x08) | (vz << 0x10)
        };
    }

    protected static void addAll(List<BakedQuad> target, BakedQuad[] quads) {
        Collections.addAll(target, quads);
    }

    protected static List<BakedQuad> newQuadList() {
        return new ArrayList<>();
    }

    /**
     * Item override list resolving to a model computed from the stack.
     * <p>
     * Note: {@code ItemOverrides()} is protected on Forge (access transformer)
     * and made accessible on Fabric through the access widener.
     */
    public static class StackOverrides extends ItemOverrides {
        private final Function<ItemStack, BakedModel> resolver;

        public StackOverrides(Function<ItemStack, BakedModel> resolver) {
            super();
            this.resolver = resolver;
        }

        @Nullable
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            return resolver.apply(stack);
        }
    }
}
