package li.cil.oc.integration.minecraft;

import li.cil.oc.Settings;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.GeolyzerEvent;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class EventHandlerVanilla {
    private EventHandlerVanilla() {
    }

    private static boolean registered = false;

    /**
     * Registers the geolyzer listeners with OC's API event bus.
     */
    public static void register() {
        if (registered) return;
        registered = true;
        EventBus.INSTANCE.register(GeolyzerEvent.Scan.class, EventHandlerVanilla::onGeolyzerScan);
        EventBus.INSTANCE.register(GeolyzerEvent.Analyze.class, EventHandlerVanilla::onGeolyzerAnalyze);
    }

    public static void onGeolyzerScan(GeolyzerEvent.Scan e) {
        final Level world = e.host.world();
        final BlockPosition blockPos = BlockPosition.apply(e.host);
        final Object includeReplaceableOption = e.options.get("includeReplaceable");
        final boolean includeReplaceable = !(includeReplaceableOption instanceof Boolean value) || value;

        final byte[] noise = new byte[e.data.length];
        world.random.nextBytes(noise);
        // Map to [-1, 1). The additional /33f is for normalization below.
        for (int i = 0; i < noise.length; i++) {
            e.data[i] = noise[i] / 128f / 33f;
        }

        final int w = e.maxX - e.minX + 1;
        final int d = e.maxZ - e.minZ + 1;
        for (int ry = e.minY; ry <= e.maxY; ry++) {
            for (int rz = e.minZ; rz <= e.maxZ; rz++) {
                for (int rx = e.minX; rx <= e.maxX; rx++) {
                    final BlockPos pos = blockPos.toBlockPos().offset(rx, ry, rz);
                    final int index = (rx - e.minX) + ((rz - e.minZ) + (ry - e.minY) * d) * w;
                    if (world.isLoaded(pos) && !world.isEmptyBlock(pos)) {
                        final BlockState blockState = world.getBlockState(pos);
                        if (!blockState.isAir() && (includeReplaceable || blockState.getBlock() instanceof LiquidBlock || !blockState.canBeReplaced())) {
                            final float distance = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
                            e.data[index] = e.data[index] * distance * Settings.get().geolyzerNoise + blockState.getDestroySpeed(world, pos);
                        } else e.data[index] = 0;
                    } else e.data[index] = 0;
                }
            }
        }
    }

    private static Optional<Float> getGrowth(BlockState blockState) {
        for (Property<?> prop : blockState.getProperties()) {
            if (prop instanceof IntegerProperty propAge && "age".equals(prop.getName())) {
                int max = 0;
                for (Integer value : propAge.getPossibleValues()) {
                    max = Math.max(max, value);
                }
                final float growth = blockState.getValue(propAge).floatValue() / max;
                return Optional.of(Math.min(1f, Math.max(0f, growth)));
            }
        }
        return Optional.empty();
    }

    public static void onGeolyzerAnalyze(GeolyzerEvent.Analyze e) {
        final Level world = e.host.world();
        final BlockState blockState = world.getBlockState(e.pos);
        final Block block = blockState.getBlock();
        final BlockPosition position = BlockPosition.apply(e.pos, world);

        e.data.put("name", BuiltInRegistries.BLOCK.getKey(block).toString());
        e.data.put("hardness", blockState.getDestroySpeed(world, e.pos));
        e.data.put("harvestLevel", ExtendedWorld.getBlockHarvestLevel(world, position));
        e.data.put("harvestTool", ExtendedWorld.getBlockHarvestTool(world, position));
        e.data.put("color", blockState.getMapColor(world, e.pos).col);

        // backward compatibility
        e.data.put("metadata", 0);

        final Map<String, Object> props = new HashMap<>();
        for (Property<?> prop : blockState.getProperties()) {
            props.put(prop.getName(), blockState.getValue(prop));
        }
        e.data.put("properties", props);

        if (Settings.get().insertIdsInConverters) {
            e.data.put("id", Block.getId(blockState));
        }

        final Optional<Float> growth;
        if (block instanceof CropBlock || block instanceof StemBlock || block == Blocks.COCOA || block == Blocks.NETHER_WART || block == Blocks.CHORUS_FLOWER) {
            growth = getGrowth(blockState);
        } else if (block == Blocks.MELON || block == Blocks.PUMPKIN || block == Blocks.CACTUS || block == Blocks.SUGAR_CANE || block == Blocks.CHORUS_PLANT) {
            growth = Optional.of(1f);
        } else {
            growth = Optional.empty();
        }
        growth.ifPresent(value -> e.data.put("growth", value));
    }
}
