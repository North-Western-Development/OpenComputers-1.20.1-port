package li.cil.oc.integration.jade;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.block.SimpleBlock;
import li.cil.oc.common.tileentity.traits.TileEntity;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade plugin entry (formerly {@code waila.BlockDataProvider}'s plugin class).
 * <p>
 * Loaded on both sides: {@link #register} adds the server data provider (also on dedicated servers),
 * {@link #registerClient} is only called on the client and adds the tooltip component. Neither this
 * class nor the providers reference client-only Minecraft classes.
 */
@WailaPlugin
public final class JadePlugin implements IWailaPlugin {
    public static final ResourceLocation Uid = new ResourceLocation(OpenComputers.ID, "block_data");
    public static final ResourceLocation ConfigAddress = new ResourceLocation(OpenComputers.ID, "address");
    public static final ResourceLocation ConfigEnergy = new ResourceLocation(OpenComputers.ID, "energy");
    public static final ResourceLocation ConfigComponentName = new ResourceLocation(OpenComputers.ID, "componentname");

    @Override
    public void register(IWailaCommonRegistration registration) {
        // All OC block entities derive from traits.TileEntity; Jade's lookup needs a class, the
        // provider itself filters for Environment / SidedEnvironment like the old interface registration.
        registration.registerBlockDataProvider(BlockDataProvider.INSTANCE, TileEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addConfig(ConfigAddress, true);
        registration.addConfig(ConfigEnergy, true);
        registration.addConfig(ConfigComponentName, true);
        registration.registerBlockComponent(BlockComponentProvider.INSTANCE, SimpleBlock.class);
    }
}
