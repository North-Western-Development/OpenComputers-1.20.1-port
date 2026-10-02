package li.cil.oc.server.driver;

import com.google.common.base.Strings;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompoundBlockDriver implements DriverBlock {
    public final DriverBlock[] sidedBlocks;

    public CompoundBlockDriver(DriverBlock[] sidedBlocks) {
        this.sidedBlocks = sidedBlocks;
    }

    @Override
    public CompoundBlockEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        final List<Pair<String, ManagedEnvironment>> list = new ArrayList<>();
        for (DriverBlock driver : sidedBlocks) {
            final ManagedEnvironment environment = driver.createEnvironment(world, pos, side);
            if (environment != null) {
                list.add(Pair.of(driver.getClass().getName(), environment));
            }
        }
        if (list.isEmpty()) return null;
        final List<ManagedEnvironment> environments = new ArrayList<>();
        for (Pair<String, ManagedEnvironment> entry : list) environments.add(entry.getRight());
        return new CompoundBlockEnvironment(cleanName(tryGetName(world, pos, environments)), list);
    }

    @Override
    public boolean worksWith(Level world, BlockPos pos, Direction side) {
        for (DriverBlock driver : sidedBlocks) {
            if (!driver.worksWith(world, pos, side)) return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CompoundBlockDriver multi && multi.sidedBlocks.length == sidedBlocks.length) {
            final Set<DriverBlock> other = new HashSet<>(Arrays.asList(multi.sidedBlocks));
            int count = 0;
            for (DriverBlock driver : sidedBlocks) {
                if (other.remove(driver)) count++;
            }
            return count == sidedBlocks.length;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashSet<>(Arrays.asList(sidedBlocks)).hashCode();
    }

    // TODO rework this method
    private String tryGetName(Level world, BlockPos pos, List<ManagedEnvironment> environments) {
        NamedBlock best = null;
        for (ManagedEnvironment environment : environments) {
            if (environment instanceof NamedBlock named && (best == null || named.priority() >= best.priority())) {
                best = named;
            }
        }
        if (best != null) return best.preferredName();
        try {
            final Block block = world.getBlockState(pos).getBlock();
            if (block.asItem() != null) {
                final ItemStack stack = new ItemStack(block, 1);
                final String id = stack.getDescriptionId();
                return id.startsWith("tile.") ? id.substring("tile.".length()) : id;
            }
        } catch (Throwable ignored) {
        }
        try {
            final BlockEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity != null) {
                final ResourceLocation key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(tileEntity.getType());
                if (key != null) return key.getPath();
            }
        } catch (Throwable ignored) {
        }
        return "component";
    }

    private static String cleanName(String name) {
        final String safeStart = name.matches("^[^a-zA-Z_]") ? "_" + name : name;
        final String identifier = safeStart.replaceAll("[^\\w_]", "_").trim();
        if (Strings.isNullOrEmpty(identifier)) return "component";
        return identifier.toLowerCase();
    }
}
