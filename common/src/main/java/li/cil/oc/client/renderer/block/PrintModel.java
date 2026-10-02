package li.cil.oc.client.renderer.block;

import com.google.common.base.Strings;
import li.cil.oc.Settings;
import li.cil.oc.client.KeyBindings;
import li.cil.oc.client.Textures;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.tileentity.Print;
import li.cil.oc.util.Color;
import li.cil.oc.util.ExtendedAABB;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class PrintModel extends SmartBlockModelBase {
    public static final PrintModel INSTANCE = new PrintModel();

    private final ItemOverrides itemOverride = new StackOverrides(ItemModel::new);

    private PrintModel() {
    }

    @Override
    public ItemOverrides getOverrides() {
        return itemOverride;
    }

    @Override
    public List<BakedQuad> getBlockQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof Print print) {
            if (side != null) return Collections.emptyList();
            final List<BakedQuad> faces = newQuadList();

            for (PrintData.Shape shape : print.shapes()) {
                if (Strings.isNullOrEmpty(shape.texture)) continue;
                final AABB bounds = ExtendedAABB.rotateTowards(shape.bounds, print.facing());
                final TextureAtlasSprite texture = resolveTexture(shape.texture);
                addAll(faces, bakeQuads(makeBox(ExtendedAABB.minVec(bounds), ExtendedAABB.maxVec(bounds)), fill6(texture), shape.tint.orElse(White)));
            }

            return faces;
        }
        return super.getQuads(state, side, rand);
    }

    static TextureAtlasSprite resolveTexture(String name) {
        try {
            final TextureAtlasSprite texture = Textures.getSprite(new ResourceLocation(name));
            if (texture.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                // Legacy texture names (1.12 style "blocks/x" without the folder).
                return Textures.getSprite(new ResourceLocation("minecraft:block/" + name));
            }
            return texture;
        } catch (Throwable t) {
            return Textures.getSprite(MissingTextureAtlasSprite.getLocation());
        }
    }

    public static final class ItemModel extends SmartBlockModelBase {
        private final PrintData data;

        public ItemModel(ItemStack stack) {
            this.data = new PrintData(stack);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            if (side != null) return super.getQuads(state, side, rand);
            final List<BakedQuad> faces = newQuadList();

            final Collection<PrintData.Shape> shapes =
                (data.hasActiveState() && KeyBindings.showExtendedTooltips())
                    ? data.stateOn
                    : data.stateOff;
            for (PrintData.Shape shape : shapes) {
                final AABB bounds = shape.bounds;
                final TextureAtlasSprite texture = resolveTexture(shape.texture);
                addAll(faces, bakeQuads(makeBox(ExtendedAABB.minVec(bounds), ExtendedAABB.maxVec(bounds)), fill6(texture), shape.tint.orElse(White)));
            }
            if (shapes.isEmpty()) {
                final AABB bounds = ExtendedAABB.unitBounds();
                final TextureAtlasSprite texture = resolveTexture(Settings.resourceDomain + ":blocks/white");
                addAll(faces, bakeQuads(makeBox(ExtendedAABB.minVec(bounds), ExtendedAABB.maxVec(bounds)), fill6(texture), Color.rgbValues.get(DyeColor.LIME)));
            }

            return faces;
        }
    }
}
