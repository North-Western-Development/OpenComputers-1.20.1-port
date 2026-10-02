package li.cil.oc.client.renderer.block;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.client.Textures;
import li.cil.oc.common.Tier;
import li.cil.oc.common.tileentity.Screen;
import li.cil.oc.util.Color;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ScreenModel extends SmartBlockModelBase {
    public static final ScreenModel INSTANCE = new ScreenModel();

    private final ItemOverrides itemOverride = new StackOverrides(ItemModel::new);

    private ScreenModel() {
    }

    @Override
    public ItemOverrides getOverrides() {
        return itemOverride;
    }

    @Override
    public List<BakedQuad> getBlockQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, @Nullable BlockEntity blockEntity) {
        final Direction safeSide = side != null ? side : Direction.SOUTH;
        if (blockEntity instanceof Screen screen) {
            // Every face is a culled face quad (the old code also emitted the
            // south face for the unculled list, rendering it twice).
            if (side == null) return Collections.emptyList();
            final Direction facing = screen.toLocal(safeSide);

            final Pair<Integer, Integer> local = screen.localPosition();
            int px = xy2part(local.getLeft(), screen.width - 1);
            int py = xy2part(local.getRight(), screen.height - 1);
            if ((safeSide == Direction.DOWN || screen.facing() == Direction.DOWN) && safeSide != screen.facing()) {
                px = 2 - px;
                py = 2 - py;
            }
            final int rotation =
                safeSide == Direction.UP ? screen.yaw().get2DDataValue()
                    : safeSide == Direction.DOWN ? -screen.yaw().get2DDataValue()
                    : 0;

            final int pitch = screen.pitch() == Direction.NORTH ? 0 : 1;
            final ResourceLocation texture;
            if (screen.width == 1 && screen.height == 1) {
                if (facing == Direction.SOUTH)
                    texture = Textures.Block.Screen.SingleFront[pitch];
                else
                    texture = Textures.Block.Screen.Single[safeSide.get3DDataValue()];
            } else if (screen.width == 1) {
                if (facing == Direction.SOUTH)
                    texture = Textures.Block.Screen.VerticalFront[pitch][py];
                else
                    texture = Textures.Block.Screen.Vertical[pitch][py][facing.get3DDataValue()];
            } else if (screen.height == 1) {
                if (facing == Direction.SOUTH)
                    texture = Textures.Block.Screen.HorizontalFront[pitch][px];
                else
                    texture = Textures.Block.Screen.Horizontal[pitch][px][facing.get3DDataValue()];
            } else {
                if (facing == Direction.SOUTH)
                    texture = Textures.Block.Screen.MultiFront[pitch][py][px];
                else
                    texture = Textures.Block.Screen.Multi[pitch][py][px][facing.get3DDataValue()];
            }

            return Collections.singletonList(bakeQuad(safeSide, Textures.getSprite(texture), Optional.of(screen.getColor()), rotation));
        }
        return super.getQuads(state, safeSide, rand);
    }

    private static int xy2part(int value, int high) {
        return value == 0 ? 2 : (value == high ? 0 : 1);
    }

    public static final class ItemModel extends SmartBlockModelBase {
        private final DyeColor color;

        public ItemModel(ItemStack stack) {
            final ItemInfo descriptor = Items.get(stack);
            final String name = descriptor != null ? descriptor.name() : null;
            if (Constants.BlockName.ScreenTier2.equals(name)) color = Color.byTier[Tier.Two];
            else if (Constants.BlockName.ScreenTier3.equals(name)) color = Color.byTier[Tier.Three];
            else color = Color.byTier[Tier.One];
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            final ResourceLocation result =
                (side == Direction.NORTH || side == null)
                    ? Textures.Block.Screen.SingleFront[0]
                    : Textures.Block.Screen.Single[side.ordinal()];
            return Collections.singletonList(bakeQuad(side != null ? side : Direction.SOUTH, Textures.getSprite(result), Optional.of(Color.rgbValues.get(color)), 0));
        }
    }
}
