package li.cil.oc.common;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.server.PacketSender;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class Sound {
    // ----------------------------------------------------------------------- //
    // Sound event registration (sounds.json entries).

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(OpenComputers.ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> ComputerRunning = register("computer_running");
    public static final RegistrySupplier<SoundEvent> FloppyAccess = register("floppy_access");
    public static final RegistrySupplier<SoundEvent> FloppyEject = register("floppy_eject");
    public static final RegistrySupplier<SoundEvent> FloppyInsert = register("floppy_insert");
    public static final RegistrySupplier<SoundEvent> HddAccess = register("hdd_access");

    private static RegistrySupplier<SoundEvent> register(String name) {
        final ResourceLocation id = new ResourceLocation(OpenComputers.ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {
        SOUNDS.register();
    }

    // ----------------------------------------------------------------------- //

    public static final Map<EnvironmentHost, Map<String, Long>> globalTimeouts = new WeakHashMap<>();

    public static void play(EnvironmentHost host, String name) {
        synchronized (Sound.class) {
            final Map<String, Long> hostTimeouts = globalTimeouts.get(host);
            if (hostTimeouts != null && hostTimeouts.getOrDefault(name, 0L) > System.currentTimeMillis()) {
                return; // Cooldown.
            }
            PacketSender.sendSound(host.world(), host.xPosition(), host.yPosition(), host.zPosition(),
                    new ResourceLocation(Settings.resourceDomain + ":" + name), SoundSource.BLOCKS, 15 * Settings.get().soundVolume);
            globalTimeouts.computeIfAbsent(host, k -> new HashMap<>()).put(name, System.currentTimeMillis() + 500);
        }
    }

    public static void playDiskInsert(EnvironmentHost host) {
        play(host, "floppy_insert");
    }

    public static void playDiskEject(EnvironmentHost host) {
        play(host, "floppy_eject");
    }

    private Sound() {
    }
}
