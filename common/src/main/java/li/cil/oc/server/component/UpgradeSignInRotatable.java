package li.cil.oc.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;

public class UpgradeSignInRotatable extends UpgradeSign {
    public final EnvironmentHost host;

    private final Rotatable rotatable;

    public <T extends EnvironmentHost & Rotatable> UpgradeSignInRotatable(T host) {
        this.host = host;
        this.rotatable = host;
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("sign", Visibility.Neighbors).
            withConnector().
            create());
    }

    @Override
    public ComponentConnector node() {
        return (ComponentConnector) super.node();
    }

    @Override
    public EnvironmentHost host() {
        return host;
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function():string -- Get the text on the sign in front of the host.")
    public Object[] getValue(Context context, Arguments args) {
        return getValue(findSign(rotatable.facing()));
    }

    @Callback(doc = "function(value:string):string -- Set the text on the sign in front of the host.")
    public Object[] setValue(Context context, Arguments args) {
        return setValue(findSign(rotatable.facing()), args.checkString(0));
    }
}
