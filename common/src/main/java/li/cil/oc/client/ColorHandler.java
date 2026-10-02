package li.cil.oc.client;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import li.cil.oc.Constants;
import li.cil.oc.api.internal.Colored;
import li.cil.oc.common.block.Cable;
import li.cil.oc.common.block.Case;
import li.cil.oc.common.block.ChameliumBlock;
import li.cil.oc.common.block.Screen;
import li.cil.oc.util.Color;
import li.cil.oc.util.ItemColorizer;
import li.cil.oc.util.ItemUtils;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Supplier;

public final class ColorHandler {
    private ColorHandler() {
    }

    /**
     * Registers block and item color handlers via Architectury. Blocks/items
     * are passed as suppliers, so this may (and on Forge must) be called before
     * the registries are populated, i.e. during client initialization.
     */
    public static void init() {
        register((state, world, pos, tintIndex) -> {
                    if (state.getBlock() instanceof Cable cable) return cable.colorMultiplierOverride.orElse(0xFFFFFFFF);
                    return 0xFFFFFFFF;
                },
                block(Constants.BlockName.Cable));

        register((state, world, pos, tintIndex) -> {
                    if (pos == null || world == null) return 0xFFFFFFFF;
                    final BlockEntity blockEntity = world.getBlockEntity(pos);
                    if (blockEntity instanceof Colored colored) return colored.getColor();
                    if (state.getBlock() instanceof Case caseBlock) return Color.rgbValues.get(Color.byTier[caseBlock.tier]);
                    return 0xFFFFFFFF;
                },
                block(Constants.BlockName.CaseTier1),
                block(Constants.BlockName.CaseTier2),
                block(Constants.BlockName.CaseTier3),
                block(Constants.BlockName.CaseCreative));

        register((state, world, pos, tintIndex) -> Color.rgbValues.get(state.getValue(ChameliumBlock.Color)),
                block(Constants.BlockName.ChameliumBlock));

        register((state, world, pos, tintIndex) -> tintIndex,
                block(Constants.BlockName.Print));

        register((state, world, pos, tintIndex) -> {
                    if (state.getBlock() instanceof Screen screen) return Color.rgbValues.get(Color.byTier[screen.tier]);
                    return 0xFFFFFFFF;
                },
                block(Constants.BlockName.ScreenTier1),
                block(Constants.BlockName.ScreenTier2),
                block(Constants.BlockName.ScreenTier3));

        register((stack, tintIndex) -> ItemColorizer.hasColor(stack) ? ItemColorizer.getColor(stack) : tintIndex,
                blockItem(Constants.BlockName.Cable));

        register((stack, tintIndex) -> Color.rgbValues.get(Color.byTier[ItemUtils.caseTier(stack)]),
                blockItem(Constants.BlockName.CaseTier1),
                blockItem(Constants.BlockName.CaseTier2),
                blockItem(Constants.BlockName.CaseTier3),
                blockItem(Constants.BlockName.CaseCreative));

        // TODO(port): item damage no longer encodes the color; kept for parity with 1.16.5.
        register((stack, tintIndex) -> Color.rgbValues.get(DyeColor.byId(stack.getDamageValue())),
                blockItem(Constants.BlockName.ChameliumBlock));

        register((stack, tintIndex) -> tintIndex,
                blockItem(Constants.BlockName.ScreenTier1),
                blockItem(Constants.BlockName.ScreenTier2),
                blockItem(Constants.BlockName.ScreenTier3),
                blockItem(Constants.BlockName.Print),
                blockItem(Constants.BlockName.Robot));

        register((stack, tintIndex) -> {
                    if (tintIndex == 1) {
                        return ItemColorizer.hasColor(stack) ? ItemColorizer.getColor(stack) : 0x66DD55;
                    }
                    return 0xFFFFFF;
                },
                item(Constants.ItemName.HoverBoots));
    }

    private static Supplier<Block> block(String name) {
        return () -> li.cil.oc.api.Items.get(name).block();
    }

    private static Supplier<ItemLike> blockItem(String name) {
        return () -> li.cil.oc.api.Items.get(name).block();
    }

    private static Supplier<ItemLike> item(String name) {
        return () -> li.cil.oc.api.Items.get(name).item();
    }

    @SafeVarargs
    public static void register(BlockColor handler, Supplier<? extends Block>... blocks) {
        ColorHandlerRegistry.registerBlockColors(handler, blocks);
    }

    @SafeVarargs
    public static void register(ItemColor handler, Supplier<? extends ItemLike>... items) {
        ColorHandlerRegistry.registerItemColors(handler, items);
    }
}
