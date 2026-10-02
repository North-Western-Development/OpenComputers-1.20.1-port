package li.cil.oc.server.machine.luac;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.Connector;
import li.cil.oc.util.ExtendedLuaState;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ComputerAPI extends NativeLuaAPI {
    public ComputerAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        // Computer API, stuff that kinda belongs to os, but we don't want to
        // clutter it.
        lua().newTable();

        // Allow getting the real world time for timeouts.
        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushNumber(System.currentTimeMillis() / 1000.0);
            return 1;
        });
        lua().setField(-2, "realTime");

        // The time the computer has been running, as opposed to the CPU time.
        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushNumber(machine.upTime());
            return 1;
        });
        lua().setField(-2, "uptime");

        // Allow the computer to figure out its own id in the component network.
        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final String address = node().address();
            if (address == null) lua.pushNil();
            else lua.pushString(address);
            return 1;
        });
        lua().setField(-2, "address");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            // This is *very* unlikely, but still: avoid this getting larger than
            // what we report as the total memory.
            lua.pushInteger((int) (Math.min(lua.getFreeMemory(), lua.getTotalMemory() - owner.kernelMemory) / owner.ramScale));
            return 1;
        });
        lua().setField(-2, "freeMemory");

        // Allow the system to read how much memory it uses and has available.
        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushInteger((int) ((lua.getTotalMemory() - owner.kernelMemory) / owner.ramScale));
            return 1;
        });
        lua().setField(-2, "totalMemory");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushBoolean(machine.signal(lua.checkString(1), ExtendedLuaState.toSimpleJavaObjects(lua, 2).toArray()));
            return 1;
        });
        lua().setField(-2, "pushSignal");

        // And it's /tmp address...
        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final String address = machine.tmpAddress();
            if (address == null) lua.pushNil();
            else lua.pushString(address);
            return 1;
        });
        lua().setField(-2, "tmpAddress");

        // User management.
        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final String[] users = machine.users();
            for (String user : users) lua.pushString(user);
            return users.length;
        });
        lua().setField(-2, "users");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final String user = lua.checkString(1);
            try {
                machine.addUser(user);
                lua.pushBoolean(true);
                return 1;
            } catch (Throwable e) {
                lua.pushNil();
                lua.pushString(e.getMessage() != null ? e.getMessage() : e.toString());
                return 2;
            }
        });
        lua().setField(-2, "addUser");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushBoolean(machine.removeUser(lua.checkString(1)));
            return 1;
        });
        lua().setField(-2, "removeUser");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            if (Settings.get().ignorePower)
                lua.pushNumber(Double.POSITIVE_INFINITY);
            else
                lua.pushNumber(((Connector) node()).globalBuffer());
            return 1;
        });
        lua().setField(-2, "energy");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushNumber(((Connector) node()).globalBufferSize());
            return 1;
        });
        lua().setField(-2, "maxEnergy");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            for (ItemStack stack : machine.host().internalComponents()) {
                final DriverItem driver = Driver.driverFor(stack);
                final Collection<Class<? extends Architecture>> architectures;
                if (driver instanceof MutableProcessor processor) architectures = processor.allArchitectures();
                else if (driver instanceof Processor processor) architectures = List.of(processor.architecture(stack));
                else continue;
                final List<String> names = new ArrayList<>();
                for (Class<? extends Architecture> arch : architectures) {
                    names.add(li.cil.oc.api.Machine.getArchitectureName(arch));
                }
                ExtendedLuaState.pushValue(lua, names);
                return 1;
            }
            lua.newTable();
            return 1;
        });
        lua().setField(-2, "getArchitectures");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            for (ItemStack stack : machine.host().internalComponents()) {
                if (Driver.driverFor(stack) instanceof Processor processor) {
                    lua.pushString(li.cil.oc.api.Machine.getArchitectureName(processor.architecture(stack)));
                    return 1;
                }
            }
            return 0;
        });
        lua().setField(-2, "getArchitecture");

        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final String archName = lua.checkString(1);
            for (ItemStack stack : machine.host().internalComponents()) {
                if (Driver.driverFor(stack) instanceof MutableProcessor processor) {
                    for (Class<? extends Architecture> archClass : processor.allArchitectures()) {
                        if (li.cil.oc.api.Machine.getArchitectureName(archClass).equals(archName)) {
                            if (archClass != processor.architecture(stack)) {
                                processor.setArchitecture(stack, archClass);
                                lua.pushBoolean(true);
                            } else {
                                lua.pushBoolean(false);
                            }
                            return 1;
                        }
                    }
                    lua.pushNil();
                    lua.pushString("unknown architecture");
                    return 2;
                }
            }
            return 0;
        });
        lua().setField(-2, "setArchitecture");

        // Set the computer table.
        lua().setGlobal("computer");
    }
}
