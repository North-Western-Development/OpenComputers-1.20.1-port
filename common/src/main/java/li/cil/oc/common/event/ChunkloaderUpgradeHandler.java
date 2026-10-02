package li.cil.oc.common.event;

import dev.architectury.event.events.common.LifecycleEvent;
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
 * Forge's ticket based ForgeChunkManager is replaced by vanilla {@link ServerLevel#setChunkForced}
 * plus OC-side bookkeeping: each loader (identified by its node address) owns a 3x3 area around a
 * center chunk. The owners and their centers are persisted per level in a {@link SavedData}, and a
 * chunk is forced as long as any owner's area covers it. On level load the persisted owners become
 * "restored tickets" which loaders reclaim via {@link #claimTicket}; tickets not reclaimed by the next
 * level save are considered orphaned and released.
 */
public final class ChunkloaderUpgradeHandler {
    private static final String DataName = OpenComputers.ID + "_chunkloaders";

    private static final Map<ResourceKey<Level>, Map<UUID, ChunkPos>> restoredTickets = new HashMap<>();

    private ChunkloaderUpgradeHandler() {
    }

    public static void register() {
        LifecycleEvent.SERVER_LEVEL_LOAD.register(ChunkloaderUpgradeHandler::onWorldLoad);
        LifecycleEvent.SERVER_LEVEL_SAVE.register(ChunkloaderUpgradeHandler::onWorldSave);
        LifecycleEvent.SERVER_STOPPED.register(server -> restoredTickets.clear());
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
            // Make sure the chunks are actually forced (vanilla persists this, but be safe).
            forceArea(world, data, pos);
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
                updateArea(world, data, current);
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
                forceArea(world, data, centerChunk);
                if (stored != null && !stored.equals(centerChunk)) updateArea(world, data, stored);
                loader.ticket = Optional.of(centerChunk);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    private static TicketData data(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TicketData::load, TicketData::new, DataName);
    }

    private static boolean isCovered(TicketData data, int x, int z) {
        for (ChunkPos center : data.tickets.values()) {
            if (Math.abs(center.x - x) <= 1 && Math.abs(center.z - z) <= 1) return true;
        }
        return false;
    }

    private static void forceArea(ServerLevel world, TicketData data, ChunkPos center) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setChunkForced(center.x + x, center.z + z, true);
            }
        }
    }

    // Unforces chunks in the 3x3 area around center that are no longer covered by any loader.
    // TODO(port): this may also unforce chunks forced by other means (e.g. /forceload).
    private static void updateArea(ServerLevel world, TicketData data, ChunkPos center) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                int cx = center.x + x;
                int cz = center.z + z;
                world.setChunkForced(cx, cz, isCovered(data, cx, cz));
            }
        }
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
