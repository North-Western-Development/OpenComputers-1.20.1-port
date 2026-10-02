package li.cil.oc.common;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.detail.IMCAPI;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.template.AssemblerTemplates;
import li.cil.oc.common.template.DisassemblerTemplates;
import li.cil.oc.integration.util.ItemCharge;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.server.machine.ProgramLocations;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Handles the messages sent via {@link li.cil.oc.api.IMC}. Formerly fed by
 * Forge's InterModComms; now {@link li.cil.oc.api.API#imc} is set to
 * {@link #INSTANCE} during initialization and messages are delivered directly.
 */
public final class IMC implements IMCAPI {
    public static final IMC INSTANCE = new IMC();

    // The sender is no longer known now that messages are delivered directly.
    private static final String Sender = "unknown";

    private IMC() {
    }

    @Override
    public void handle(String method, Object payload) {
        handleMessage(method, payload);
    }

    public static void handleMessage(String method, Object payload) {
        if (payload instanceof CompoundTag template && li.cil.oc.api.IMC.REGISTER_ASSEMBLER_TEMPLATE.equals(method)) {
            if (template.contains("name", Tag.TAG_STRING))
                OpenComputers.log.debug("Registering new assembler template '" + template.getString("name") + "' from mod " + Sender + ".");
            else
                OpenComputers.log.debug("Registering new, unnamed assembler template from mod " + Sender + ".");
            try {
                AssemblerTemplates.add(template);
            } catch (Throwable t) {
                OpenComputers.log.warn("Failed registering assembler template.", t);
            }
        } else if (payload instanceof CompoundTag template && li.cil.oc.api.IMC.REGISTER_DISASSEMBLER_TEMPLATE.equals(method)) {
            if (template.contains("name", Tag.TAG_STRING))
                OpenComputers.log.debug("Registering new disassembler template '" + template.getString("name") + "' from mod " + Sender + ".");
            else
                OpenComputers.log.debug("Registering new, unnamed disassembler template from mod " + Sender + ".");
            try {
                DisassemblerTemplates.add(template);
            } catch (Throwable t) {
                OpenComputers.log.warn("Failed registering disassembler template.", t);
            }
        } else if (payload instanceof String name && li.cil.oc.api.IMC.REGISTER_TOOL_DURABILITY_PROVIDER.equals(method)) {
            OpenComputers.log.debug("Registering new tool durability provider '" + name + "' from mod " + Sender + ".");
            try {
                ToolDurabilityProviders.add(getStaticMethod(name, ItemStack.class));
            } catch (Throwable t) {
                OpenComputers.log.warn("Failed registering tool durability provider.", t);
            }
        } else if (payload instanceof String name && li.cil.oc.api.IMC.REGISTER_WRENCH_TOOL.equals(method)) {
            OpenComputers.log.debug("Registering new wrench usage '" + name + "' from mod " + Sender + ".");
            try {
                Wrench.addUsage(getStaticMethod(name, Player.class, BlockPos.class, boolean.class));
            } catch (Throwable t) {
                OpenComputers.log.warn("Failed registering wrench usage.", t);
            }
        } else if (payload instanceof String name && li.cil.oc.api.IMC.REGISTER_WRENCH_TOOL_CHECK.equals(method)) {
            OpenComputers.log.debug("Registering new wrench tool check '" + name + "' from mod " + Sender + ".");
            try {
                Wrench.addCheck(getStaticMethod(name, ItemStack.class));
            } catch (Throwable t) {
                OpenComputers.log.warn("Failed registering wrench check.", t);
            }
        } else if (payload instanceof CompoundTag implInfo && li.cil.oc.api.IMC.REGISTER_ITEM_CHARGE.equals(method)) {
            OpenComputers.log.debug("Registering new item charge implementation '" + implInfo.getString("name") + "' from mod " + Sender + ".");
            try {
                ItemCharge.add(
                        getStaticMethod(implInfo.getString("canCharge"), ItemStack.class),
                        getStaticMethod(implInfo.getString("charge"), ItemStack.class, double.class, boolean.class));
            } catch (Throwable t) {
                OpenComputers.log.warn("Failed registering item charge implementation.", t);
            }
        } else if (payload instanceof String name && li.cil.oc.api.IMC.BLACKLIST_PERIPHERAL.equals(method)) {
            OpenComputers.log.debug("Blacklisting CC peripheral '" + name + "' as requested by mod " + Sender + ".");
            if (!Settings.get().peripheralBlacklist.contains(name)) {
                Settings.get().peripheralBlacklist.add(name);
            }
        } else if (payload instanceof CompoundTag compInfo && li.cil.oc.api.IMC.BLACKLIST_HOST.equals(method)) {
            OpenComputers.log.debug("Blacklisting component '" + compInfo.getString("name") + "' for host '" + compInfo.getString("host") + "' as requested by mod " + Sender + ".");
            try {
                Registry.INSTANCE.blacklistHost(ItemStack.of(compInfo.getCompound("item")), Class.forName(compInfo.getString("host")));
            } catch (Throwable t) {
                OpenComputers.log.warn("Failed blacklisting component.", t);
            }
        } else if (payload instanceof String name && li.cil.oc.api.IMC.REGISTER_ASSEMBLER_FILTER.equals(method)) {
            OpenComputers.log.debug("Registering new assembler template filter '" + name + "' from mod " + Sender + ".");
            try {
                AssemblerTemplates.addFilter(name);
            } catch (Throwable t) {
                OpenComputers.log.warn("Failed registering assembler template filter.", t);
            }
        } else if (payload instanceof String name && li.cil.oc.api.IMC.REGISTER_INK_PROVIDER.equals(method)) {
            OpenComputers.log.debug("Registering new ink provider '" + name + "' from mod " + Sender + ".");
            try {
                PrintData.addInkProvider(getStaticMethod(name, ItemStack.class));
            } catch (Throwable t) {
                OpenComputers.log.warn("Failed registering ink provider.", t);
            }
        } else if (payload instanceof CompoundTag diskInfo && li.cil.oc.api.IMC.REGISTER_PROGRAM_DISK_LABEL.equals(method)) {
            OpenComputers.log.debug("Registering new program location mapping for program '" + diskInfo.getString("program") + "' being on disk '" + diskInfo.getString("label") + "' from mod " + Sender + ".");
            final ListTag list = diskInfo.getList("architectures", Tag.TAG_STRING);
            final String[] architectures = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                architectures[i] = list.getString(i);
            }
            ProgramLocations.addMapping(diskInfo.getString("program"), diskInfo.getString("label"), architectures);
        } else {
            // TODO(port): REGISTER_CUSTOM_POWER_SYSTEM was not handled on 1.16.5 either.
            OpenComputers.log.warn("Got an unrecognized or invalid IMC message '" + method + "' from mod " + Sender + ".");
        }
    }

    public static Method getStaticMethod(String name, Class<?>... signature) throws ClassNotFoundException, NoSuchMethodException {
        final int nameSplit = name.lastIndexOf('.');
        final String className = name.substring(0, nameSplit);
        final String methodName = name.substring(nameSplit + 1);
        final Class<?> clazz = Class.forName(className);
        final Method method = clazz.getDeclaredMethod(methodName, signature);
        if (!Modifier.isStatic(method.getModifiers()))
            throw new IllegalArgumentException("Method " + name + " is not static.");
        return method;
    }

    /**
     * Scala signature was {@code tryInvokeStatic[T](method, args*)(default)};
     * the default moved before the varargs.
     */
    @SuppressWarnings("unchecked")
    public static <T> T tryInvokeStatic(Method method, T defaultValue, Object... args) {
        try {
            return (T) method.invoke(null, args);
        } catch (Throwable t) {
            OpenComputers.log.warn("Error invoking callback " + method.getDeclaringClass().getCanonicalName() + "." + method.getName() + ".", t);
            return defaultValue;
        }
    }

    public static void tryInvokeStaticVoid(Method method, Object... args) {
        try {
            method.invoke(null, args);
        } catch (Throwable t) {
            OpenComputers.log.warn("Error invoking callback " + method.getDeclaringClass().getCanonicalName() + "." + method.getName() + ".", t);
        }
    }
}
