package li.cil.oc.server.machine.luaj;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.network.Connector;
import li.cil.oc.util.ScalaClosure;
import li.cil.repack.org.luaj.vm2.LuaTable;
import li.cil.repack.org.luaj.vm2.LuaValue;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ComputerAPI extends LuaJAPI {
    public ComputerAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        // Computer API, stuff that kinda belongs to os, but we don't want to
        // clutter it.
        final LuaTable computer = LuaValue.tableOf();

        // Allow getting the real world time for timeouts.
        computer.set("realTime", new ScalaClosure(args -> LuaValue.valueOf(System.currentTimeMillis() / 1000.0)));

        computer.set("uptime", new ScalaClosure(args -> LuaValue.valueOf(machine.upTime())));

        // Allow the computer to figure out its own id in the component network.
        computer.set("address", new ScalaClosure(args -> {
            final String address = node().address();
            return address != null ? LuaValue.valueOf(address) : LuaValue.NIL;
        }));

        computer.set("freeMemory", new ScalaClosure(args -> LuaValue.valueOf(owner.memory / 2)));

        computer.set("totalMemory", new ScalaClosure(args -> LuaValue.valueOf(owner.memory)));

        computer.set("pushSignal", new ScalaClosure(args -> LuaValue.valueOf(machine.signal(args.checkjstring(1), ScalaClosure.toSimpleJavaObjects(args, 2).toArray()))));

        // And it's /tmp address...
        computer.set("tmpAddress", new ScalaClosure(args -> {
            final String address = machine.tmpAddress();
            if (address == null) return LuaValue.NIL;
            return LuaValue.valueOf(address);
        }));

        // User management.
        computer.set("users", new ScalaClosure(args -> {
            final String[] users = machine.users();
            final LuaValue[] values = new LuaValue[users.length];
            for (int i = 0; i < users.length; i++) values[i] = LuaValue.valueOf(users[i]);
            return LuaValue.varargsOf(values);
        }));

        computer.set("addUser", new ScalaClosure(args -> {
            try {
                machine.addUser(args.checkjstring(1));
            } catch (Exception e) {
                throw LuaJAPI.<RuntimeException>sneakyThrow(e);
            }
            return LuaValue.TRUE;
        }));

        computer.set("removeUser", new ScalaClosure(args -> LuaValue.valueOf(machine.removeUser(args.checkjstring(1)))));

        computer.set("energy", new ScalaClosure(args -> {
            if (Settings.get().ignorePower)
                return LuaValue.valueOf(Double.POSITIVE_INFINITY);
            else
                return LuaValue.valueOf(((Connector) node()).globalBuffer());
        }));

        computer.set("maxEnergy", new ScalaClosure(args -> LuaValue.valueOf(((Connector) node()).globalBufferSize())));

        computer.set("getArchitectures", new ScalaClosure(args -> {
            for (ItemStack stack : machine.host().internalComponents()) {
                final DriverItem driver = Driver.driverFor(stack);
                final Collection<Class<? extends Architecture>> architectures;
                if (driver instanceof MutableProcessor processor) architectures = processor.allArchitectures();
                else if (driver instanceof Processor processor) architectures = List.of(processor.architecture(stack));
                else continue;
                final List<LuaValue> names = new ArrayList<>();
                for (Class<? extends Architecture> arch : architectures) {
                    names.add(LuaValue.valueOf(li.cil.oc.api.Machine.getArchitectureName(arch)));
                }
                return LuaValue.listOf(names.toArray(new LuaValue[0]));
            }
            return LuaValue.tableOf();
        }));

        computer.set("getArchitecture", new ScalaClosure(args -> {
            for (ItemStack stack : machine.host().internalComponents()) {
                if (Driver.driverFor(stack) instanceof Processor processor) {
                    return LuaValue.valueOf(li.cil.oc.api.Machine.getArchitectureName(processor.architecture(stack)));
                }
            }
            return LuaValue.NONE;
        }));

        computer.set("setArchitecture", new ScalaClosure(args -> {
            final String archName = args.checkjstring(1);
            for (ItemStack stack : machine.host().internalComponents()) {
                if (Driver.driverFor(stack) instanceof MutableProcessor processor) {
                    for (Class<? extends Architecture> archClass : processor.allArchitectures()) {
                        if (li.cil.oc.api.Machine.getArchitectureName(archClass).equals(archName)) {
                            if (archClass != processor.architecture(stack)) {
                                processor.setArchitecture(stack, archClass);
                                return LuaValue.TRUE;
                            } else {
                                return LuaValue.FALSE;
                            }
                        }
                    }
                    return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("unknown architecture"));
                }
            }
            return LuaValue.NONE;
        }));

        // Set the computer table.
        lua().set("computer", computer);
    }
}
