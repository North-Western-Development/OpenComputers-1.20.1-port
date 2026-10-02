package li.cil.oc.common.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import static li.cil.oc.util.ResultWrapper.result;

public class Screen extends TextBuffer {
    public final li.cil.oc.common.tileentity.Screen screen;

    public Screen(li.cil.oc.common.tileentity.Screen screen) {
        super(screen);
        this.screen = screen;
    }

    @Callback(direct = true, doc = "function():boolean -- Whether touch mode is inverted (sneak-activate opens GUI, instead of normal activate).")
    public Object[] isTouchModeInverted(Context computer, Arguments args) {
        return result(screen.invertTouchMode);
    }

    @Callback(doc = "function(value:boolean):boolean -- Sets whether to invert touch mode (sneak-activate opens GUI, instead of normal activate).")
    public Object[] setTouchModeInverted(Context computer, Arguments args) {
        final boolean newValue = args.checkBoolean(0);
        final boolean oldValue = screen.invertTouchMode;
        if (newValue != oldValue) {
            screen.invertTouchMode = newValue;
            li.cil.oc.server.PacketSender.sendScreenTouchMode(screen, newValue);
        }
        return result(oldValue);
    }
}
