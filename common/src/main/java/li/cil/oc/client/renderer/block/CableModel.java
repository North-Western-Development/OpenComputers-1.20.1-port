package li.cil.oc.client.renderer.block;

import li.cil.oc.client.Textures;
import li.cil.oc.common.block.property.PropertyCableConnection;
import li.cil.oc.common.tileentity.Cable;
import li.cil.oc.util.Color;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class CableModel extends SmartBlockModelBase {
    public static final CableModel INSTANCE = new CableModel();

    private final ItemOverrides itemOverride = new StackOverrides(ItemModel::new);

    private CableModel() {
    }

    @Override
    public ItemOverrides getOverrides() {
        return itemOverride;
    }

    @Override
    public List<BakedQuad> getBlockQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof Cable cable && side == null && state != null) {
            final int color = cable.getColor();
            final List<BakedQuad> faces = newQuadList();

            addAll(faces, bakeQuads(Middle, cableTexture(), color));
            final Direction[] directions = Direction.values();
            int numConnected = 0;
            for (Direction d : directions) {
                if (state.getValue(PropertyCableConnection.BY_DIRECTION.get(d)) != PropertyCableConnection.Shape.NONE) {
                    numConnected++;
                }
            }
            for (Direction dir : directions) {
                final PropertyCableConnection.Shape shape = state.getValue(PropertyCableConnection.BY_DIRECTION.get(dir));
                final boolean connected = shape != PropertyCableConnection.Shape.NONE;
                final boolean isCableOnSide = shape == PropertyCableConnection.Shape.CABLE;
                final Vec3[][][] boxes = Connected[dir.get3DDataValue()];
                final Vec3[][] plug = boxes[0];
                final Vec3[][] shortBody = boxes[1];
                final Vec3[][] longBody = boxes[2];
                if (connected) {
                    if (isCableOnSide) {
                        addAll(faces, bakeQuads(longBody, cableTexture(), color));
                    } else {
                        addAll(faces, bakeQuads(shortBody, cableTexture(), color));
                        addAll(faces, bakeQuads(plug, cableCapTexture(), Optional.empty()));
                    }
                } else {
                    final boolean otherConn = state.getValue(PropertyCableConnection.BY_DIRECTION.get(dir.getOpposite())) != PropertyCableConnection.Shape.NONE;
                    if ((otherConn && numConnected == 1) || numConnected == 0) {
                        addAll(faces, bakeQuads(Disconnected[dir.get3DDataValue()], cableCapTexture(), Optional.empty()));
                    }
                }
            }

            return faces;
        }
        return super.getQuads(state, side, rand);
    }

    static final Vec3[][] Middle = makeBox(6 / 16f, 6 / 16f, 6 / 16f, 10 / 16f, 10 / 16f, 10 / 16f);

    // Per side, always plug + short cable + long cable (no plug).
    static final Vec3[][][][] Connected = new Vec3[][][][]{
        {makeBox(5 / 16f, 0 / 16f, 5 / 16f, 11 / 16f, 1 / 16f, 11 / 16f),
            makeBox(6 / 16f, 1 / 16f, 6 / 16f, 10 / 16f, 6 / 16f, 10 / 16f),
            makeBox(6 / 16f, 0 / 16f, 6 / 16f, 10 / 16f, 6 / 16f, 10 / 16f)},
        {makeBox(5 / 16f, 15 / 16f, 5 / 16f, 11 / 16f, 16 / 16f, 11 / 16f),
            makeBox(6 / 16f, 10 / 16f, 6 / 16f, 10 / 16f, 15 / 16f, 10 / 16f),
            makeBox(6 / 16f, 10 / 16f, 6 / 16f, 10 / 16f, 16 / 16f, 10 / 16f)},
        {makeBox(5 / 16f, 5 / 16f, 0 / 16f, 11 / 16f, 11 / 16f, 1 / 16f),
            makeBox(6 / 16f, 6 / 16f, 1 / 16f, 10 / 16f, 10 / 16f, 6 / 16f),
            makeBox(6 / 16f, 6 / 16f, 0 / 16f, 10 / 16f, 10 / 16f, 6 / 16f)},
        {makeBox(5 / 16f, 5 / 16f, 15 / 16f, 11 / 16f, 11 / 16f, 16 / 16f),
            makeBox(6 / 16f, 6 / 16f, 10 / 16f, 10 / 16f, 10 / 16f, 15 / 16f),
            makeBox(6 / 16f, 6 / 16f, 10 / 16f, 10 / 16f, 10 / 16f, 16 / 16f)},
        {makeBox(0 / 16f, 5 / 16f, 5 / 16f, 1 / 16f, 11 / 16f, 11 / 16f),
            makeBox(1 / 16f, 6 / 16f, 6 / 16f, 6 / 16f, 10 / 16f, 10 / 16f),
            makeBox(0 / 16f, 6 / 16f, 6 / 16f, 6 / 16f, 10 / 16f, 10 / 16f)},
        {makeBox(15 / 16f, 5 / 16f, 5 / 16f, 16 / 16f, 11 / 16f, 11 / 16f),
            makeBox(10 / 16f, 6 / 16f, 6 / 16f, 15 / 16f, 10 / 16f, 10 / 16f),
            makeBox(10 / 16f, 6 / 16f, 6 / 16f, 16 / 16f, 10 / 16f, 10 / 16f)}
    };

    // Per side, cap only.
    static final Vec3[][][] Disconnected = new Vec3[][][]{
        makeBox(6 / 16f, 5 / 16f, 6 / 16f, 10 / 16f, 6 / 16f, 10 / 16f),
        makeBox(6 / 16f, 10 / 16f, 6 / 16f, 10 / 16f, 11 / 16f, 10 / 16f),
        makeBox(6 / 16f, 6 / 16f, 5 / 16f, 10 / 16f, 10 / 16f, 6 / 16f),
        makeBox(6 / 16f, 6 / 16f, 10 / 16f, 10 / 16f, 10 / 16f, 11 / 16f),
        makeBox(5 / 16f, 6 / 16f, 6 / 16f, 6 / 16f, 10 / 16f, 10 / 16f),
        makeBox(10 / 16f, 6 / 16f, 6 / 16f, 11 / 16f, 10 / 16f, 10 / 16f)
    };

    static TextureAtlasSprite[] cableTexture() {
        return fill6(Textures.getSprite(Textures.Block.Cable));
    }

    static TextureAtlasSprite[] cableCapTexture() {
        return fill6(Textures.getSprite(Textures.Block.CableCap));
    }

    public static final class ItemModel extends SmartBlockModelBase {
        private final ItemStack stack;

        public ItemModel(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            if (side != null) return super.getQuads(state, side, rand);

            final List<BakedQuad> faces = newQuadList();

            final int color = ItemColorizer.hasColor(stack) ? ItemColorizer.getColor(stack) : Color.rgbValues.get(DyeColor.LIGHT_GRAY);

            addAll(faces, bakeQuads(Middle, cableTexture(), color));
            addAll(faces, bakeQuads(Connected[0][1], cableTexture(), color));
            addAll(faces, bakeQuads(Connected[1][1], cableTexture(), color));
            addAll(faces, bakeQuads(Connected[0][0], cableCapTexture(), Optional.empty()));
            addAll(faces, bakeQuads(Connected[1][0], cableCapTexture(), Optional.empty()));

            return faces;
        }
    }
}
