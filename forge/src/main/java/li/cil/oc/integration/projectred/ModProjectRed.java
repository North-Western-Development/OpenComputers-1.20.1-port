package li.cil.oc.integration.projectred;

import li.cil.oc.api.IMC;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.util.BlockPosition;
import mrtjp.projectred.api.ProjectRedAPI;
import net.minecraft.core.Direction;

/**
 * Integration with ProjectRed Transmission (mod id {@code projectred_transmission}, Forge only):
 * OC blocks with bundled redstone (redstone I/O, computers with a tier 2 redstone card) read
 * from and emit to ProjectRed bundled cables; ProjectRed screwdrivers count as wrenches.
 */
public final class ModProjectRed implements ModProxy, BundledRedstone.RedstoneProvider {
    public static final ModProjectRed INSTANCE = new ModProjectRed();

    private ModProjectRed() {
    }

    @Override
    public Mod getMod() {
        return Mods.ProjectRedTransmission;
    }

    @Override
    public void initialize() {
        IMC.registerWrenchTool("li.cil.oc.integration.projectred.EventHandlerProjectRed.useWrench");
        IMC.registerWrenchToolCheck("li.cil.oc.integration.projectred.EventHandlerProjectRed.isWrench");

        BundledRedstone.addProvider(this);
        BundledProviderProjectRed.install();
    }

    @Override
    public int computeInput(final BlockPosition pos, final Direction side) {
        return 0;
    }

    @Override
    public int[] computeBundledInput(final BlockPosition pos, final Direction side) {
        if (ProjectRedAPI.transmissionAPI == null || pos.world.isEmpty()) {
            return null;
        }
        final byte[] input = ProjectRedAPI.transmissionAPI.getBundledInput(pos.world.get(), pos.toBlockPos(), side);
        if (input == null) {
            return null;
        }
        final int[] result = new int[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = input[i] & 0xFF;
        }
        return result;
    }
}
