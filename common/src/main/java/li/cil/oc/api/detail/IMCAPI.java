package li.cil.oc.api.detail;

/**
 * Receiver for the messages sent via {@link li.cil.oc.api.IMC}.
 * <p/>
 * In earlier versions these messages were transported using Forge's
 * <tt>InterModComms</tt>. Since the 1.20.1 port is loader independent, the
 * {@link li.cil.oc.api.IMC} utility methods deliver their messages directly to
 * the implementation of this interface, which OpenComputers assigns to
 * {@link li.cil.oc.api.API#imc} during initialization.
 */
public interface IMCAPI {
    /**
     * Handle a single message.
     *
     * @param method  the message key, one of the constants in {@link li.cil.oc.api.IMC}.
     * @param payload the message payload: either a {@link String} or a
     *                {@link net.minecraft.nbt.CompoundTag}, depending on the
     *                message (exactly what used to be sent via IMC).
     */
    void handle(String method, Object payload);
}
