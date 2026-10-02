package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Keyboard.UsabilityChecker;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO key up when screen is disconnected from which the key down came
// TODO key up after load for anything that was pressed

public class Keyboard extends AbstractManagedEnvironment implements li.cil.oc.api.internal.Keyboard, DeviceInfo {
    public final EnvironmentHost host;

    public final Component node;

    public final Map<Player, Map<Integer, Character>> pressedKeys = new HashMap<>();

    public Optional<UsabilityChecker> usableOverride = Optional.empty();

    public Keyboard(EnvironmentHost host) {
        this.host = host;
        this.node = (Component) Network.newNode(this, Visibility.Network).
                withComponent("keyboard").
                create();
        setNode(node);
    }

    @Override
    public Component node() {
        return node;
    }

    @Override
    public void setUsableOverride(UsabilityChecker callback) {
        usableOverride = Optional.ofNullable(callback);
    }

    // ----------------------------------------------------------------------- //

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            final Map<String, String> info = new HashMap<>();
            info.put(DeviceAttribute.Class, DeviceClass.Input);
            info.put(DeviceAttribute.Description, "Keyboard");
            info.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            info.put(DeviceAttribute.Product, "Fancytyper MX-Stone");
            deviceInfo = info;
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    public void releasePressedKeys(Player player) {
        final Map<Integer, Character> keys = pressedKeys.get(player);
        if (keys != null) {
            for (Map.Entry<Integer, Character> entry : keys.entrySet()) {
                if (Settings.get().inputUsername) {
                    signal(player, "key_up", entry.getValue(), entry.getKey(), player.getName().getString());
                } else {
                    signal(player, "key_up", entry.getValue(), entry.getKey());
                }
            }
        }
        pressedKeys.remove(player);
    }

    // ----------------------------------------------------------------------- //

    private static final Pattern LINES_WITH_SEPARATORS = Pattern.compile("[^\\r\\n]*(\\r\\n|\\r|\\n)|[^\\r\\n]+$");

    @Override
    public void onMessage(Message message) {
        final Object[] data = message.data();
        if ("keyboard.keyDown".equals(message.name()) && data.length == 3 && data[0] instanceof Player p && data[1] instanceof Character c && data[2] instanceof Integer code) {
            if (isUsableByPlayer(p)) {
                pressedKeys.computeIfAbsent(p, k -> new HashMap<>()).put(code, c);
                if (Settings.get().inputUsername) {
                    signal(p, "key_down", c, code, p.getName().getString());
                } else {
                    signal(p, "key_down", c, code);
                }
            }
        } else if ("keyboard.keyUp".equals(message.name()) && data.length == 3 && data[0] instanceof Player p && data[1] instanceof Character c && data[2] instanceof Integer code) {
            final Map<Integer, Character> keys = pressedKeys.get(p);
            if (keys != null && keys.containsKey(code)) {
                keys.remove(code);
                if (Settings.get().inputUsername) {
                    signal(p, "key_up", c, code, p.getName().getString());
                } else {
                    signal(p, "key_up", c, code);
                }
            }
        } else if ("keyboard.textInput".equals(message.name()) && data.length == 2 && data[0] instanceof Player p && data[1] instanceof Integer codePt) {
            if (Settings.get().inputUsername) {
                signal(p, "text_input", new String(Character.toChars(codePt)), p.getName().getString());
            } else {
                signal(p, "text_input", new String(Character.toChars(codePt)));
            }
        } else if ("keyboard.clipboard".equals(message.name()) && data.length == 2 && data[0] instanceof Player p && data[1] instanceof String value) {
            if (isUsableByPlayer(p)) {
                // Equivalent of Scala's linesWithSeparators.
                final Matcher m = LINES_WITH_SEPARATORS.matcher(value);
                while (m.find()) {
                    final String line = m.group();
                    if (line.isEmpty()) continue;
                    if (Settings.get().inputUsername) {
                        signal(p, "clipboard", line, p.getName().getString());
                    } else {
                        signal(p, "clipboard", line);
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    public boolean isUsableByPlayer(Player p) {
        if (usableOverride.isPresent()) return usableOverride.get().isUsableByPlayer(this, p);
        else return p.distanceToSqr(host.xPosition(), host.yPosition(), host.zPosition()) <= 64;
    }

    protected void signal(Object... args) {
        node.sendToReachable("computer.checked_signal", args);
    }
}
