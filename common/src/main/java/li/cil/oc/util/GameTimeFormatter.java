package li.cil.oc.util;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Function;

public final class GameTimeFormatter {
    private GameTimeFormatter() {
    }

    // Locale? What locale? Seriously though, since this would depend on the
    // server's locale I think it makes more sense to keep it English always.
    private static final String[] weekDays = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private static final String[] shortWeekDays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private static final String[] shortMonths = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static final String[] amPm = {"AM", "PM"};

    public static class DateTime {
        public final int year;
        public final int month;
        public final int day;
        public final int weekDay;
        public final int yearDay;
        public final int hour;
        public final int minute;
        public final int second;

        public DateTime(int year, int month, int day,
                        int weekDay, int yearDay,
                        int hour, int minute, int second) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.weekDay = weekDay;
            this.yearDay = yearDay;
            this.hour = hour;
            this.minute = minute;
            this.second = second;
        }
    }

    // See http://www.cplusplus.com/reference/ctime/strftime/
    private static final Map<Character, Function<DateTime, String>> specifiers = new HashMap<>();

    static {
        specifiers.put('a', t -> shortWeekDays[t.weekDay - 1]);
        specifiers.put('A', t -> weekDays[t.weekDay - 1]);
        specifiers.put('b', t -> shortMonths[t.month - 1]);
        specifiers.put('B', t -> months[t.month - 1]);
        specifiers.put('c', t -> format("%a %b %e %H:%M:%S %Y", t));
        specifiers.put('C', t -> String.format("%02d", t.year / 100));
        specifiers.put('d', t -> String.format("%02d", t.day));
        specifiers.put('D', t -> format("%m/%d/%y", t));
        specifiers.put('e', t -> String.format("%2d", t.day));
        specifiers.put('F', t -> format("%Y-%m-%d", t));
        //'g' -> (t => ""),
        //'G' -> (t => ""),
        specifiers.put('h', t -> format("%b", t));
        specifiers.put('H', t -> String.format("%02d", t.hour));
        specifiers.put('I', t -> String.format("%02d", (t.hour + 11) % 12 + 1));
        specifiers.put('j', t -> String.format("%03d", t.yearDay));
        specifiers.put('m', t -> String.format("%02d", t.month));
        specifiers.put('M', t -> String.format("%02d", t.minute));
        specifiers.put('n', t -> "\n");
        specifiers.put('p', t -> amPm[t.hour < 12 ? 0 : 1]);
        specifiers.put('r', t -> format("%I:%M:%S %p", t));
        specifiers.put('R', t -> format("%H:%M", t));
        specifiers.put('S', t -> String.format("%02d", t.second));
        specifiers.put('t', t -> "\t");
        specifiers.put('T', t -> format("%H:%M:%S", t));
        //'u' -> (t => ""),
        //'U' -> (t => ""),
        //'V' -> (t => ""),
        specifiers.put('w', t -> Integer.toString(t.weekDay - 1));
        //'W' -> (t => ""),
        specifiers.put('x', t -> format("%D", t));
        specifiers.put('X', t -> format("%T", t));
        specifiers.put('y', t -> String.format("%02d", t.year % 100));
        specifiers.put('Y', t -> String.format("%04d", t.year));
        //'z' -> (t => ""),
        //'Z' -> (t => ""),
        specifiers.put('%', t -> "%");
    }

    public static DateTime parse(double time) {
        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis((long) (time * 1000));

        return new DateTime(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.DAY_OF_WEEK),
                calendar.get(Calendar.DAY_OF_YEAR),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
    }

    public static String format(String format, DateTime time) {
        final StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < format.length()) {
            final char c = format.charAt(i++);
            if (c == '%' && i < format.length()) {
                final Function<DateTime, String> specifier = specifiers.get(format.charAt(i++));
                if (specifier != null) {
                    result.append(specifier.apply(time));
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static Optional<Integer> mktime(int year, int mon, int mday, int hour, int min, int sec) {
        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, mon - 1);
        calendar.set(Calendar.DAY_OF_MONTH, mday);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, sec);

        return Optional.of((int) (calendar.getTimeInMillis() / 1000));
    }
}
