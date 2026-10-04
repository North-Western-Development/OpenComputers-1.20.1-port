package li.cil.oc.common.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

/**
 * Loader specific parts of the opt-in debug commands
 * ({@link li.cil.oc.server.command.DebugCommands}).
 */
public final class DebugPlatform {
    private DebugPlatform() {
    }

    /**
     * Registers protection hooks that deny interacting with the entities and blocks
     * {@link li.cil.oc.server.command.DebugCommands#isProtected} reports as protected,
     * like a protection mod would (Forge {@code PlayerInteractEvent}, Fabric
     * {@code UseEntityCallback} / {@code UseBlockCallback}). Used to test OC's permission checks.
     */
    @ExpectPlatform
    public static void registerProtectionHooks() {
        throw new AssertionError();
    }
}
