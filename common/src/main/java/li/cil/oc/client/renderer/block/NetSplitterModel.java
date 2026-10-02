package li.cil.oc.client.renderer.block;

import li.cil.oc.client.Textures;
import li.cil.oc.common.tileentity.NetSplitter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class NetSplitterModel extends SmartBlockModelBase {
    public static final NetSplitterModel INSTANCE = new NetSplitterModel();

    private final ItemOverrides itemOverride = new StackOverrides(stack -> ItemModel.INSTANCE);

    private NetSplitterModel() {
    }

    @Override
    public ItemOverrides getOverrides() {
        return itemOverride;
    }

    @Override
    public List<BakedQuad> getBlockQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof NetSplitter splitter) {
            if (side != null) return Collections.emptyList();
            final List<BakedQuad> faces = newQuadList();

            faces.addAll(baseModel());
            final Direction[] directions = Direction.values();
            final boolean[] openSides = new boolean[directions.length];
            for (Direction dir : directions) {
                openSides[dir.ordinal()] = splitter.isSideOpen(dir);
            }
            addSideQuads(faces, openSides);

            return faces;
        }
        return super.getQuads(state, side, rand);
    }

    static TextureAtlasSprite[] splitterTexture() {
        final TextureAtlasSprite top = Textures.getSprite(Textures.Block.NetSplitterTop);
        final TextureAtlasSprite sideTex = Textures.getSprite(Textures.Block.NetSplitterSide);
        return new TextureAtlasSprite[]{top, top, sideTex, sideTex, sideTex, sideTex};
    }

    // The static part of the model, regenerated whenever the atlas changes
    // (the original regenerated it on texture stitch).
    private static TextureAtlasSprite baseModelSprite = null;
    private static List<BakedQuad> baseModel = Collections.emptyList();

    static synchronized List<BakedQuad> baseModel() {
        final TextureAtlasSprite current = Textures.getSprite(Textures.Block.NetSplitterTop);
        if (current != baseModelSprite) {
            baseModelSprite = current;
            baseModel = generateBaseModel();
        }
        return baseModel;
    }

    private static List<BakedQuad> generateBaseModel() {
        final List<BakedQuad> faces = new ArrayList<>();
        final TextureAtlasSprite[] tex = splitterTexture();

        // Bottom.
        addAll(faces, bakeQuads(makeBox(0 / 16f, 0 / 16f, 5 / 16f, 5 / 16f, 5 / 16f, 11 / 16f), tex, Optional.empty()));
        addAll(faces, bakeQuads(makeBox(11 / 16f, 0 / 16f, 5 / 16f, 16 / 16f, 5 / 16f, 11 / 16f), tex, Optional.empty()));
        addAll(faces, bakeQuads(makeBox(5 / 16f, 0 / 16f, 0 / 16f, 11 / 16f, 5 / 16f, 5 / 16f), tex, Optional.empty()));
        addAll(faces, bakeQuads(makeBox(5 / 16f, 0 / 16f, 11 / 16f, 11 / 16f, 5 / 16f, 16 / 16f), tex, Optional.empty()));
        // Corners.
        addAll(faces, bakeQuads(makeBox(0 / 16f, 0 / 16f, 0 / 16f, 5 / 16f, 16 / 16f, 5 / 16f), tex, Optional.empty()));
        addAll(faces, bakeQuads(makeBox(11 / 16f, 0 / 16f, 0 / 16f, 16 / 16f, 16 / 16f, 5 / 16f), tex, Optional.empty()));
        addAll(faces, bakeQuads(makeBox(0 / 16f, 0 / 16f, 11 / 16f, 5 / 16f, 16 / 16f, 16 / 16f), tex, Optional.empty()));
        addAll(faces, bakeQuads(makeBox(11 / 16f, 0 / 16f, 11 / 16f, 16 / 16f, 16 / 16f, 16 / 16f), tex, Optional.empty()));
        // Top.
        addAll(faces, bakeQuads(makeBox(0 / 16f, 11 / 16f, 5 / 16f, 5 / 16f, 16 / 16f, 11 / 16f), tex, Optional.empty()));
        addAll(faces, bakeQuads(makeBox(11 / 16f, 11 / 16f, 5 / 16f, 16 / 16f, 16 / 16f, 11 / 16f), tex, Optional.empty()));
        addAll(faces, bakeQuads(makeBox(5 / 16f, 11 / 16f, 0 / 16f, 11 / 16f, 16 / 16f, 5 / 16f), tex, Optional.empty()));
        addAll(faces, bakeQuads(makeBox(5 / 16f, 11 / 16f, 11 / 16f, 11 / 16f, 16 / 16f, 16 / 16f), tex, Optional.empty()));

        return Collections.unmodifiableList(faces);
    }

    static void addSideQuads(List<BakedQuad> faces, boolean[] openSides) {
        final TextureAtlasSprite[] tex = splitterTexture();

        final boolean down = openSides[Direction.DOWN.ordinal()];
        addAll(faces, bakeQuads(makeBox(5 / 16f, down ? 0 / 16f : 2 / 16f, 5 / 16f, 11 / 16f, 5 / 16f, 11 / 16f), tex, Optional.empty()));

        final boolean up = openSides[Direction.UP.ordinal()];
        addAll(faces, bakeQuads(makeBox(5 / 16f, 11 / 16f, 5 / 16f, 11 / 16f, up ? 16 / 16f : 14f / 16f, 11 / 16f), tex, Optional.empty()));

        final boolean north = openSides[Direction.NORTH.ordinal()];
        addAll(faces, bakeQuads(makeBox(5 / 16f, 5 / 16f, north ? 0 / 16f : 2 / 16f, 11 / 16f, 11 / 16f, 5 / 16f), tex, Optional.empty()));

        final boolean south = openSides[Direction.SOUTH.ordinal()];
        addAll(faces, bakeQuads(makeBox(5 / 16f, 5 / 16f, 11 / 16f, 11 / 16f, 11 / 16f, south ? 16 / 16f : 14 / 16f), tex, Optional.empty()));

        final boolean west = openSides[Direction.WEST.ordinal()];
        addAll(faces, bakeQuads(makeBox(west ? 0 / 16f : 2 / 16f, 5 / 16f, 5 / 16f, 5 / 16f, 11 / 16f, 11 / 16f), tex, Optional.empty()));

        final boolean east = openSides[Direction.EAST.ordinal()];
        addAll(faces, bakeQuads(makeBox(11 / 16f, 5 / 16f, 5 / 16f, east ? 16 / 16f : 14 / 16f, 11 / 16f, 11 / 16f), tex, Optional.empty()));
    }

    public static final class ItemModel extends SmartBlockModelBase {
        public static final ItemModel INSTANCE = new ItemModel();

        private ItemModel() {
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            if (side != null) return super.getQuads(state, side, rand);
            final List<BakedQuad> faces = newQuadList();

            faces.addAll(baseModel());
            addSideQuads(faces, new boolean[Direction.values().length]);

            return faces;
        }
    }
}
