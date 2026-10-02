package li.cil.oc.common.item;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableMap;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.client.ItemClientHooks;
import li.cil.oc.common.Tier;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.item.data.TabletData;
import li.cil.oc.common.item.traits.Chargeable;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.util.Audio;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.Rarity;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Tablet extends SimpleItem implements CustomModel, Chargeable {
    public final int TimeToAnalyze = 10;

    public Tablet(Properties props) {
        super(props);
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected void tooltipExtended(ItemStack stack, List<Component> tooltip) {
        if (li.cil.oc.util.Tooltip.showExtended()) {
            final TabletData info = new TabletData(stack);
            // Ignore/hide the screen.
            final ItemStack[] components = Arrays.copyOfRange(info.items, Math.min(1, info.items.length), info.items.length);
            if (components.length > 1) {
                for (String curr : Tooltip.get("server.Components")) {
                    tooltip.add(Component.literal(curr).setStyle(Tooltip.DefaultStyle));
                }
                for (ItemStack component : components) {
                    if (!component.isEmpty()) {
                        tooltip.add(Component.literal("- " + component.getHoverName().getString()).setStyle(Tooltip.DefaultStyle));
                    }
                }
            }
        }
    }

    @Override
    public net.minecraft.world.item.Rarity getRarity(ItemStack stack) {
        final TabletData data = new TabletData(stack);
        return Rarity.byTier(data.tier);
    }

    // Formerly showDurabilityBar / getDurabilityForDisplay.
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    public double getDurabilityForDisplay(ItemStack stack) {
        if (stack.hasTag()) {
            final Optional<TabletWrapper> weak = Client.INSTANCE.getWeak(stack);
            final TabletData data = weak.isPresent() ? weak.get().data : new TabletData(stack);
            return 1 - data.energy / data.maxEnergy;
        } else return 1.0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return barWidth(getDurabilityForDisplay(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return barColor(getDurabilityForDisplay(stack));
    }

    // ----------------------------------------------------------------------- //

    private static ResourceLocation modelLocationFromState(Optional<Boolean> running) {
        final String suffix = running.map(state -> state ? "_on" : "_off").orElse("");
        return new ResourceLocation(Settings.resourceDomain, "item/" + Constants.ItemName.Tablet + suffix);
    }

    @Override
    public ResourceLocation getModelLocation(ItemStack stack) {
        return modelLocationFromState(Client.INSTANCE.getWeak(stack).map(tablet -> tablet.data.isRunning));
    }

    @Override
    public List<ResourceLocation> modelLocations() {
        return Arrays.asList(modelLocationFromState(Optional.empty()), modelLocationFromState(Optional.of(true)), modelLocationFromState(Optional.of(false)));
    }

    @Override
    public boolean canCharge(ItemStack stack) {
        return true;
    }

    @Override
    public double charge(ItemStack stack, double amount, boolean simulate) {
        if (amount < 0) return amount;
        final TabletData data = new TabletData(stack);
        return Chargeable.applyCharge(amount, data.energy, data.maxEnergy, used -> {
            if (!simulate) {
                data.energy += used;
                data.saveData(stack);
            }
        });
    }

    // ----------------------------------------------------------------------- //

    // Must be assembled to be usable so we hide it in the item list (no creative tab, see Items).

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (entity instanceof Player player) {
            // Play an audio cue to let players know when they finished analyzing a block.
            if (world.isClientSide && player.getUseItemRemainingTicks() == TimeToAnalyze && li.cil.oc.api.Items.get(player.getUseItem()) == li.cil.oc.api.Items.get(Constants.ItemName.Tablet)) {
                Audio.play((float) player.getX(), (float) player.getY() + 2, (float) player.getZ(), ".");
            }
            get(stack, player).update(world, player, slot, selected);
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        currentlyAnalyzing = Optional.of(new AnalyzeContext(BlockPosition.apply(pos, world), side, hitX, hitY, hitZ));
        return super.onItemUseFirst(stack, player, world, pos, side, hitX, hitY, hitZ, hand);
    }

    @Override
    public boolean onItemUse(ItemStack stack, Player player, BlockPosition position, Direction side, float hitX, float hitY, float hitZ) {
        player.startUsingItem(player.getItemInHand(InteractionHand.MAIN_HAND) == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level world, Player player) {
        player.startUsingItem(player.getItemInHand(InteractionHand.MAIN_HAND) == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity entity, int duration) {
        if (!(entity instanceof Player player)) return;
        final boolean didAnalyze = getUseDuration(stack) - duration >= TimeToAnalyze;
        if (didAnalyze) {
            if (!world.isClientSide) {
                if (currentlyAnalyzing.isPresent()) {
                    final AnalyzeContext ctx = currentlyAnalyzing.get();
                    try {
                        final li.cil.oc.api.machine.Machine computer = get(stack, player).machine();
                        if (computer.isRunning()) {
                            final CompoundTag data = new CompoundTag();
                            computer.node().sendToReachable("tablet.use", data, stack, player, ctx.position(), ctx.side(), ctx.hitX(), ctx.hitY(), ctx.hitZ());
                            if (!data.isEmpty()) {
                                computer.signal("tablet_use", data);
                            }
                        }
                    } catch (Throwable t) {
                        OpenComputers.log.warn("Block analysis on tablet right click failed gloriously!", t);
                    }
                }
            }
        } else {
            if (player.isCrouching()) {
                if (!world.isClientSide) {
                    final TabletWrapper tablet = Server.INSTANCE.get(stack, player);
                    tablet.machine().stop();
                    if (tablet.data.tier > Tier.One && player instanceof ServerPlayer srvPlr) {
                        ContainerTypes.openTabletGui(srvPlr, get(stack, player));
                    }
                }
            } else {
                if (!world.isClientSide) {
                    final li.cil.oc.api.machine.Machine computer = get(stack, player).machine();
                    computer.start();
                    final String message = computer.lastError();
                    if (message != null) {
                        player.sendSystemMessage(Localization.Analyzer.LastError(message));
                    }
                } else {
                    for (Optional<ManagedEnvironment> component : get(stack, player).components()) {
                        if (component.isPresent() && component.get() instanceof TextBuffer buffer) {
                            ItemClientHooks.openTabletScreen(buffer);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public double maxCharge(ItemStack stack) {
        return new TabletData(stack).maxEnergy;
    }

    @Override
    public double getCharge(ItemStack stack) {
        return new TabletData(stack).energy;
    }

    @Override
    public void setCharge(ItemStack stack, double amount) {
        final TabletData data = new TabletData(stack);
        data.energy = Math.min(Math.max(0.0, amount), maxCharge(stack));
        data.saveData(stack);
    }

    // ----------------------------------------------------------------------- //
    // Companion object.

    public record AnalyzeContext(BlockPosition position, Direction side, float hitX, float hitY, float hitZ) {
    }

    // This is super-hacky, but since it's only used on the client we get away
    // with storing context information for analyzing a block in the singleton.
    public static Optional<AnalyzeContext> currentlyAnalyzing = Optional.empty();

    public static String getId(ItemStack stack) {
        final CompoundTag data = stack.getOrCreateTag();
        if (!data.contains(Settings.namespace + "tablet")) {
            data.putString(Settings.namespace + "tablet", UUID.randomUUID().toString());
        }
        return data.getString(Settings.namespace + "tablet");
    }

    public static TabletWrapper get(ItemStack stack, Player holder) {
        if (holder.level().isClientSide) return Client.INSTANCE.get(stack, holder);
        else return Server.INSTANCE.get(stack, holder);
    }

    private static boolean registered;

    /**
     * Registers the former Forge event subscriptions (world save / unload, client / server
     * tick). Called from {@code ModOpenComputers}.
     */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        LifecycleEvent.SERVER_LEVEL_SAVE.register(Tablet::onWorldSave);
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(Tablet::onWorldUnload);
        TickEvent.SERVER_POST.register(server -> onServerTick());
        if (Platform.getEnvironment() == Env.CLIENT) {
            ItemClientHooks.registerTabletClientEvents();
        }
    }

    public static void onWorldSave(Level world) {
        Server.INSTANCE.saveAll(world);
    }

    public static void onWorldUnload(Level world) {
        Client.INSTANCE.clear(world);
        Server.INSTANCE.clear(world);
    }

    /** Client tick; {@code paused} is true while an integrated server is paused. */
    public static void onClientTick(boolean integratedServerPaused) {
        Client.INSTANCE.cleanUp();
        if (integratedServerPaused) {
            // While the game is paused, manually keep all tablets alive, to avoid
            // them being cleared from the cache, causing them to stop.
            Client.INSTANCE.keepAlive();
            Server.INSTANCE.keepAlive();
        }
    }

    public static void onServerTick() {
        Server.INSTANCE.cleanUp();
    }

    public abstract static class TabletCache implements Callable<TabletWrapper>, RemovalListener<String, TabletWrapper> {
        public final Cache<String, TabletWrapper> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(timeout(), TimeUnit.SECONDS)
            .removalListener(this)
            .build();

        protected int timeout() {
            return 10;
        }

        // To allow access in cache entry init.
        private ItemStack currentStack;

        private Player currentHolder;

        public TabletWrapper get(ItemStack stack, Player holder) {
            final String id = getId(stack);
            synchronized (cache) {
                currentStack = stack;
                currentHolder = holder;

                // if the item is still cached, we can detect if it is dirty (client side only)
                if (holder.level().isClientSide) {
                    final Optional<TabletWrapper> weak = Client.INSTANCE.getWeak(stack);
                    if (weak.isPresent()) {
                        final int timesChanged = holder.getInventory().getTimesChanged();
                        if (timesChanged != weak.get().timesChanged) {
                            if (!weak.get().isDirty) {
                                weak.get().isDirty = true;
                                li.cil.oc.client.PacketSender.sendMachineItemStateRequest(stack);
                            }
                            weak.get().timesChanged = timesChanged;
                        }
                    }
                }

                TabletWrapper wrapper = load(id);

                // Force re-load on world change, in case some components store a
                // reference to the world object.
                if (holder.level() != wrapper.world) {
                    wrapper.writeToNBT(false);
                    wrapper.autoSave = false;
                    cache.invalidate(id);
                    cache.cleanUp();
                    wrapper = load(id);
                }

                currentStack = null;
                currentHolder = null;

                wrapper.stack = stack;
                wrapper.player = holder;
                return wrapper;
            }
        }

        private TabletWrapper load(String id) {
            try {
                return cache.get(id, this);
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }
        }

        @Override
        public TabletWrapper call() {
            return new TabletWrapper(currentStack, currentHolder);
        }

        @Override
        public void onRemoval(RemovalNotification<String, TabletWrapper> e) {
            final TabletWrapper tablet = e.getValue();
            if (tablet != null && tablet.node() != null) {
                // Server.
                if (tablet.autoSave) tablet.writeToNBT();
                tablet.machine().stop();
                for (Node node : tablet.machine().node().network().nodes()) {
                    node.remove();
                }
                if (tablet.autoSave) tablet.writeToNBT();
                tablet.setChanged();
            }
        }

        public void clear(Level world) {
            synchronized (cache) {
                final List<String> tabletsInWorld = new ArrayList<>();
                for (Map.Entry<String, TabletWrapper> entry : cache.asMap().entrySet()) {
                    if (entry.getValue().world == world) tabletsInWorld.add(entry.getKey());
                }
                cache.invalidateAll(tabletsInWorld);
                cache.cleanUp();
            }
        }

        /** Clears all tablets not belonging to the given world (client level changes / disconnects). */
        public void clearAllExcept(Level world) {
            synchronized (cache) {
                final List<String> tablets = new ArrayList<>();
                for (Map.Entry<String, TabletWrapper> entry : cache.asMap().entrySet()) {
                    if (entry.getValue().world != world) tablets.add(entry.getKey());
                }
                cache.invalidateAll(tablets);
                cache.cleanUp();
            }
        }

        public void cleanUp() {
            synchronized (cache) {
                cache.cleanUp();
            }
        }

        public ImmutableMap<String, TabletWrapper> keepAlive() {
            // Just touching to update last access time.
            return cache.getAllPresent(new ArrayList<>(cache.asMap().keySet()));
        }
    }

    public static final class Client extends TabletCache {
        public static final Client INSTANCE = new Client();

        private Client() {
        }

        @Override
        protected int timeout() {
            return 5;
        }

        public Optional<TabletWrapper> getWeak(ItemStack stack) {
            final String key = getId(stack);
            return Optional.ofNullable(cache.asMap().get(key));
        }

        public Optional<TabletWrapper> get(ItemStack stack) {
            if (stack.hasTag() && stack.getTag().contains(Settings.namespace + "tablet")) {
                final String id = stack.getTag().getString(Settings.namespace + "tablet");
                synchronized (cache) {
                    return Optional.ofNullable(cache.getIfPresent(id));
                }
            } else return Optional.empty();
        }
    }

    public static final class Server extends TabletCache {
        public static final Server INSTANCE = new Server();

        private Server() {
        }

        public void saveAll(Level world) {
            synchronized (cache) {
                for (TabletWrapper tablet : cache.asMap().values()) {
                    if (tablet.world == world) tablet.writeToNBT();
                }
            }
        }
    }
}
