package li.cil.oc.common.item;

import com.google.common.base.Strings;
import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.client.ItemClientHooks;
import li.cil.oc.common.component.TerminalServer;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.common.tileentity.traits.TileEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public class Terminal extends SimpleItem implements CustomModel {
    public Terminal(Properties props) {
        super(props);
    }

    public boolean hasServer(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(Settings.namespace + "server");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        if (hasServer(stack)) {
            final String server = stack.getTag().getString(Settings.namespace + "server");
            tooltip.add(Component.literal("§8" + server.substring(0, 13) + "...§7"));
        }
    }

    private static ResourceLocation modelLocationFromState(boolean running) {
        return new ResourceLocation(Settings.resourceDomain, "item/" + Constants.ItemName.Terminal + (running ? "_on" : "_off"));
    }

    @Override
    public ResourceLocation getModelLocation(ItemStack stack) {
        return modelLocationFromState(hasServer(stack));
    }

    @Override
    public List<ResourceLocation> modelLocations() {
        return Arrays.asList(modelLocationFromState(true), modelLocationFromState(false));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level world, Player player) {
        if (!player.isCrouching() && stack.hasTag()) {
            final String key = stack.getTag().getString(Settings.namespace + "key");
            final String server = stack.getTag().getString(Settings.namespace + "server");
            if (key != null && !key.isEmpty() && server != null && !server.isEmpty()) {
                if (world.isClientSide) {
                    if (stack.hasTag()) {
                        final String address = stack.getTag().getString(Settings.namespace + "server");
                        final String key2 = stack.getTag().getString(Settings.namespace + "key");
                        if (!Strings.isNullOrEmpty(key2) && !Strings.isNullOrEmpty(address)) {
                            final Optional<TerminalServer> found = TerminalServer.loaded.find(address);
                            if (found.isPresent() && found.get() != null && found.get().rack != null) {
                                final TerminalServer term = found.get();
                                if (term.rack instanceof TileEntity rack && term.rack instanceof li.cil.oc.api.internal.Rack) {
                                    final BooleanSupplier inRange = () -> player.isAlive() && !rack.isRemoved() &&
                                        player.distanceToSqr(rack.x() + 0.5, rack.y() + 0.5, rack.z() + 0.5) < term.range * term.range;
                                    if (inRange.getAsBoolean()) {
                                        if (term.sidedKeys().contains(key2)) ItemClientHooks.openTerminalScreen(stack, key2, term, inRange);
                                        else player.displayClientMessage(Localization.Terminal.InvalidKey(), true);
                                    } else player.displayClientMessage(Localization.Terminal.OutOfRange(), true);
                                }
                                // else: Eh?
                            } else player.displayClientMessage(Localization.Terminal.OutOfRange(), true);
                        }
                    }
                }
                player.swing(InteractionHand.MAIN_HAND);
            }
        }
        return super.use(stack, world, player);
    }
}
