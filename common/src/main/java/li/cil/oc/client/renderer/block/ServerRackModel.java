package li.cil.oc.client.renderer.block;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.RackMountableRenderEvent;
import li.cil.oc.client.Textures;
import li.cil.oc.common.tileentity.Rack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ServerRackModel extends SmartBlockModelBase {
    public final BakedModel parent;

    private final ItemOverrides itemOverride;

    public ServerRackModel(BakedModel parent) {
        this.parent = parent;
        this.itemOverride = new StackOverrides(stack -> parent);
    }

    @Override
    public ItemOverrides getOverrides() {
        return itemOverride;
    }

    @Override
    public ItemTransforms getTransforms() {
        return parent != null ? parent.getTransforms() : super.getTransforms();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        // Without block entity data (e.g. block breaking / moving pistons) use the plain model.
        return parent != null ? parent.getQuads(state, side, rand) : Collections.emptyList();
    }

    @Override
    public List<BakedQuad> getBlockQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof Rack rack) {
            if (side != null) return Collections.emptyList();
            final Direction facing = rack.facing();
            final List<BakedQuad> faces = newQuadList();

            for (Direction dir : Direction.values()) {
                if (dir != facing) {
                    addAll(faces, bakeQuads(Case[dir.get3DDataValue()], serverRackTexture(), Optional.empty()));
                }
            }

            final TextureAtlasSprite[] textures = serverTexture();
            final TextureAtlasSprite defaultFront = Textures.getSprite(Textures.Block.RackFront);
            for (int slot = 0; slot < 4; slot++) {
                final RackMountable mountable = rack.getMountable(slot);
                if (mountable != null) {
                    final RackMountableRenderEvent.Block event = new RackMountableRenderEvent.Block(rack, slot, rack.getMountableData(slot), side);
                    if (!EventBus.INSTANCE.post(event)) {
                        final TextureAtlasSprite front = event.getFrontTextureOverride() != null ? event.getFrontTextureOverride() : defaultFront;
                        for (int i = 2; i < 6; i++) textures[i] = front;
                        addAll(faces, bakeQuads(Servers[slot], textures, Optional.empty()));
                    }
                }
            }

            return faces;
        }
        return getQuads(state, side, rand);
    }

    private static TextureAtlasSprite[] serverRackTexture() {
        final TextureAtlasSprite top = Textures.getSprite(Textures.Block.GenericTop);
        final TextureAtlasSprite sideTex = Textures.getSprite(Textures.Block.RackSide);
        return new TextureAtlasSprite[]{top, top, sideTex, sideTex, sideTex, sideTex};
    }

    private static TextureAtlasSprite[] serverTexture() {
        final TextureAtlasSprite top = Textures.getSprite(Textures.Block.GenericTop);
        final TextureAtlasSprite front = Textures.getSprite(Textures.Block.RackFront);
        return new TextureAtlasSprite[]{top, top, front, front, front, front};
    }

    private static final Vec3[][][] Case = new Vec3[][][]{
        makeBox(0 / 16f, 0 / 16f, 0 / 16f, 16 / 16f, 2 / 16f, 16 / 16f),
        makeBox(0 / 16f, 14 / 16f, 0 / 16f, 16 / 16f, 16 / 16f, 16 / 16f),
        makeBox(0 / 16f, 2 / 16f, 0 / 16f, 16 / 16f, 14 / 16f, 0.99f / 16f),
        makeBox(0 / 16f, 2 / 16f, 15.01f / 16f, 16 / 16f, 14 / 16f, 16 / 16f),
        makeBox(0 / 16f, 2 / 16f, 0 / 16f, 0.99f / 16f, 14 / 16f, 16 / 16f),
        makeBox(15.01f / 16f, 2 / 16f, 0 / 16f, 16 / 16f, 14f / 16f, 16 / 16f)
    };

    private static final Vec3[][][] Servers = new Vec3[][][]{
        makeBox(0.5f / 16f, 11 / 16f, 0.5f / 16f, 15.5f / 16f, 14 / 16f, 15.5f / 16f),
        makeBox(0.5f / 16f, 8 / 16f, 0.5f / 16f, 15.5f / 16f, 11 / 16f, 15.5f / 16f),
        makeBox(0.5f / 16f, 5 / 16f, 0.5f / 16f, 15.5f / 16f, 8 / 16f, 15.5f / 16f),
        makeBox(0.5f / 16f, 2 / 16f, 0.5f / 16f, 15.5f / 16f, 5 / 16f, 15.5f / 16f)
    };
}
