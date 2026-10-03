package li.cil.oc.integration.tis3d;

import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.DeferredRegister;
import li.cil.oc.OpenComputers;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;
import li.cil.tis3d.api.serial.SerialInterfaceProvider;

/**
 * Integration with TIS-3D (mod id {@code tis3d}): adapters can be accessed by TIS-3D's
 * serial port module.
 */
public final class ModTIS3D implements ModProxy {
    public static final ModTIS3D INSTANCE = new ModTIS3D();

    private DeferredRegister<SerialInterfaceProvider> providers;

    private ModTIS3D() {
    }

    @Override
    public Mod getMod() {
        return Mods.TIS3D;
    }

    @Override
    public void preInitialize() {
        providers = DeferredRegister.create(OpenComputers.ID, SerialInterfaceProvider.REGISTRY);
        providers.register("serial_port", () -> SerialInterfaceProviderAdapter.INSTANCE);
        // On Forge TIS-3D's registry is filled during RegisterEvent, after mod construction, so the
        // deferred register has to be hooked up right now (OC's mods.toml orders OC after TIS-3D, so
        // TIS-3D's registrar builder already exists). On Fabric the registry only exists once
        // TIS-3D's own initializer ran, which may be after ours, so wait for common setup (which
        // runs after all mod initializers there).
        if (!Platform.isFabric()) {
            providers.register();
        }
    }

    @Override
    public void initialize() {
        if (Platform.isFabric()) {
            providers.register();
        }
    }
}
