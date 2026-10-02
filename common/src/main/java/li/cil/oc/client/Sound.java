package li.cil.oc.client;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

public final class Sound {
    private Sound() {
    }

    private static final Map<BlockEntity, PseudoLoopingStream> sources = new HashMap<>();

    private static final PriorityQueue<Command> commandQueue = new PriorityQueue<>(Comparator.comparingLong(c -> c.when));

    private static final Timer updateTimer = new Timer("OpenComputers-SoundUpdater", true);

    private static Runnable updateCallable = null;

    private static boolean initialized = false;

    static {
        if (Settings.get().soundVolume > 0) {
            updateTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    synchronized (sources) {
                        Sound.updateCallable = Sound::processQueue;
                    }
                }
            }, 500, 50);
        }
    }

    /**
     * Registers the client tick / level unload handlers (formerly Forge event subscriptions).
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        ClientTickEvent.CLIENT_POST.register(minecraft -> onTick());
        // TODO(port): Forge's WorldEvent.Unload also fired on dimension changes; Architectury has no
        //  client level unload event, so we only clean up when leaving the world.
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> onWorldUnload());
    }

    private static void processQueue() {
        if (!commandQueue.isEmpty()) {
            synchronized (commandQueue) {
                while (!commandQueue.isEmpty() && commandQueue.peek().when < System.currentTimeMillis()) {
                    try {
                        commandQueue.poll().apply();
                    } catch (Throwable t) {
                        OpenComputers.log.warn("Error processing sound command.", t);
                    }
                }
            }
        }
    }

    public static void startLoop(BlockEntity tileEntity, String name) {
        startLoop(tileEntity, name, 1f, 0);
    }

    public static void startLoop(BlockEntity tileEntity, String name, float volume) {
        startLoop(tileEntity, name, volume, 0);
    }

    public static void startLoop(BlockEntity tileEntity, String name, float volume, long delay) {
        if (Settings.get().soundVolume > 0) {
            synchronized (commandQueue) {
                commandQueue.add(new StartCommand(System.currentTimeMillis() + delay, tileEntity, name, volume));
            }
        }
    }

    public static void stopLoop(BlockEntity tileEntity) {
        if (Settings.get().soundVolume > 0) {
            synchronized (commandQueue) {
                commandQueue.add(new StopCommand(tileEntity));
            }
        }
    }

    public static void updatePosition(BlockEntity tileEntity) {
        if (Settings.get().soundVolume > 0) {
            synchronized (commandQueue) {
                commandQueue.add(new UpdatePositionCommand(tileEntity));
            }
        }
    }

    public static void onTick() {
        synchronized (sources) {
            if (updateCallable != null) updateCallable.run();
            updateCallable = null;
        }
    }

    public static void onWorldUnload() {
        synchronized (commandQueue) {
            commandQueue.clear();
        }
        synchronized (sources) {
            try {
                for (PseudoLoopingStream sound : sources.values()) {
                    sound.halt();
                }
            } catch (Throwable ignored) {
                // Ignore.
            }
            sources.clear();
        }
    }

    private abstract static class Command {
        final long when;
        final BlockEntity tileEntity;

        Command(long when, BlockEntity tileEntity) {
            this.when = when;
            this.tileEntity = tileEntity;
        }

        abstract void apply();
    }

    private static final class StartCommand extends Command {
        final String name;
        final float volume;

        StartCommand(long when, BlockEntity tileEntity, String name, float volume) {
            super(when, tileEntity);
            this.name = name;
            this.volume = volume;
        }

        @Override
        void apply() {
            synchronized (sources) {
                final PseudoLoopingStream current = sources.get(tileEntity);
                if (current == null || !current.getLocation().getPath().equals(name)) {
                    if (current != null) current.halt();
                    // TODO(port): like on 1.16.5 the stream is only tracked, never handed to the
                    //  SoundManager (Minecraft.getInstance().getSoundManager().play(...)).
                    sources.put(tileEntity, new PseudoLoopingStream(tileEntity, volume, name));
                }
            }
        }
    }

    private static final class StopCommand extends Command {
        StopCommand(BlockEntity tileEntity) {
            super(System.currentTimeMillis() + 1, tileEntity);
        }

        @Override
        void apply() {
            synchronized (sources) {
                final PseudoLoopingStream sound = sources.remove(tileEntity);
                if (sound != null) sound.halt();
            }
            synchronized (commandQueue) {
                // Remove all other commands for this tile entity from the queue. This
                // is inefficient, but we generally don't expect the command queue to
                // be very long, so this should be OK.
                final List<Command> remaining = new ArrayList<>(commandQueue);
                commandQueue.clear();
                for (Command command : remaining) {
                    if (command.tileEntity != tileEntity) commandQueue.add(command);
                }
            }
        }
    }

    private static final class UpdatePositionCommand extends Command {
        UpdatePositionCommand(BlockEntity tileEntity) {
            super(System.currentTimeMillis(), tileEntity);
        }

        @Override
        void apply() {
            synchronized (sources) {
                final PseudoLoopingStream sound = sources.get(tileEntity);
                if (sound != null) sound.updatePosition();
            }
        }
    }

    private static final class PseudoLoopingStream extends AbstractTickableSoundInstance {
        final BlockEntity tileEntity;
        final float subVolume;

        PseudoLoopingStream(BlockEntity tileEntity, float subVolume, String name) {
            super(SoundEvent.createVariableRangeEvent(new ResourceLocation(OpenComputers.ID, name)), SoundSource.BLOCKS, RandomSource.create());
            this.tileEntity = tileEntity;
            this.subVolume = subVolume;
            this.volume = subVolume * Settings.get().soundVolume;
            this.relative = tileEntity != null;
            this.looping = true;
            updatePosition();
        }

        void updatePosition() {
            if (tileEntity != null) {
                final BlockPos pos = tileEntity.getBlockPos();
                x = pos.getX() + 0.5;
                y = pos.getY() + 0.5;
                z = pos.getZ() + 0.5;
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        // Required by TickableSoundInstance, which is required to update position while playing.
        @Override
        public void tick() {
        }

        void halt() {
            stop();
        }
    }
}
