package li.cil.oc.integration;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;

public class ManagedBlockEntityEnvironment<T> extends AbstractManagedEnvironment {
    protected final T tileEntity;

    public ManagedBlockEntityEnvironment(final T tileEntity, final String name) {
        this.tileEntity = tileEntity;
        setNode(Network.newNode(this, Visibility.Network).
                withComponent(name).
                create());
    }
}
