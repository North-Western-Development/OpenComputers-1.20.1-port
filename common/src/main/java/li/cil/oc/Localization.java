package li.cil.oc;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.stream.Collectors;

public final class Localization {
    private Localization() {
    }

    private static String resolveKey(String key) {
        return canLocalize(Settings.namespace + key) ? Settings.namespace + key : key;
    }

    public static boolean canLocalize(String key) {
        return Language.getInstance().has(key);
    }

    public static MutableComponent localizeLater(String formatKey, Object... values) {
        return Component.translatable(resolveKey(formatKey), values);
    }

    public static MutableComponent localizeLater(String key) {
        return Component.translatable(resolveKey(key));
    }

    public static String localizeImmediately(String formatKey, Object... values) {
        final String k = resolveKey(formatKey);
        final Language lm = Language.getInstance();
        if (!lm.has(k)) return k;
        return String.format(lm.getOrDefault(k), values).lines().map(String::trim).collect(Collectors.joining("\n"));
    }

    public static String localizeImmediately(String key) {
        final String k = resolveKey(key);
        final Language lm = Language.getInstance();
        if (!lm.has(k)) return k;
        return lm.getOrDefault(k).lines().map(String::trim).collect(Collectors.joining("\n"));
    }

    public static final class Analyzer {
        private Analyzer() {
        }

        public static MutableComponent Address(String value) {
            final MutableComponent result = localizeLater("gui.Analyzer.Address", value);
            return result.setStyle(result.getStyle()
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, localizeLater("gui.Analyzer.CopyToClipboard"))));
        }

        public static MutableComponent AddressCopied() {
            return localizeLater("gui.Analyzer.AddressCopied");
        }

        public static MutableComponent ChargerSpeed(double value) {
            return localizeLater("gui.Analyzer.ChargerSpeed", (int) (value * 100) + "%");
        }

        public static MutableComponent ComponentName(String value) {
            return localizeLater("gui.Analyzer.ComponentName", value);
        }

        public static MutableComponent Components(int count, int maxCount) {
            return localizeLater("gui.Analyzer.Components", count + "/" + maxCount);
        }

        public static MutableComponent LastError(String value) {
            return localizeLater("gui.Analyzer.LastError", localizeLater(value));
        }

        public static MutableComponent RobotOwner(String owner) {
            return localizeLater("gui.Analyzer.RobotOwner", owner);
        }

        public static MutableComponent RobotName(String name) {
            return localizeLater("gui.Analyzer.RobotName", name);
        }

        public static MutableComponent RobotXp(double experience, int level) {
            return localizeLater("gui.Analyzer.RobotXp", String.format("%.2f", experience), Integer.toString(level));
        }

        public static MutableComponent StoredEnergy(String value) {
            return localizeLater("gui.Analyzer.StoredEnergy", value);
        }

        public static MutableComponent TotalEnergy(String value) {
            return localizeLater("gui.Analyzer.TotalEnergy", value);
        }

        public static MutableComponent Users(Iterable<String> list) {
            return localizeLater("gui.Analyzer.Users", String.join(", ", list));
        }

        public static MutableComponent WirelessStrength(double value) {
            return localizeLater("gui.Analyzer.WirelessStrength", Integer.toString((int) value));
        }
    }

    public static final class Assembler {
        private Assembler() {
        }

        public static String InsertTemplate() {
            return localizeImmediately("gui.Assembler.InsertCase");
        }

        public static String CollectResult() {
            return localizeImmediately("gui.Assembler.Collect");
        }

        public static MutableComponent InsertCPU() {
            return localizeLater("gui.Assembler.InsertCPU");
        }

        public static MutableComponent InsertRAM() {
            return localizeLater("gui.Assembler.InsertRAM");
        }

        public static MutableComponent Complexity(int complexity, int maxComplexity) {
            final MutableComponent message = localizeLater("gui.Assembler.Complexity", Integer.toString(complexity), Integer.toString(maxComplexity));
            if (complexity > maxComplexity) return Component.literal("§4").append(message);
            else return message;
        }

        public static String Run() {
            return localizeImmediately("gui.Assembler.Run");
        }

        public static String Progress(double progress, String timeRemaining) {
            return localizeImmediately("gui.Assembler.Progress", Integer.toString((int) progress), timeRemaining);
        }

        public static MutableComponent Warning(String name) {
            return Component.literal("§7- ").append(localizeLater("gui.Assembler.Warning." + name));
        }

        public static MutableComponent Warnings() {
            return localizeLater("gui.Assembler.Warnings");
        }
    }

    public static final class Chat {
        private Chat() {
        }

        public static MutableComponent WarningLuaFallback() {
            return Component.literal("§aOpenComputers§f: ").append(localizeLater("gui.Chat.WarningLuaFallback"));
        }

        public static MutableComponent WarningProjectRed() {
            return Component.literal("§aOpenComputers§f: ").append(localizeLater("gui.Chat.WarningProjectRed"));
        }

        public static MutableComponent WarningRecipes() {
            return Component.literal("§aOpenComputers§f: ").append(localizeLater("gui.Chat.WarningRecipes"));
        }

        public static MutableComponent WarningClassTransformer() {
            return Component.literal("§aOpenComputers§f: ").append(localizeLater("gui.Chat.WarningClassTransformer"));
        }

        public static MutableComponent WarningLink(String url) {
            return Component.literal("§aOpenComputers§f: ").append(localizeLater("gui.Chat.WarningLink", url));
        }

        public static MutableComponent InfoNewVersion(String version) {
            return Component.literal("§aOpenComputers§f: ").append(localizeLater("gui.Chat.NewVersion", version));
        }

        public static MutableComponent TextureName(String name) {
            return Component.literal("§aOpenComputers§f: ").append(localizeLater("gui.Chat.TextureName", name));
        }
    }

    public static final class Computer {
        private Computer() {
        }

        public static String TurnOff() {
            return localizeImmediately("gui.Robot.TurnOff");
        }

        public static String TurnOn() {
            return localizeImmediately("gui.Robot.TurnOn");
        }

        public static String Power() {
            return localizeImmediately("gui.Robot.Power");
        }
    }

    public static final class Drive {
        private Drive() {
        }

        public static String Managed() {
            return localizeImmediately("gui.Drive.Managed");
        }

        public static String Unmanaged() {
            return localizeImmediately("gui.Drive.Unmanaged");
        }

        public static String Warning() {
            return localizeImmediately("gui.Drive.Warning");
        }

        public static String ReadOnlyLock() {
            return localizeImmediately("gui.Drive.ReadOnlyLock");
        }

        public static String LockWarning() {
            return localizeImmediately("gui.Drive.ReadOnlyLockWarning");
        }
    }

    public static final class Raid {
        private Raid() {
        }

        public static String Warning() {
            return localizeImmediately("gui.Raid.Warning");
        }
    }

    public static final class Rack {
        private Rack() {
        }

        public static String Top() {
            return localizeImmediately("gui.Rack.Top");
        }

        public static String Bottom() {
            return localizeImmediately("gui.Rack.Bottom");
        }

        public static String Left() {
            return localizeImmediately("gui.Rack.Left");
        }

        public static String Right() {
            return localizeImmediately("gui.Rack.Right");
        }

        public static String Back() {
            return localizeImmediately("gui.Rack.Back");
        }

        public static String None() {
            return localizeImmediately("gui.Rack.None");
        }

        public static String RelayEnabled() {
            return localizeImmediately("gui.Rack.Enabled");
        }

        public static String RelayDisabled() {
            return localizeImmediately("gui.Rack.Disabled");
        }

        public static String RelayModeTooltip() {
            return localizeImmediately("gui.Rack.RelayModeTooltip");
        }
    }

    public static final class Switch {
        private Switch() {
        }

        public static String TransferRate() {
            return localizeImmediately("gui.Switch.TransferRate");
        }

        public static String PacketsPerCycle() {
            return localizeImmediately("gui.Switch.PacketsPerCycle");
        }

        public static String QueueSize() {
            return localizeImmediately("gui.Switch.QueueSize");
        }
    }

    public static final class Terminal {
        private Terminal() {
        }

        public static MutableComponent InvalidKey() {
            return localizeLater("gui.Terminal.InvalidKey");
        }

        public static MutableComponent OutOfRange() {
            return localizeLater("gui.Terminal.OutOfRange");
        }
    }

    public static final class Tooltip {
        private Tooltip() {
        }

        public static String DiskUsage(long used, long capacity) {
            return localizeImmediately("tooltip.diskusage", Long.toString(used), Long.toString(capacity));
        }

        public static String DiskMode(boolean isUnmanaged) {
            return localizeImmediately(isUnmanaged ? "tooltip.diskmodeunmanaged" : "tooltip.diskmodemanaged");
        }

        public static String Materials() {
            return localizeImmediately("tooltip.materials");
        }

        public static String DiskLock(String lockInfo) {
            return lockInfo.isEmpty() ? "" : localizeImmediately("tooltip.disklocked", lockInfo);
        }

        public static String Tier(int tier) {
            return localizeImmediately("tooltip.tier", Integer.toString(tier));
        }

        public static String PrintBeaconBase() {
            return localizeImmediately("tooltip.print.BeaconBase");
        }

        public static String PrintLightValue(int level) {
            return localizeImmediately("tooltip.print.LightValue", Integer.toString(level));
        }

        public static String PrintRedstoneLevel(int level) {
            return localizeImmediately("tooltip.print.RedstoneLevel", Integer.toString(level));
        }

        public static String MFULinked(boolean isLinked) {
            return localizeImmediately(isLinked ? "tooltip.upgrademf.Linked" : "tooltip.upgrademf.Unlinked");
        }

        public static String ExperienceLevel(double level) {
            return localizeImmediately("tooltip.robot_level", Double.toString(level));
        }
    }
}
