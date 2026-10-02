package li.cil.oc.client.renderer.block;

import li.cil.oc.client.Textures;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The robot block itself is rendered by its block entity renderer, this only
 * provides the item model.
 */
public final class RobotModel extends SmartBlockModelBase {
    public static final RobotModel INSTANCE = new RobotModel();

    private final ItemOverrides itemOverride = new StackOverrides(stack -> ItemModel.INSTANCE);

    private RobotModel() {
    }

    @Override
    public ItemOverrides getOverrides() {
        return itemOverride;
    }

    public static final class ItemModel extends SmartBlockModelBase {
        public static final ItemModel INSTANCE = new ItemModel();

        private static final float size = 0.4f;
        private static final float l = 0.5f - size;
        private static final float h = 0.5f + size;

        private static final float[] top = {0.5f, 1f, 0.5f, 0.25f, 0.25f};
        private static final float[] top1 = {l, 0.5f, h, 0f, 0f};
        private static final float[] top2 = {h, 0.5f, h, 0f, 0.5f};
        private static final float[] top3 = {h, 0.5f, l, 0.5f, 0.5f};
        private static final float[] top4 = {l, 0.5f, l, 0.5f, 0f};

        private static final float[] bottom = {0.5f, 0f, 0.5f, 0.75f, 0.25f};
        private static final float[] bottom1 = {l, 0.5f, l, 0.5f, 0.5f};
        private static final float[] bottom2 = {h, 0.5f, l, 0.5f, 0f};
        private static final float[] bottom3 = {h, 0.5f, h, 1f, 0f};
        private static final float[] bottom4 = {l, 0.5f, h, 1f, 0.5f};

        // I don't know why this is super-bright when using 0xFF888888 :/
        private static final int tint = 0xFF555555;

        private ItemModel() {
        }

        private static TextureAtlasSprite robotTexture() {
            return Textures.getSprite(Textures.Item.Robot);
        }

        private static float[] interpolate(float[] v0, float[] v1) {
            final float[] result = new float[5];
            for (int i = 0; i < 5; i++) {
                result[i] = v0[i] * 0.5f + v1[i] * 0.5f;
            }
            return result;
        }

        private static int[] quad(float[]... verts) {
            final TextureAtlasSprite texture = robotTexture();
            final float[] added = interpolate(verts[verts.length - 1], verts[0]);
            final int[] result = new int[(verts.length + 1) * 8];
            for (int i = 0; i <= verts.length; i++) {
                final float[] v = i < verts.length ? verts[i] : added;
                System.arraycopy(rawData(
                    (v[0] - 0.5f) * 1.4f + 0.5f,
                    (v[1] - 0.5f) * 1.4f + 0.5f,
                    (v[2] - 0.5f) * 1.4f + 0.5f,
                    Direction.UP, texture, texture.getU(v[3] * 16), texture.getV(v[4] * 16),
                    White), 0, result, i * 8, 8);
            }
            return result;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            if (side != null) return super.getQuads(state, side, rand);
            final List<BakedQuad> faces = newQuadList();
            final TextureAtlasSprite texture = robotTexture();

            faces.add(new BakedQuad(quad(top, top1, top2), tint, Direction.NORTH, texture, true));
            faces.add(new BakedQuad(quad(top, top2, top3), tint, Direction.EAST, texture, true));
            faces.add(new BakedQuad(quad(top, top3, top4), tint, Direction.SOUTH, texture, true));
            faces.add(new BakedQuad(quad(top, top4, top1), tint, Direction.WEST, texture, true));

            faces.add(new BakedQuad(quad(bottom, bottom1, bottom2), tint, Direction.NORTH, texture, true));
            faces.add(new BakedQuad(quad(bottom, bottom2, bottom3), tint, Direction.EAST, texture, true));
            faces.add(new BakedQuad(quad(bottom, bottom3, bottom4), tint, Direction.SOUTH, texture, true));
            faces.add(new BakedQuad(quad(bottom, bottom4, bottom1), tint, Direction.WEST, texture, true));

            return faces;
        }
    }
}
