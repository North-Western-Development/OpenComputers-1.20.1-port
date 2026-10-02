package li.cil.oc.client.renderer.block;

import li.cil.oc.client.Textures;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Item model of the drone. The drone item uses {@code builtin/entity}, its
 * quads are emitted by {@link li.cil.oc.client.renderer.item.DroneItemRenderer}.
 */
public final class DroneModel extends SmartBlockModelBase {
    public static final DroneModel INSTANCE = new DroneModel();

    private final ItemOverrides itemOverride = new StackOverrides(stack -> INSTANCE);

    private DroneModel() {
    }

    @Override
    public ItemOverrides getOverrides() {
        return itemOverride;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if (side != null) return super.getQuads(state, side, rand);
        final List<BakedQuad> faces = newQuadList();

        final TextureAtlasSprite[] texture = fill6(droneTexture());
        for (Vec3[][] box : boxes()) {
            addAll(faces, bakeQuads(box, texture, Optional.empty()));
        }

        return faces;
    }

    private static TextureAtlasSprite droneTexture() {
        return Textures.getSprite(Textures.Item.DroneItem);
    }

    private static Vec3[][][] boxes() {
        return new Vec3[][][]{
            makeBox(1f / 16f, 7f / 16f, 1f / 16f, 7f / 16f, 8f / 16f, 7f / 16f),
            makeBox(1f / 16f, 7f / 16f, 9f / 16f, 7f / 16f, 8f / 16f, 15f / 16f),
            makeBox(9f / 16f, 7f / 16f, 1f / 16f, 15f / 16f, 8f / 16f, 7f / 16f),
            makeBox(9f / 16f, 7f / 16f, 9f / 16f, 15f / 16f, 8f / 16f, 15f / 16f),
            rotateBox(makeBox(6f / 16f, 6f / 16f, 6f / 16f, 10f / 16f, 9f / 16f, 10f / 16f), 45)
        };
    }
}
