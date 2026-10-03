package li.cil.oc.integration.appeng;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageHelper;
import appeng.crafting.CraftingLink;
import appeng.me.service.CraftingService;
import com.google.common.collect.ImmutableSet;
import dev.architectury.utils.GameInstance;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.common.EventHandler;
import li.cil.oc.server.component.UpgradeDatabase;
import li.cil.oc.util.DatabaseAccess;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.NbtDataStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Function;

import static li.cil.oc.util.ResultWrapper.result;

/**
 * Callbacks shared by all components that give access to a whole ME network (controller and
 * interfaces).
 * <p>
 * Differences to the 1.16 version (AE2 15 API):
 * <ul>
 * <li>Item stacks are {@link AEItemKey}s with long amounts; {@code size} may exceed the int range.</li>
 * <li>{@code getItemsInNetwork} additionally lists craftable items that are not in stock with
 *     {@code size = 0} (old AE2 did that implicitly).</li>
 * <li>{@code getCraftables} also lists craftable fluids (as fluid tables) since AE2 crafting is
 *     generic now.</li>
 * <li>{@code getCpus} entries additionally contain {@code crafting} (the stack being crafted, if busy).</li>
 * <li>{@code request(amount, prioritizePower, cpuName)} works as before; a request returns
 *     {@code nil, reason} from {@code isDone()/isCanceled()} while AE2 is still computing the plan,
 *     and fails with AE2's submit error code (e.g. {@code missing resources: ...}).</li>
 * <li>Power values are AE units, like before.</li>
 * </ul>
 */
// Note to self: this class is used by ExtraCells (and potentially others), do not rename / drastically change it.
public interface NetworkControl {
    /** The OC node of the environment. */
    Node node();

    /** The AE2 grid node this component accesses the network through, if it's online. */
    @Nullable
    IGridNode gridNode();

    /** The block entity hosting the AE2 machine (for persistence of crafting requests). */
    BlockEntity hostEntity();

    /** The side of the part on the host, or {@code null} for full blocks. */
    @Nullable
    Direction partSide();

    // ----------------------------------------------------------------------- //

    private Object[] withGrid(Function<IGrid, Object[]> f) {
        final IGridNode node = gridNode();
        final IGrid grid = node != null ? node.getGrid() : null;
        if (grid == null) return result(null, "no ae grid");
        return f.apply(grid);
    }

    private static Map<Object, Object> getFilter(Arguments args, int index) {
        final Map<Object, Object> hash = new HashMap<>();
        final Map<?, ?> table = args.optTable(index, Map.of());
        if (table != null) {
            for (Map.Entry<?, ?> e : table.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) hash.put(e.getKey(), e.getValue());
            }
        }
        return hash;
    }

    /** All stored keys plus craftable keys that are not in stock (with amount 0). */
    private static Map<AEKey, Long> allStacks(IGrid grid, boolean itemsOnly) {
        final Map<AEKey, Long> result = new LinkedHashMap<>();
        final KeyCounter stored = AEUtil.getGridStorage(grid).getCachedInventory();
        for (Object2LongMap.Entry<AEKey> entry : stored) {
            if (!itemsOnly || entry.getKey() instanceof AEItemKey) {
                result.put(entry.getKey(), entry.getLongValue());
            }
        }
        for (AEKey key : AEUtil.getGridCrafting(grid).getCraftables(k -> !itemsOnly || k instanceof AEItemKey)) {
            result.putIfAbsent(key, 0L);
        }
        return result;
    }

    /** Amount produced by one craft of the first pattern that outputs the key. */
    private static long craftAmount(ICraftingService crafting, AEKey key) {
        for (IPatternDetails pattern : crafting.getCraftingFor(key)) {
            for (GenericStack output : pattern.getOutputs()) {
                if (output != null && key.equals(output.what())) return output.amount();
            }
        }
        return 1;
    }

    private static Map<Object, Object> convert(IGrid grid, AEKey key, long amount) {
        final ICraftingService crafting = AEUtil.getGridCrafting(grid);
        final boolean isCraftable = crafting.isCraftable(key);
        final Map<Object, Object> hash = AEUtil.convert(key, amount > 0 || !isCraftable ? amount : craftAmount(crafting, key));
        hash.put("isCraftable", isCraftable);
        if (key instanceof AEFluidKey fluid) {
            hash.put("amount", AEUtil.fluidToMillibuckets(fluid, amount));
        } else {
            hash.put("size", amount);
        }
        return hash;
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function():table -- Get a list of tables representing the available CPUs in the network.")
    default Object[] getCpus(Context context, Arguments args) {
        return withGrid(grid -> {
            final List<Object> buffer = new ArrayList<>();
            for (ICraftingCPU cpu : AEUtil.getGridCrafting(grid).getCpus()) {
                final Map<String, Object> entry = new HashMap<>();
                entry.put("name", cpu.getName() != null ? cpu.getName().getString() : "");
                entry.put("storage", cpu.getAvailableStorage());
                entry.put("coprocessors", cpu.getCoProcessors());
                entry.put("busy", cpu.isBusy());
                final var status = cpu.getJobStatus();
                if (status != null && status.crafting() != null) {
                    entry.put("crafting", AEUtil.convert(status.crafting().what(), status.crafting().amount()));
                }
                buffer.add(entry);
            }
            return result((Object) buffer.toArray());
        });
    }

    @Callback(doc = "function([filter:table]):table -- Get a list of known item recipes. These can be used to issue crafting requests.")
    default Object[] getCraftables(Context context, Arguments args) {
        final Map<Object, Object> filter = getFilter(args, 0);
        return withGrid(grid -> {
            final List<Object> craftables = new ArrayList<>();
            final ICraftingService crafting = AEUtil.getGridCrafting(grid);
            for (AEKey key : crafting.getCraftables(k -> true)) {
                if (filter.isEmpty() || matches(convert(grid, key, craftAmount(crafting, key)), filter)) {
                    craftables.add(new Craftable(this, key));
                }
            }
            return result((Object) craftables.toArray());
        });
    }

    @Callback(doc = "function([filter:table]):table -- Get a list of the stored items in the network.")
    default Object[] getItemsInNetwork(Context context, Arguments args) {
        final Map<Object, Object> filter = getFilter(args, 0);
        return withGrid(grid -> {
            final List<Object> items = new ArrayList<>();
            for (Map.Entry<AEKey, Long> entry : allStacks(grid, true).entrySet()) {
                final Map<Object, Object> converted = convert(grid, entry.getKey(), entry.getValue());
                if (matches(converted, filter)) items.add(converted);
            }
            return result((Object) items.toArray());
        });
    }

    @Callback(doc = "function([filter:table, dbAddress:string, startSlot:number, count:number]): bool -- Store items in the network matching the specified filter in the database with the specified address.")
    default Object[] store(Context context, Arguments args) {
        final Map<Object, Object> filter = getFilter(args, 0);
        final String address = args.optString(1, null);
        final UpgradeDatabase database;
        if (address != null) {
            database = DatabaseAccess.database(node(), address);
        } else {
            final List<UpgradeDatabase> databases = DatabaseAccess.databases(node());
            if (databases.isEmpty()) throw new IllegalArgumentException("no database upgrade found");
            database = databases.get(0);
        }
        return withGrid(grid -> {
            final List<ItemStack> items = new ArrayList<>();
            for (Map.Entry<AEKey, Long> entry : allStacks(grid, true).entrySet()) {
                if (matches(convert(grid, entry.getKey(), entry.getValue()), filter)) {
                    items.add(((AEItemKey) entry.getKey()).toStack());
                }
            }
            final int offset = ExtendedArguments.optSlot(args, database.data, 2, 0);
            final int count = Math.min(Math.min(args.optInteger(3, Integer.MAX_VALUE), database.size() - offset), items.size());
            int slot = offset;
            for (int i = 0; i < count; i++) {
                while (slot < database.size() && !database.getStackInSlot(slot).isEmpty()) slot++;
                if (slot >= database.size()) break;
                database.setStackInSlot(slot, items.get(i).copy());
            }
            return result(true);
        });
    }

    @Callback(doc = "function():table -- Get a list of the stored fluids in the network.")
    default Object[] getFluidsInNetwork(Context context, Arguments args) {
        return withGrid(grid -> {
            final List<Object> fluids = new ArrayList<>();
            for (Object2LongMap.Entry<AEKey> entry : AEUtil.getGridStorage(grid).getCachedInventory()) {
                if (entry.getKey() instanceof AEFluidKey) {
                    fluids.add(AEUtil.toStack(entry.getKey(), entry.getLongValue()));
                }
            }
            return result((Object) fluids.toArray());
        });
    }

    private Object[] withEnergy(Function<IEnergyService, Object> f) {
        return withGrid(grid -> result(f.apply(AEUtil.getGridEnergy(grid))));
    }

    @Callback(doc = "function():number -- Get the average power injection into the network.")
    default Object[] getAvgPowerInjection(Context context, Arguments args) {
        return withEnergy(IEnergyService::getAvgPowerInjection);
    }

    @Callback(doc = "function():number -- Get the average power usage of the network.")
    default Object[] getAvgPowerUsage(Context context, Arguments args) {
        return withEnergy(IEnergyService::getAvgPowerUsage);
    }

    @Callback(doc = "function():number -- Get the idle power usage of the network.")
    default Object[] getIdlePowerUsage(Context context, Arguments args) {
        return withEnergy(IEnergyService::getIdlePowerUsage);
    }

    @Callback(doc = "function():number -- Get the maximum stored power in the network.")
    default Object[] getMaxStoredPower(Context context, Arguments args) {
        return withEnergy(IEnergyService::getMaxStoredPower);
    }

    @Callback(doc = "function():number -- Get the stored power in the network. ")
    default Object[] getStoredPower(Context context, Arguments args) {
        return withEnergy(IEnergyService::getStoredPower);
    }

    @Callback(doc = "function():boolean -- True if the AE network is considered online")
    default Object[] isNetworkPowered(Context context, Arguments args) {
        return withEnergy(IEnergyService::isNetworkPowered);
    }

    @Callback(direct = false, doc = "function():number -- Returns the energy demand on the AE network")
    default Object[] getEnergyDemand(Context context, Arguments args) {
        context.consumeCallBudget(1.5);
        return withEnergy(energy -> energy.getEnergyDemand(Double.MAX_VALUE));
    }

    // ----------------------------------------------------------------------- //

    static boolean matches(@Nullable Map<Object, Object> stack, Map<Object, Object> filter) {
        if (stack == null) return false;
        for (Map.Entry<Object, Object> e : filter.entrySet()) {
            if (!stack.containsKey(e.getKey()) || !valueMatch(e.getValue(), stack.get(e.getKey()))) return false;
        }
        return true;
    }

    private static boolean valueMatch(@Nullable Object a, @Nullable Object b) {
        if ((a == null) != (b == null)) return false;
        if (a == null || a.equals(b)) return true;
        if (a instanceof Number na && b instanceof Number nb) return na.doubleValue() == nb.doubleValue();
        if (a instanceof Map<?, ?> ma && b instanceof Map<?, ?> mb) {
            for (Map.Entry<?, ?> e : ma.entrySet()) {
                if (!mb.containsKey(e.getKey()) || !valueMatch(e.getValue(), mb.get(e.getKey()))) return false;
            }
            return true;
        }
        if (a instanceof Map<?, ?> ma && b instanceof Object[] array) {
            // Lua sequence {[1]=x, [2]=y, ...} versus array: every listed value must be present.
            for (Object va : ma.values()) {
                boolean found = false;
                for (Object vb : array) {
                    if (valueMatch(va, vb)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
        return false;
    }

    // ----------------------------------------------------------------------- //

    /** Keeps crafting links and statuses together while both are being restored from NBT. */
    final class LinkCache {
        private LinkCache() {
        }

        static final Map<UUID, ICraftingLink> linkCache = new HashMap<>();
        static final Map<UUID, CraftingStatus> statusCache = new HashMap<>();

        static synchronized ICraftingLink store(ICraftingLink link) {
            final CraftingStatus status = statusCache.remove(link.getCraftingID());
            if (status != null) status.setLink(link);
            else linkCache.put(link.getCraftingID(), link);
            return link;
        }

        static synchronized void store(CraftingStatus status, UUID id) {
            final ICraftingLink link = linkCache.remove(id);
            if (link != null) status.setLink(link);
            else statusCache.put(id, status);
        }
    }

    /**
     * A craftable key in the network. Acts as the {@link ICraftingRequester} of the jobs it submits;
     * it accepts none of the crafted output, so results end up in network storage (same as with
     * the 1.16 version).
     */
    class Craftable extends AbstractValue implements ICraftingRequester, ICraftingSimulationRequester {
        private static final String DIMENSION_KEY = "dimension";
        private static final String X_KEY = "x";
        private static final String Y_KEY = "y";
        private static final String Z_KEY = "z";
        private static final String SIDE_KEY = "side";
        private static final String LINKS_KEY = "links";
        private static final String KEY_KEY = "key";

        private static final int MAX_BACKOFF_TICKS = 20 * 5; // 5 seconds
        private static final int BACKOFF_SCALE = 2; // multiply by this factor on each failure

        @Nullable
        private ResourceKey<Level> dimension;
        @Nullable
        private BlockPos pos;
        @Nullable
        private Direction side;
        @Nullable
        private AEKey stack;
        private final Set<ICraftingLink> links = new HashSet<>();
        // Links loaded from NBT that still need to be registered with the crafting service.
        private final List<ICraftingLink> pendingLinks = new ArrayList<>();
        private int delay = -1; // >= 0 while waiting for the AE network to load
        // The grid node as of the last lookup on the server thread (see node()).
        @Nullable
        private volatile IGridNode lastNode;

        public Craftable() {
        }

        public Craftable(NetworkControl controller, AEKey stack) {
            final BlockEntity host = controller.hostEntity();
            this.dimension = host.getLevel() != null ? host.getLevel().dimension() : null;
            this.pos = host.getBlockPos();
            this.side = controller.partSide();
            this.stack = stack;
        }

        @Nullable
        private Level level() {
            final MinecraftServer server = GameInstance.getServer();
            return server != null && dimension != null ? server.getLevel(dimension) : null;
        }

        @Nullable
        private IGridNode node() {
            final MinecraftServer server = GameInstance.getServer();
            if (server != null && !server.isSameThread()) {
                // AE2 computes crafting plans on a worker thread, where Level#getBlockEntity always
                // returns null; use the node last resolved on the server thread instead.
                return lastNode;
            }
            lastNode = pos != null ? AEUtil.nodeAt(level(), pos, side) : null;
            return lastNode;
        }

        // ----------------------------------------------------------------------- //

        @Override
        public ImmutableSet<ICraftingLink> getRequestedJobs() {
            return ImmutableSet.copyOf(links);
        }

        @Override
        public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
            return 0; // Let the network store the results.
        }

        @Override
        public void jobStateChange(ICraftingLink link) {
            links.remove(link);
        }

        @Nullable
        @Override
        public IGridNode getActionableNode() {
            return node();
        }

        @Nullable
        @Override
        public IActionSource getActionSource() {
            return new MachineSource(this);
        }

        // ----------------------------------------------------------------------- //

        private Object[] withGridNode(Function<IGridNode, Object[]> f) {
            if (delay >= 0) return result(null, "waiting for ae network to load");
            if (stack == null) return result(null, "no controller");
            final Level level = level();
            if (level == null || pos == null || level.getBlockEntity(pos) == null) {
                return result(null, "no controller");
            }
            final IGridNode node = node();
            if (node == null || node.getGrid() == null) return result(null, "no ae grid");
            return f.apply(node);
        }

        @Callback(doc = "function():table -- Returns the item stack representation of the crafting result.")
        public Object[] getItemStack(Context context, Arguments args) {
            return result(stack != null ? AEUtil.toStack(stack, 1) : null);
        }

        @Callback(doc = "function():number -- Returns the number of requests in progress.")
        public Object[] requesting(Context context, Arguments args) {
            return withGridNode(node -> result(AEUtil.getGridCrafting(node.getGrid()).getRequestedAmount(stack)));
        }

        @Callback(doc = "function([amount:int=1, prioritizePower:boolean=true, cpuName:string]):userdata -- Requests item to be crafted, returning an object that allows tracking the crafting status.")
        public Object[] request(Context context, Arguments args) {
            return withGridNode(node -> {
                final int count = args.optInteger(0, 1);
                final boolean prioritizePower = args.optBoolean(1, true);
                final String cpuName = args.optString(2, "");
                if (count < 1) throw new IllegalArgumentException("invalid amount");

                final ICraftingService crafting = AEUtil.getGridCrafting(node.getGrid());
                ICraftingCPU cpu = null;
                if (!cpuName.isEmpty()) {
                    for (ICraftingCPU c : crafting.getCpus()) {
                        if (c.getName() != null && cpuName.equals(c.getName().getString())) {
                            cpu = c;
                            break;
                        }
                    }
                }

                final IActionSource source = new MachineSource(this);
                final Future<ICraftingPlan> future = crafting.beginCraftingCalculation(
                        node.getLevel(), this, stack, count, CalculationStrategy.REPORT_MISSING_ITEMS);
                final CraftingStatus status = new CraftingStatus();
                final ICraftingCPU target = cpu;
                // AE2 computes plans on a background thread; poll for the result on the server thread.
                final Runnable[] poll = new Runnable[1];
                poll[0] = () -> {
                    if (!future.isDone()) {
                        EventHandler.scheduleServer(poll[0], 1);
                        return;
                    }
                    try {
                        final ICraftingPlan plan = future.get();
                        if (plan.simulation() || !plan.missingItems().isEmpty()) {
                            final StringBuilder missing = new StringBuilder();
                            for (Object2LongMap.Entry<AEKey> entry : plan.missingItems()) {
                                if (missing.length() > 0) missing.append(", ");
                                missing.append(entry.getLongValue()).append("x").append(entry.getKey().getId());
                            }
                            status.fail("missing resources: " + (missing.length() > 0 ? missing : "?"));
                            return;
                        }
                        final IGridNode current = node();
                        if (current == null || current.getGrid() == null) {
                            status.fail("no ae grid");
                            return;
                        }
                        final ICraftingSubmitResult submitted = AEUtil.getGridCrafting(current.getGrid())
                                .submitJob(plan, this, target, prioritizePower, source);
                        if (submitted.successful() && submitted.link() != null) {
                            links.add(submitted.link());
                            status.setLink(submitted.link());
                        } else {
                            status.fail(String.valueOf(submitted.errorCode()));
                        }
                    } catch (Exception e) {
                        OpenComputers.log.debug("Error submitting job to AE2.", e);
                        status.fail(e.toString());
                    }
                };
                EventHandler.scheduleServer(poll[0], 1);
                return result(status);
            });
        }

        // ----------------------------------------------------------------------- //

        // Returns true when we do not want to try again, either because we completely failed or we succeeded.
        // Returns false when things appear just not ready yet.
        private boolean tryLoadGrid() {
            final Level level = level();
            if (level == null || pos == null || !level.isLoaded(pos)) return false;
            final BlockEntity be = level.getBlockEntity(pos);
            if (be == null) return false;
            final IGridNode node = node();
            if (node == null || node.getGrid() == null) return false; // AE network still loading
            if (node.getGrid().getCraftingService() instanceof CraftingService service) {
                for (ICraftingLink link : pendingLinks) {
                    if (link instanceof CraftingLink craftingLink) service.addLink(craftingLink);
                }
            }
            pendingLinks.clear();
            return true;
        }

        private void delayLoadGrid() {
            if (delay < 0) return;
            if (tryLoadGrid()) {
                delay = -1;
            } else {
                pushDelayLoadBackoff(delay * BACKOFF_SCALE);
            }
        }

        private void pushDelayLoadBackoff(int newDelay) {
            delay = Math.max(1, Math.min(newDelay, MAX_BACKOFF_TICKS));
            EventHandler.scheduleServer(this::delayLoadGrid, delay);
        }

        @Override
        public void loadData(CompoundTag nbt) {
            super.loadData(nbt);
            stack = nbt.contains(KEY_KEY) ? AEKey.fromTagGeneric(nbt.getCompound(KEY_KEY)) : null;
            for (Tag tag : nbt.getList(LINKS_KEY, Tag.TAG_COMPOUND)) {
                try {
                    final ICraftingLink link = LinkCache.store(StorageHelper.loadCraftingLink((CompoundTag) tag, this));
                    links.add(link);
                    pendingLinks.add(link);
                } catch (Exception e) {
                    OpenComputers.log.debug("Failed restoring AE2 crafting link.", e);
                }
            }
            side = nbt.contains(SIDE_KEY) ? Direction.from3DDataValue(nbt.getInt(SIDE_KEY)) : null;
            if (nbt.contains(DIMENSION_KEY)) {
                dimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(nbt.getString(DIMENSION_KEY)));
                pos = new BlockPos(nbt.getInt(X_KEY), nbt.getInt(Y_KEY), nbt.getInt(Z_KEY));
                pushDelayLoadBackoff(1);
            }
        }

        @Override
        public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            if (stack != null) nbt.put(KEY_KEY, stack.toTagGeneric());
            final ListTag list = new ListTag();
            for (ICraftingLink link : links) {
                final CompoundTag comp = new CompoundTag();
                link.writeToNBT(comp);
                list.add(comp);
            }
            nbt.put(LINKS_KEY, list);
            if (side != null) nbt.putInt(SIDE_KEY, side.get3DDataValue());
            if (dimension != null && pos != null) {
                nbt.putString(DIMENSION_KEY, dimension.location().toString());
                nbt.putInt(X_KEY, pos.getX());
                nbt.putInt(Y_KEY, pos.getY());
                nbt.putInt(Z_KEY, pos.getZ());
            }
        }
    }

    class CraftingStatus extends AbstractValue {
        private static final String COMPUTING_KEY = "computing";
        private static final String LINK_ID_KEY = "link";
        private static final String FAILED_KEY = "failed";
        private static final String REASON_KEY = "reason";

        private boolean isComputing = true;
        @Nullable
        private ICraftingLink link;
        private boolean failed = false;
        private String reason = "no link";

        public CraftingStatus() {
        }

        public void setLink(ICraftingLink value) {
            isComputing = false;
            link = value;
        }

        public void fail(String reason) {
            isComputing = false;
            failed = true;
            this.reason = "request failed (" + reason + ")";
        }

        private Object[] asCraft(Function<ICraftingLink, Object[]> f) {
            if (isComputing) return result(null, "computing");
            if (link != null && !failed) return f.apply(link);
            return result(false, reason);
        }

        @Callback(doc = "function():boolean -- Get whether the crafting request has been canceled.")
        public Object[] isCanceled(Context context, Arguments args) {
            return asCraft(craft -> result(craft.isCanceled()));
        }

        @Callback(doc = "function():boolean -- Get whether the crafting request is done.")
        public Object[] isDone(Context context, Arguments args) {
            return asCraft(craft -> result(craft.isDone()));
        }

        @Callback(doc = "function():boolean -- Cancels the request. Returns false if the craft cannot be canceled or nil if the link is computing")
        public Object[] cancel(Context context, Arguments args) {
            return asCraft(craft -> {
                if (craft.isDone()) return result(false, "job already completed");
                craft.cancel();
                return result(true);
            });
        }

        @Override
        public void saveData(CompoundTag nbt) {
            super.saveData(nbt);
            nbt.putBoolean(COMPUTING_KEY, isComputing);
            if (link != null) nbt.putUUID(LINK_ID_KEY, link.getCraftingID());
            nbt.putBoolean(FAILED_KEY, failed);
            nbt.putString(REASON_KEY, reason);
        }

        @Override
        public void loadData(CompoundTag nbt) {
            super.loadData(nbt);
            isComputing = NbtDataStream.getOptBoolean(nbt, COMPUTING_KEY, isComputing);
            failed = NbtDataStream.getOptBoolean(nbt, FAILED_KEY, failed);
            reason = NbtDataStream.getOptString(nbt, REASON_KEY, reason);
            if (nbt.hasUUID(LINK_ID_KEY)) {
                LinkCache.store(this, nbt.getUUID(LINK_ID_KEY));
            } else if (isComputing) {
                // The plan was still being computed when saved; that computation is gone now.
                fail("interrupted by reload");
            }
        }
    }
}
