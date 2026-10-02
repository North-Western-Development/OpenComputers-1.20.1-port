package li.cil.oc.common.item;

import dev.architectury.event.EventResult;
import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.server.PacketSender;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Arrays;

public class Analyzer extends SimpleItem {
    private static ItemInfo analyzer;

    private static ItemInfo analyzer() {
        if (analyzer == null) analyzer = li.cil.oc.api.Items.get(Constants.ItemName.Analyzer);
        return analyzer;
    }

    private static boolean registered;

    /** Registers {@link #onInteract} (formerly a Forge event subscriber). Called from {@code ModOpenComputers}. */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        dev.architectury.event.events.common.InteractionEvent.INTERACT_ENTITY.register(Analyzer::onInteract);
    }

    /**
     * Formerly a Forge {@code PlayerInteractEvent.EntityInteract} subscriber; registered on
     * Architectury's {@code InteractionEvent.INTERACT_ENTITY} by {@link #register()}.
     */
    public static EventResult onInteract(Player player, Entity target, InteractionHand hand) {
        final ItemStack held = player.getItemInHand(hand);
        if (li.cil.oc.api.Items.get(held) == analyzer()) {
            if (analyze(target, player, Direction.DOWN, 0, 0, 0)) {
                player.swing(hand);
                return EventResult.interruptTrue();
            }
        }
        return EventResult.pass();
    }

    public static boolean analyze(Object thing, Player player, Direction side, float hitX, float hitY, float hitZ) {
        final Level world = player.level();
        if (thing instanceof Analyzable analyzable) {
            if (!world.isClientSide) {
                analyzeNodes(analyzable.onAnalyze(player, side, hitX, hitY, hitZ), player);
            }
            return true;
        } else if (thing instanceof SidedEnvironment host) {
            if (!world.isClientSide) {
                analyzeNodes(new Node[]{host.sidedNode(side)}, player);
            }
            return true;
        } else if (thing instanceof Environment host) {
            if (!world.isClientSide) {
                analyzeNodes(new Node[]{host.node()}, player);
            }
            return true;
        }
        return false;
    }

    private static void analyzeNodes(Node[] nodes, Player player) {
        if (nodes == null) return;
        for (Node node : nodes) {
            if (node == null) continue;
            if (PlatformHooks.isFakePlayer(player)) continue; // Nope
            if (!(player instanceof ServerPlayer playerMP)) continue;
            if (node.host() instanceof Machine machine) {
                if (machine.lastError() != null) {
                    playerMP.sendSystemMessage(Localization.Analyzer.LastError(machine.lastError()));
                }
                playerMP.sendSystemMessage(Localization.Analyzer.Components(machine.componentCount(), machine.maxComponents()));
                final String[] list = machine.users();
                if (list.length > 0) {
                    playerMP.sendSystemMessage(Localization.Analyzer.Users(Arrays.asList(list)));
                }
            }
            if (node instanceof Connector connector) {
                if (connector.localBufferSize() > 0) {
                    playerMP.sendSystemMessage(Localization.Analyzer.StoredEnergy(String.format("%.2f/%.2f", connector.localBuffer(), connector.localBufferSize())));
                }
                playerMP.sendSystemMessage(Localization.Analyzer.TotalEnergy(String.format("%.2f/%.2f", connector.globalBuffer(), connector.globalBufferSize())));
            }
            if (node instanceof li.cil.oc.api.network.Component component) {
                playerMP.sendSystemMessage(Localization.Analyzer.ComponentName(component.name()));
            }
            final String address = node.address();
            if (address != null && !address.isEmpty()) {
                playerMP.sendSystemMessage(Localization.Analyzer.Address(address));
                PacketSender.sendAnalyze(address, playerMP);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    public Analyzer(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level world, Player player) {
        if (player.isCrouching() && stack.hasTag()) {
            stack.removeTagKey(Settings.namespace + "clipboard");
        }
        return super.use(stack, world, player);
    }

    @Override
    public boolean onItemUse(ItemStack stack, Player player, BlockPosition position, Direction side, float hitX, float hitY, float hitZ) {
        final Level world = player.level();
        final BlockEntity blockEntity = ExtendedWorld.getBlockEntity(world, position);
        if (blockEntity instanceof li.cil.oc.common.tileentity.Screen screen && side == screen.facing()) {
            if (player.isCrouching()) {
                return screen.copyToAnalyzer(hitX, hitY, hitZ);
            } else if (stack.hasTag() && stack.getTag().contains(Settings.namespace + "clipboard")) {
                if (!world.isClientSide) {
                    screen.origin.buffer().clipboard(stack.getTag().getString(Settings.namespace + "clipboard"), player);
                }
                return true;
            } else return false;
        }
        return analyze(ExtendedWorld.getBlockEntity(position.world.get(), position), player, side, hitX, hitY, hitZ);
    }
}
