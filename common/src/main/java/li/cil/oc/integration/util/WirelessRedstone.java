package li.cil.oc.integration.util;

import li.cil.oc.server.component.RedstoneWireless;

import java.util.LinkedHashSet;
import java.util.Set;

public final class WirelessRedstone {
    private WirelessRedstone() {
    }

    public static final Set<WirelessRedstoneSystem> systems = new LinkedHashSet<>();

    public static boolean isAvailable() {
        return !systems.isEmpty();
    }

    public static void addReceiver(RedstoneWireless rs) {
        for (WirelessRedstoneSystem system : systems) {
            try {
                system.addReceiver(rs);
            } catch (Throwable ignored) {
                // Ignore
            }
        }
    }

    public static void removeReceiver(RedstoneWireless rs) {
        for (WirelessRedstoneSystem system : systems) {
            try {
                system.removeReceiver(rs);
            } catch (Throwable ignored) {
                // Ignore
            }
        }
    }

    public static void updateOutput(RedstoneWireless rs) {
        for (WirelessRedstoneSystem system : systems) {
            try {
                system.updateOutput(rs);
            } catch (Throwable ignored) {
                // Ignore
            }
        }
    }

    public static void removeTransmitter(RedstoneWireless rs) {
        for (WirelessRedstoneSystem system : systems) {
            try {
                system.removeTransmitter(rs);
            } catch (Throwable ignored) {
                // Ignore
            }
        }
    }

    public static boolean getInput(RedstoneWireless rs) {
        for (WirelessRedstoneSystem system : systems) {
            if (system.getInput(rs)) return true;
        }
        return false;
    }

    public interface WirelessRedstoneSystem {
        void addReceiver(RedstoneWireless rs);

        void removeReceiver(RedstoneWireless rs);

        void updateOutput(RedstoneWireless rs);

        void removeTransmitter(RedstoneWireless rs);

        boolean getInput(RedstoneWireless rs);
    }
}
