package li.cil.oc.server.fs;

/**
 * Port note: Scala mixin trait over {@link VirtualFileSystem}; now an abstract class.
 */
public abstract class Volatile extends VirtualFileSystem {
    @Override
    public void close() {
        super.close();
        root.children.clear();
    }
}
