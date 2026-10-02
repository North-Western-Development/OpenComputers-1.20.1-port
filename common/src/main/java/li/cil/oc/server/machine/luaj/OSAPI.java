package li.cil.oc.server.machine.luaj;

import li.cil.oc.util.GameTimeFormatter;
import li.cil.oc.util.ScalaClosure;
import li.cil.repack.org.luaj.vm2.LuaTable;
import li.cil.repack.org.luaj.vm2.LuaValue;

import java.util.Optional;

public class OSAPI extends LuaJAPI {
    public OSAPI(LuaJLuaArchitecture owner) {
        super(owner);
    }

    @Override
    public void initialize() {
        final LuaTable os = LuaValue.tableOf();

        os.set("clock", new ScalaClosure(args -> LuaValue.valueOf(machine.cpuTime())));

        // Date formatting function.
        os.set("date", new ScalaClosure(args -> {
            final String format =
                    args.narg() > 0 && args.isstring(1) ? args.tojstring(1)
                            : "%d/%m/%y %H:%M:%S";
            final double time =
                    args.narg() > 1 && args.isnumber(2) ? args.todouble(2)
                            : (double) ((machine.worldTime() + 6000) * 60 * 60 / 1000);

            final GameTimeFormatter.DateTime dt = GameTimeFormatter.parse(time);

            // Just ignore the allowed leading '!', Minecraft has no time zones...
            final String f = format.startsWith("!") ? format.substring(1) : format;
            if (f.equals("*t")) {
                final LuaTable table = LuaValue.tableOf(0, 8);
                table.set("year", LuaValue.valueOf(dt.year));
                table.set("month", LuaValue.valueOf(dt.month));
                table.set("day", LuaValue.valueOf(dt.day));
                table.set("hour", LuaValue.valueOf(dt.hour));
                table.set("min", LuaValue.valueOf(dt.minute));
                table.set("sec", LuaValue.valueOf(dt.second));
                table.set("wday", LuaValue.valueOf(dt.weekDay));
                table.set("yday", LuaValue.valueOf(dt.yearDay));
                return table;
            } else {
                return LuaValue.valueOf(GameTimeFormatter.format(f, dt));
            }
        }));

        // Return ingame time for os.time().
        os.set("time", new ScalaClosure(args -> {
            if (args.isnoneornil(1)) {
                // Game time is in ticks, so that each day has 24000 ticks, meaning
                // one hour is game time divided by one thousand. Also, Minecraft
                // starts days at 6 o'clock, versus the 1 o'clock of timestamps so we
                // add those five hours. Thus:
                // timestamp = (time + 5000) * 60[kh] * 60[km] / 1000[s]
                return LuaValue.valueOf((double) ((machine.worldTime() + 5000) * 60 * 60 / 1000));
            } else {
                final LuaTable table = args.checktable(1);

                final int sec = getField(table, "sec", 0);
                final int min = getField(table, "min", 0);
                final int hour = getField(table, "hour", 12);
                final int mday = getField(table, "day", -1);
                final int mon = getField(table, "month", -1);
                final int year = getField(table, "year", -1);

                final Optional<Integer> time = GameTimeFormatter.mktime(year, mon, mday, hour, min, sec);
                return time.isPresent() ? LuaValue.valueOf(time.get().intValue()) : LuaValue.NIL;
            }
        }));

        lua().set("os", os);
    }

    private static int getField(LuaTable table, String key, int d) {
        final LuaValue res = table.get(key);
        if (!res.isint()) {
            if (d < 0) throw LuaJAPI.<RuntimeException>sneakyThrow(new Exception("field '" + key + "' missing in date table"));
            return d;
        }
        return res.toint();
    }
}
