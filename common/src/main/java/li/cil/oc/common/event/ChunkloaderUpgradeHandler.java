package li.cil.oc.common.event;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.network.Node;
import li.cil.oc.server.component.UpgradeChunkloader;
import li.cil.oc.util.BlockPosition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Chunk loading for the chunkloader upgrade.
 * <p>
 * Forge's ticket based ForgeChunkManager is replaced by vanilla chunk tickets of OC's own
 * {@link #TICKET_TYPE}: each loader (identified by its node address) owns one region ticket that
 * keeps the 3x3 chunks around its center chunk entity-ticking (like the nine forced chunks of the
 * original). Tickets are keyed by the owner, so they never interfere with each other or with
 * chunks forced by other means ({@code /forceload}, other mods): releasing a loader only removes
 * its own ticket. Vanilla does not persist tickets, so the owners and their centers are persisted
 * per level in a {@link SavedData} and re-added on level load as "restored tickets" which loaders
 * reclaim via {@link #claimTicket}; tickets not reclaimed by the next level save are considered
 * orphaned and released. Since a level without players stops ticking (block) entities after a
 * while unless it has forced chunks, levels with active tickets are kept awake each tick.
 */
public final class ChunkloaderUpgradeHandler {
    private static final String DataName = OpenComputers.ID + "_chunkloaders";

    /** Ticket type of the chunkloader upgrade; the ticket's value is the owner (node address). */
    public static final TicketType<UUID> TICKET_TYPE = TicketType.create(OpenComputers.ID + ":chunkloader", UUID::compareTo);

    /** Ticket level 33 - 3 = 30: the center chunk and its 8 neighbours are entity ticking. */
    private static final int TICKET_DISTANCE = 3;

    private static final Map<ResourceKey<Level>, Map<UUID, ChunkPos>> restoredTickets = new HashMap<>();

    private ChunkloaderUpgradeHandler() {
    }

    public static void register() {
        LifecycleEvent.SERVER_LEVEL_LOAD.register(ChunkloaderUpgradeHandler::onWorldLoad);
        LifecycleEvent.SERVER_LEVEL_SAVE.register(ChunkloaderUpgradeHandler::onWorldSave);
        LifecycleEvent.SERVER_STOPPED.register(server -> restoredTickets.clear());
        TickEvent.SERVER_LEVEL_PRE.register(ChunkloaderUpgradeHandler::onLevelTick);
        EventBus.INSTANCE.register(RobotMoveEvent.Post.class, ChunkloaderUpgradeHandler::onMove);
    }

    private static Optional<UUID> parseAddress(String addr) {
        try {
            return Optional.of(UUID.fromString(addr));
        }
        catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public static synchronized Optional<ChunkPos> claimTicket(String addr) {
        Optional<UUID> owner = parseAddress(addr);
        if (owner.isEmpty()) return Optional.empty();
        for (Map<UUID, ChunkPos> tickets : restoredTickets.values()) {
            ChunkPos pos = tickets.remove(owner.get());
            if (pos != null) return Optional.of(pos);
        }
        return Optional.empty();
    }

    // Replaces ForgeChunkManager's LoadingValidationCallback.validateTickets.
    private static synchronized void onWorldLoad(ServerLevel world) {
        TicketData data = data(world);
        Map<UUID, ChunkPos> restored = restoredTickets.computeIfAbsent(world.dimension(), k -> new HashMap<>());
        for (Map.Entry<UUID, ChunkPos> entry : data.tickets.entrySet()) {
            ChunkPos pos = entry.getValue();
            OpenComputers.log.info("Restoring chunk loader ticket for upgrade at chunk (" + pos.x + ", " + pos.z + ") with address " + entry.getKey() + ".");
            restored.put(entry.getKey(), pos);
            addTicket(world, entry.getKey(), pos);
        }
    }

    // Vanilla stops ticking entities and block entities in a level without players 300 ticks
    // after the last one left, unless the level has forced chunks; our tickets count as such.
    private static void onLevelTick(ServerLevel world) {
        if (!data(world).tickets.isEmpty()) {
            world.resetEmptyTime();
        }
    }

    private static synchronized void onWorldSave(ServerLevel world) {
        // Any tickets that were not reassigned by the time the world gets saved
        // again can be considered orphaned, so we release them.
        // TODO figure out a better event *after* tile entities were restored
        // but *before* the world is saved, because the tickets are saved first,
        // so if the save is because the game is being quit the tickets aren't
        // actually being cleared. This will *usually* not be a problem, but it
        // has room for improvement.
        Map<UUID, ChunkPos> restored = restoredTickets.remove(world.dimension());
        if (restored == null) return;
        for (Map.Entry<UUID, ChunkPos> entry : restored.entrySet()) {
            ChunkPos pos = entry.getValue();
            try {
                OpenComputers.log.warn("A chunk loader ticket has been orphaned! Address: " + entry.getKey() + ", position: (" + pos.x + ", " + pos.z + "). Removing...");
                releaseTicket(world, entry.getKey().toString(), pos);
            }
            catch (Throwable err) {
                OpenComputers.log.error(err);
            }
        }
    }

    // Note: it might be necessary to use pre move to force load the target chunk
    // in case the robot moves across a chunk border into an otherwise unloaded
    // chunk (I think it would just fail to move otherwise).
    // Update 2014-06-21: did some testing, seems not to be necessary. My guess
    // is that the access to the block in the direction the robot moves causes
    // the chunk it might move into to get loaded.

    private static void onMove(RobotMoveEvent.Post e) {
        Node machineNode = e.agent.machine().node();
        for (Node node : machineNode.reachableNodes()) {
            if (node.host() instanceof UpgradeChunkloader loader) updateLoadedChunk(loader);
        }
    }

    public static synchronized void releaseTicket(ServerLevel world, String addr, ChunkPos pos) {
        Optional<UUID> uuid = parseAddress(addr);
        if (uuid.isPresent()) {
            // Only the persisted center is authoritative (the passed position may be a
            // placeholder, see UpgradeChunkloader), so we never unforce chunks we didn't force.
            TicketData data = data(world);
            ChunkPos current = data.tickets.remove(uuid.get());
            if (current != null) {
                data.setDirty();
                removeTicket(world, uuid.get(), current);
            }
        }
        else OpenComputers.log.warn("Address '" + addr + "' could not be parsed");
    }

    public static synchronized void updateLoadedChunk(UpgradeChunkloader loader) {
        Optional<UUID> owner = parseAddress(loader.node().address());
        // If loader.ticket is empty that means we shouldn't load anything (as did the old ticketing system).
        if (loader.host.world() instanceof ServerLevel world && owner.isPresent() && loader.ticket.isPresent()) {
            BlockPosition blockPos = BlockPosition.apply(loader.host);
            ChunkPos centerChunk = new ChunkPos(blockPos.x >> 4, blockPos.z >> 4);
            TicketData data = data(world);
            if (!centerChunk.equals(loader.ticket.get()) || !centerChunk.equals(data.tickets.get(owner.get()))) {
                ChunkPos stored = data.tickets.put(owner.get(), centerChunk);
                data.setDirty();
                addTicket(world, owner.get(), centerChunk);
                if (stored != null && !stored.equals(centerChunk)) removeTicket(world, owner.get(), stored);
                loader.ticket = Optional.of(centerChunk);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    private static TicketData data(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TicketData::load, TicketData::new, DataName);
    }

    private static void addTicket(ServerLevel world, UUID owner, ChunkPos center) {
        world.getChunkSource().addRegionTicket(TICKET_TYPE, center, TICKET_DISTANCE, owner);
    }

    private static void removeTicket(ServerLevel world, UUID owner, ChunkPos center) {
        world.getChunkSource().removeRegionTicket(TICKET_TYPE, center, TICKET_DISTANCE, owner);
    }

    private static final class TicketData extends SavedData {
        final Map<UUID, ChunkPos> tickets = new HashMap<>();

        static TicketData load(CompoundTag nbt) {
            TicketData data = new TicketData();
            ListTag list = nbt.getList("tickets", Tag.TAG_COMPOUND);
            Set<UUID> seen = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (!entry.hasUUID("owner")) continue;
                UUID owner = entry.getUUID("owner");
                if (seen.add(owner)) {
                    data.tickets.put(owner, new ChunkPos(entry.getInt("x"), entry.getInt("z")));
                }
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag nbt) {
            ListTag list = new ListTag();
            for (Map.Entry<UUID, ChunkPos> ticket : tickets.entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("owner", ticket.getKey());
                entry.putInt("x", ticket.getValue().x);
                entry.putInt("z", ticket.getValue().z);
                list.add(entry);
            }
            nbt.put("tickets", list);
            return nbt;
        }
    }
}
