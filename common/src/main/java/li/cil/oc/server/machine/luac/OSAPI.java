package li.cil.oc.server.machine.luac;

import li.cil.oc.util.ExtendedLuaState;
import li.cil.oc.util.GameTimeFormatter;
import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaType;

import java.util.Optional;

public class OSAPI extends NativeLuaAPI {
    public OSAPI(NativeLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        // Push a couple of functions that override original Lua API functions or
        // that add new functionality to it.
        lua().getGlobal("os");

        // Custom os.clock() implementation returning the time the computer has
        // been actively running, instead of the native library...
        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            lua.pushNumber(machine.cpuTime());
            return 1;
        });
        lua().setField(-2, "clock");

        // Date formatting function.
        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            final String format =
                    lua.getTop() > 0 && lua.isString(1) ? lua.toString(1)
                            : "%d/%m/%y %H:%M:%S";
            final double time =
                    lua.getTop() > 1 && lua.isNumber(2) ? lua.toNumber(2)
                            : (double) ((machine.worldTime() + 6000) * 60 * 60 / 1000);

            final GameTimeFormatter.DateTime dt = GameTimeFormatter.parse(time);

            // Just ignore the allowed leading '!', Minecraft has no time zones...
            final String f = format.startsWith("!") ? format.substring(1) : format;
            if (f.equals("*t")) {
                lua.newTable(0, 8);
                lua.pushInteger(dt.year);
                lua.setField(-2, "year");
                lua.pushInteger(dt.month);
                lua.setField(-2, "month");
                lua.pushInteger(dt.day);
                lua.setField(-2, "day");
                lua.pushInteger(dt.hour);
                lua.setField(-2, "hour");
                lua.pushInteger(dt.minute);
                lua.setField(-2, "min");
                lua.pushInteger(dt.second);
                lua.setField(-2, "sec");
                lua.pushInteger(dt.weekDay);
                lua.setField(-2, "wday");
                lua.pushInteger(dt.yearDay);
                lua.setField(-2, "yday");
            } else {
                lua.pushString(GameTimeFormatter.format(f, dt));
            }
            return 1;
        });
        lua().setField(-2, "date");

        // Return ingame time for os.time().
        ExtendedLuaState.pushScalaFunction(lua(), lua -> {
            if (lua.isNoneOrNil(1)) {
                // Game time is in ticks, so that each day has 24000 ticks, meaning
                // one hour is game time divided by one thousand. Also, Minecraft
                // starts days at 6 o'clock, versus the 1 o'clock of timestamps so we
                // add those five hours. Thus:
                // timestamp = (time + 5000) * 60[kh] * 60[km] / 1000[s]
                lua.pushNumber((double) ((machine.worldTime() + 5000) * 60 * 60 / 1000));
            } else {
                lua.checkType(1, LuaType.TABLE);
                lua.setTop(1);

                final int sec = getField(lua, "sec", 0);
                final int min = getField(lua, "min", 0);
                final int hour = getField(lua, "hour", 12);
                final int mday = getField(lua, "day", -1);
                final int mon = getField(lua, "month", -1);
                final int year = getField(lua, "year", -1);

                final Optional<Integer> time = GameTimeFormatter.mktime(year, mon, mday, hour, min, sec);
                if (time.isPresent()) lua.pushNumber(time.get());
                else lua.pushNil();
            }
            return 1;
        });
        lua().setField(-2, "time");

        // Pop the os table.
        lua().pop(1);
    }

    private static int getField(LuaState lua, String key, int d) {
        lua.getField(-1, key);
        final Long res = lua.toIntegerX(-1);
        lua.pop(1);
        if (res == null) {
            if (d < 0) throw NativeLuaAPI.<RuntimeException>sneakyThrow(new Exception("field '" + key + "' missing in date table"));
            return d;
        }
        return res.intValue();
    }
}
