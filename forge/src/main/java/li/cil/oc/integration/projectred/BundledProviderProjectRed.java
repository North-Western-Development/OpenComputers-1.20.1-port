package li.cil.oc.integration.projectred;

import li.cil.oc.common.tileentity.traits.BundledRedstoneAware;
import mrtjp.projectred.api.IBundledTileInteraction;
import mrtjp.projectred.api.ProjectRedAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Lets ProjectRed bundled cables read the bundled output of OC blocks. {@code side} is the side
 * of the OC block facing the cable.
 */
public final class BundledProviderProjectRed implements IBundledTileInteraction {
    public static final BundledProviderProjectRed INSTANCE = new BundledProviderProjectRed();

    private BundledProviderProjectRed() {
    }

    public static void install() {
        ProjectRedAPI.transmissionAPI.registerBundledTileInteraction(INSTANCE);
    }

    @Override
    public boolean isValidInteractionFor(final Level world, final BlockPos pos, final Direction side) {
        return world.getBlockEntity(pos) instanceof BundledRedstoneAware;
    }

    @Override
    public boolean canConnectBundled(final Level world, final BlockPos pos, final Direction side) {
        return world.getBlockEntity(pos) instanceof BundledRedstoneAware tileEntity && tileEntity.isOutputEnabled();
    }

    @Override
    public byte[] getBundledSignal(final Level world, final BlockPos pos, final Direction side) {
        if (!(world.getBlockEntity(pos) instanceof BundledRedstoneAware tileEntity)) {
            return null;
        }
        final int[] output = tileEntity.getBundledOutput(side);
        final byte[] result = new byte[output.length];
        for (int i = 0; i < output.length; i++) {
            result[i] = (byte) Math.min(Math.max(output[i], 0), 255);
        }
        return result;
    }
}
