package li.cil.oc.client.platform.fabric;

import li.cil.oc.client.renderer.block.SmartBlockModelBase;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Fabric wrapper for OC's code generated models: looks up the block entity at
 * the rendered position and emits the quads of
 * {@link SmartBlockModelBase#getBlockQuads} through the vanilla model consumer.
 */
public final class FabricSmartBakedModel extends ForwardingBakedModel {
    private final SmartBlockModelBase model;

    public FabricSmartBakedModel(SmartBlockModelBase model) {
        this.model = model;
        this.wrapped = model;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        final BlockEntity blockEntity = blockView.getBlockEntity(pos);
        context.bakedModelConsumer().accept(new BlockQuads(model, blockEntity), state);
    }

    /**
     * Vanilla model view of the block quads for one block entity.
     */
    private static final class BlockQuads implements BakedModel {
        private final SmartBlockModelBase model;
        @Nullable
        private final BlockEntity blockEntity;

        BlockQuads(SmartBlockModelBase model, @Nullable BlockEntity blockEntity) {
            this.model = model;
            this.blockEntity = blockEntity;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return model.getBlockQuads(state, side, rand, blockEntity);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return model.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return model.isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return model.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return model.getParticleIcon();
        }

        @Override
        public ItemTransforms getTransforms() {
            return model.getTransforms();
        }

        @Override
        public ItemOverrides getOverrides() {
            return model.getOverrides();
        }
    }
}
