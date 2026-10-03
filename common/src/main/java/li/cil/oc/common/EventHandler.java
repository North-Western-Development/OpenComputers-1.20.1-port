package li.cil.oc.common;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.utils.GameInstance;
import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.common.item.data.MicrocontrollerData;
import li.cil.oc.common.item.data.RobotData;
import li.cil.oc.common.item.data.TabletData;
import li.cil.oc.common.mixin.ChunkMapAccessor;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.common.tileentity.Robot;
import li.cil.oc.integration.util.WirelessRedstone;
import li.cil.oc.server.PacketSender;
import li.cil.oc.server.component.Keyboard;
import li.cil.oc.server.component.RedstoneWireless;
import li.cil.oc.server.machine.Callbacks;
import li.cil.oc.server.machine.Machine;
import li.cil.oc.server.machine.luac.LuaStateFactory;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.PlayerUtils;
import li.cil.oc.util.SideTracker;
import li.cil.oc.util.UpdateCheck;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;

public final class EventHandler {
    private record TimedCallback(long time, Runnable callback) {
    }

    private static long serverTicks = 0L;
    private static final PriorityQueue<TimedCallback> pendingServerTimed = new PriorityQueue<>(Comparator.comparingLong(TimedCallback::time));

    private static final List<Runnable> pendingServer = new ArrayList<>();

    private static final List<Runnable> pendingClient = new ArrayList<>();

    private static final Set<Robot> runningRobots = Collections.synchronizedSet(new HashSet<>());

    private static final Set<Keyboard> keyboards = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private static final Set<Machine> machines = Collections.synchronizedSet(new HashSet<>());

    private static boolean registered;
    private static boolean registeredClient;

    private EventHandler() {
    }

    // ----------------------------------------------------------------------- //

    /**
     * Registers the common (both sides) event listeners. Idempotent.
     */
    public static synchronized void register() {
        if (registered) return;
        registered = true;

        TickEvent.SERVER_PRE.register(server -> onServerTickStart());
        TickEvent.SERVER_POST.register(server -> onServerTickEnd());
        PlayerEvent.PLAYER_JOIN.register(EventHandler::playerLoggedIn);
        BlockEvent.BREAK.register((level, pos, state, player, xp) -> onBlockBreak(level, pos, state, player));
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd) -> releasePressedKeys(player));
        PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> releasePressedKeys(player));
        PlayerEvent.PLAYER_QUIT.register(EventHandler::releasePressedKeys);
        EntityEvent.ADD.register(EventHandler::onEntityJoinWorld);
        // On Fabric, EntityEvent.ADD does not fire for players joining the server.
        PlayerEvent.PLAYER_JOIN.register(player -> onEntityJoinWorld(player, player.level()));
        PlayerEvent.CRAFT_ITEM.register(EventHandler::onCrafting);
        PlayerEvent.PICKUP_ITEM_POST.register((player, entity, stack) -> onPickup(player, stack));
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(EventHandler::onWorldUnload);

        // TODO(port): Forge AttachCapabilitiesEvent for ItemStack (Chargeable) / TileEntity
        //  (Environment, SidedEnvironment, Colored) is gone. Use instanceof checks on the
        //  block entity / item instead (see PORTING.md); charging goes through the transfer layer.
        // TODO(port): Forge ChunkEvent.Unload stopped machines of MachineHost *entities* (drones)
        //  in the unloaded chunk. Entities are no longer part of chunks; the drone entity must
        //  call EventHandler.scheduleClose(machine) itself when removed with
        //  RemovalReason.UNLOADED_TO_CHUNK. Block entities are handled by traits.TileEntity.
    }

    /**
     * Registers the client-only event listeners. Must only be called on the
     * physical client (from the proxies' initClient()). Idempotent.
     */
    public static synchronized void registerClient() {
        if (registeredClient) return;
        registeredClient = true;
        li.cil.oc.client.ClientEventHandler.register();
    }

    // ----------------------------------------------------------------------- //

    public static void onRobotStart(Robot robot) {
        runningRobots.add(robot);
    }

    public static void onRobotStopped(Robot robot) {
        runningRobots.remove(robot);
    }

    public static void addKeyboard(Keyboard keyboard) {
        keyboards.add(keyboard);
    }

    public static void scheduleClose(Machine machine) {
        machines.add(machine);
    }

    public static void unscheduleClose(Machine machine) {
        machines.remove(machine);
    }

    public static void scheduleServer(BlockEntity tileEntity) {
        if (SideTracker.isServer()) synchronized (pendingServer) {
            pendingServer.add(() -> Network.joinOrCreateNetwork(tileEntity));
        }
    }

    public static void scheduleServer(Runnable f) {
        synchronized (pendingServer) {
            pendingServer.add(f);
        }
    }

    public static void scheduleServer(Runnable f, int delay) {
        synchronized (pendingServerTimed) {
            pendingServerTimed.add(new TimedCallback(serverTicks + Math.max(delay, 0), f));
        }
    }

    /** Runs the callbacks queued with {@link #scheduleClient}; called by the client tick hook. */
    public static void runPendingClient() {
        runAll(pendingClient);
    }

    public static void scheduleClient(Runnable f) {
        synchronized (pendingClient) {
            pendingClient.add(f);
        }
    }

    // TODO(port): integration - EventHandler.AE2.scheduleAE2Add (Applied Energistics 2) dropped.

    public static void scheduleWirelessRedstone(RedstoneWireless rs) {
        if (SideTracker.isServer()) synchronized (pendingServer) {
            pendingServer.add(() -> {
                if (rs.node().network() != null) {
                    WirelessRedstone.addReceiver(rs);
                    WirelessRedstone.updateOutput(rs);
                }
            });
        }
    }

    // ----------------------------------------------------------------------- //

    private static void runAll(List<Runnable> callbacks) {
        final Runnable[] adds;
        synchronized (callbacks) {
            adds = callbacks.toArray(new Runnable[0]);
            callbacks.clear();
        }
        for (Runnable callback : adds) {
            try {
                callback.run();
            } catch (Throwable t) {
                OpenComputers.log.warn("Error in scheduled tick action.", t);
            }
        }
    }

    private static void onServerTickStart() {
        runAll(pendingServer);

        serverTicks += 1;
        while (true) {
            final TimedCallback next;
            synchronized (pendingServerTimed) {
                final TimedCallback head = pendingServerTimed.peek();
                if (head == null || head.time() >= serverTicks) break;
                next = pendingServerTimed.poll();
            }
            try {
                next.callback().run();
            } catch (Throwable t) {
                OpenComputers.log.warn("Error in scheduled tick action.", t);
            }
        }

        final List<Robot> robots;
        synchronized (runningRobots) {
            robots = new ArrayList<>(runningRobots);
        }
        final List<Robot> invalid = new ArrayList<>();
        for (Robot robot : robots) {
            if (robot.isRemoved()) invalid.add(robot);
            else if (robot.getLevel() != null) robot.machine().update();
        }
        invalid.forEach(runningRobots::remove);
    }

    private static void onServerTickEnd() {
        // Clean up machines *after* a tick, to allow stuff to be saved, first.
        final List<Machine> scheduled;
        synchronized (machines) {
            scheduled = new ArrayList<>(machines);
        }
        final List<Machine> closed = new ArrayList<>();
        for (Machine machine : scheduled) {
            if (machine.tryClose()) {
                closed.add(machine);
                final Level world = machine.host().world();
                if (world == null || !ExtendedWorld.blockExists(world, BlockPosition.apply(machine.host()))) {
                    if (machine.node() != null) machine.node().remove();
                }
            }
        }
        closed.forEach(machines::remove);
    }

    private static void playerLoggedIn(ServerPlayer player) {
        if (!SideTracker.isServer() || PlatformHooks.isFakePlayer(player)) return;
        if (!LuaStateFactory.isAvailableStatic() && !LuaStateFactory.luajRequested()) {
            player.sendSystemMessage(Localization.Chat.WarningLuaFallback());
        }
        // Gaaah, MC 1.8 y u do this to me? Sending the packets here directly can lead to them
        // arriving on the client before it has a world and player instance, which causes all
        // sorts of trouble. It worked perfectly fine in MC 1.7.10... oSWDEG'PIl;dg'poinEG\a'pi=
        scheduleServer(() -> {
            PacketSender.sendPetVisibility(Optional.empty(), Optional.of(player));
            PacketSender.sendLootDisks(player);
        });
        // Do update check in local games and for OPs.
        final MinecraftServer server = GameInstance.getServer();
        if (server != null && server.getPlayerList().isOp(player.getGameProfile())) {
            UpdateCheck.info.thenAccept(info -> info.ifPresent(release ->
                    server.execute(() -> player.sendSystemMessage(Localization.Chat.InfoNewVersion(release.tag_name)))));
        }
    }

    private static EventResult onBlockBreak(Level level, BlockPos pos, BlockState state, ServerPlayer player) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof li.cil.oc.common.tileentity.Case c) {
            if (c.isCreative() && (!player.isCreative() || !c.canInteract(player.getName().getString()))) {
                return EventResult.interruptFalse();
            }
        } else if (blockEntity instanceof li.cil.oc.common.tileentity.RobotProxy r) {
            final Robot robot = r.robot;
            if (robot.isCreative() && (!player.isCreative() || !robot.canInteract(player.getName().getString()))) {
                return EventResult.interruptFalse();
            }
        }
        return EventResult.pass();
    }

    private static void releasePressedKeys(Player player) {
        final List<Keyboard> copy;
        synchronized (keyboards) {
            copy = new ArrayList<>(keyboards);
        }
        for (Keyboard keyboard : copy) {
            keyboard.releasePressedKeys(player);
        }
    }

    private static EventResult onEntityJoinWorld(Entity entity, Level world) {
        if (Settings.get().giveManualToNewPlayers && !world.isClientSide() &&
                entity instanceof Player player && !PlatformHooks.isFakePlayer(player)) {
            final CompoundTag persistedData = PlayerUtils.persistedData(player);
            if (!persistedData.getBoolean(Settings.namespace + "receivedManual")) {
                persistedData.putBoolean(Settings.namespace + "receivedManual", true);
                player.getInventory().add(li.cil.oc.api.Items.get(Constants.ItemName.Manual).createItemStack(1));
            }
        }
        return EventResult.pass();
    }

    private static ItemInfo drone() {
        return li.cil.oc.api.Items.get(Constants.ItemName.Drone);
    }

    private static ItemInfo eeprom() {
        return li.cil.oc.api.Items.get(Constants.ItemName.EEPROM);
    }

    private static ItemInfo mcu() {
        return li.cil.oc.api.Items.get(Constants.BlockName.Microcontroller);
    }

    private static ItemInfo navigationUpgrade() {
        return li.cil.oc.api.Items.get(Constants.ItemName.NavigationUpgrade);
    }

    private static ItemInfo robot() {
        return li.cil.oc.api.Items.get(Constants.BlockName.Robot);
    }

    private static ItemInfo tablet() {
        return li.cil.oc.api.Items.get(Constants.ItemName.Tablet);
    }

    private static ItemStack findEEPROM(ItemStack[] stacks) {
        final ItemInfo eeprom = eeprom();
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty() && li.cil.oc.api.Items.get(stack) == eeprom) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void onCrafting(Player player, ItemStack crafting, Container inventory) {
        boolean didRecraft = false;

        didRecraft = recraft(player, crafting, inventory, navigationUpgrade(), stack -> {
            // Restore the map currently used in the upgrade.
            final DriverItem driver = li.cil.oc.api.Driver.driverFor(crafting);
            if (driver != null) return ItemStack.of(driver.dataTag(stack).getCompound(Settings.namespace + "map"));
            else return ItemStack.EMPTY;
        }) || didRecraft;

        // Restore EEPROM currently used in microcontroller.
        didRecraft = recraft(player, crafting, inventory, mcu(), stack -> findEEPROM(new MicrocontrollerData(stack).components)) || didRecraft;

        // Restore EEPROM currently used in drone.
        didRecraft = recraft(player, crafting, inventory, drone(), stack -> findEEPROM(new MicrocontrollerData(stack).components)) || didRecraft;

        // Restore EEPROM currently used in robot.
        didRecraft = recraft(player, crafting, inventory, robot(), stack -> findEEPROM(new RobotData(stack).components)) || didRecraft;

        // Restore EEPROM currently used in tablet.
        didRecraft = recraft(player, crafting, inventory, tablet(), stack -> findEEPROM(new TabletData(stack).items)) || didRecraft;

        // Presents?
        // No presents for automatons. Such discrimination. Much bad conscience.
        if (player instanceof ServerPlayer && !PlatformHooks.isFakePlayer(player) && player.level() != null && !player.level().isClientSide()) {
            // Presents!? If we didn't recraft, it's an OC item, and the time is right...
            if (Settings.get().presentChance > 0 && !didRecraft && li.cil.oc.api.Items.get(crafting) != null &&
                    player.getRandom().nextFloat() < Settings.get().presentChance && timeForPresents()) {
                // Presents!
                final ItemStack present = li.cil.oc.api.Items.get(Constants.ItemName.Present).createItemStack(1);
                player.level().playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 0.2f, 1f);
                InventoryUtils.addToPlayerInventory(present, player);
            }
        }

        Achievement.onCraft(crafting, player);
    }

    private static void onPickup(Player player, ItemStack stack) {
        if (stack != null) {
            Achievement.onAssemble(stack, player);
            Achievement.onCraft(stack, player);
        }
    }

    private static boolean timeForPresents() {
        final Calendar now = Calendar.getInstance();
        final int month = now.get(Calendar.MONTH);
        final int dayOfMonth = now.get(Calendar.DAY_OF_MONTH);
        // On the 12th day of Christmas, my robot brought to me~
        return (month == Calendar.DECEMBER && dayOfMonth > 24) || (month == Calendar.JANUARY && dayOfMonth < 7) ||
                (month == Calendar.FEBRUARY && dayOfMonth == 14) ||
                (month == Calendar.APRIL && dayOfMonth == 22) ||
                (month == Calendar.MAY && dayOfMonth == 1) ||
                (month == Calendar.OCTOBER && dayOfMonth == 3) ||
                (month == Calendar.DECEMBER && dayOfMonth == 14);
    }

    public static boolean isItTime() {
        final Calendar now = Calendar.getInstance();
        final int month = now.get(Calendar.MONTH);
        final int dayOfMonth = now.get(Calendar.DAY_OF_MONTH);
        return month == Calendar.APRIL && dayOfMonth == 1;
    }

    private static boolean recraft(Player player, ItemStack crafting, Container inventory, ItemInfo item, Function<ItemStack, ItemStack> callback) {
        if (item != null && li.cil.oc.api.Items.get(crafting) == item) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                final ItemStack stack = inventory.getItem(slot);
                if (li.cil.oc.api.Items.get(stack) == item) {
                    final ItemStack extra = callback.apply(stack);
                    if (extra != null && !extra.isEmpty()) {
                        InventoryUtils.addToPlayerInventory(extra, player);
                    }
                }
            }
            return true;
        } else return false;
    }

    private static Iterable<ChunkHolder> getChunks(ServerLevel world) {
        try {
            return ((ChunkMapAccessor) world.getChunkSource().chunkMap).oc$getChunks();
        } catch (Throwable e) {
            throw new Error("Could not access server chunk list", e);
        }
    }

    // This is called from the ServerThread *and* the ClientShutdownThread, which
    // can potentially happen at the same time... for whatever reason. So let's
    // synchronize what we're doing here to avoid race conditions (e.g. when
    // disposing networks, where this actually triggered an assert).
    private static synchronized void onWorldUnload(ServerLevel world) {
        for (ChunkHolder holder : getChunks(world)) {
            final LevelChunk chunk = holder.getTickingChunk();
            if (chunk != null) {
                for (BlockEntity te : new ArrayList<>(chunk.getBlockEntities().values())) {
                    if (te instanceof li.cil.oc.common.tileentity.traits.TileEntity tile) tile.dispose();
                }
            }
        }

        for (Entity entity : world.getAllEntities()) {
            if (entity instanceof MachineHost host && host.machine() != null) host.machine().stop();
        }

        Callbacks.clear();
    }
}
