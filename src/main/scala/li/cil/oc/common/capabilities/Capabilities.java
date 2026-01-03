package li.cil.oc.common.capabilities;

import li.cil.oc.api.internal.Colored;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
//import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.concurrent.Callable;

// Gotta be Java, @CapabilityInject don't werk for Scala ;_;
public final class Capabilities {
    //    @CapabilityInject(Colored.class)
    public static Capability<Colored> ColoredCapability = CapabilityManager.get(new CapabilityToken<>() {
    });

    //    @CapabilityInject(Environment.class)
    public static Capability<Environment> EnvironmentCapability = CapabilityManager.get(new CapabilityToken<>() {
    });

    //    @CapabilityInject(SidedEnvironment.class)
    public static Capability<SidedEnvironment> SidedEnvironmentCapability = CapabilityManager.get(new CapabilityToken<>() {
    });

    // java 7 doesn't have generic type constraints
    // java 7 doesn't have lambdas
    // java 7 doesn't generic type covariance
//    private static class StupidJavaTookTooManyYearsToIntroduceLambdas<T> implements Callable<T> {
//
//        StupidJavaTookTooManyYearsToIntroduceLambdas(Class cls) {
//            _cls = cls;
//        }
//
//        @Override
//        public T call() throws Exception {
//            return (T)_cls.newInstance();
//        }
//
//        private Class _cls;
//    }


    public static void init(RegisterCapabilitiesEvent event) {
//        event.register(CapabilityColored.class);
//        event.register(CapabilityEnvironment.class);
//        event.register(CapabilitySidedEnvironment.class);
//        CapabilityManager.INSTANCE.register(Environment.class, new CapabilityEnvironment.DefaultStorage(), new StupidJavaTookTooManyYearsToIntroduceLambdas<Environment>(CapabilityEnvironment.DefaultImpl.class));
//        CapabilityManager.INSTANCE.register(SidedEnvironment.class, new CapabilitySidedEnvironment.DefaultStorage(), new StupidJavaTookTooManyYearsToIntroduceLambdas<SidedEnvironment>(CapabilitySidedEnvironment.DefaultImpl.class));
//        CapabilityManager.INSTANCE.register(Colored.class, new CapabilityColored.DefaultStorage(), new StupidJavaTookTooManyYearsToIntroduceLambdas<Colored>(CapabilityColored.DefaultImpl.class));
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        if ((event.getObject() instanceof Colored))
            event.addCapability(
                    CapabilityColored.ProviderColored(),
                    new CapabilityColored.Provider(event.getObject())
            );

        if ((event.getObject() instanceof Environment))
            event.addCapability(
                    CapabilityEnvironment.ProviderEnvironment(),
                    new CapabilityEnvironment.Provider(event.getObject())
            );

        if ((event.getObject() instanceof SidedEnvironment))
            event.addCapability(
                    CapabilitySidedEnvironment.ProviderSidedEnvironment(),
                    new CapabilitySidedEnvironment.Provider(event.getObject())
            );
    }

    private Capabilities() {
    }
}
