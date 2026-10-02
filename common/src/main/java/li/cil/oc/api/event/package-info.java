/**
 * Events dispatched by OpenComputers to allow other mods to hook into some
 * of its functionality.
 * <p/>
 * All events extend {@link li.cil.oc.api.event.OCEvent} and are posted to
 * {@link li.cil.oc.api.event.EventBus#INSTANCE}; subscribe via
 * <tt>EventBus.INSTANCE.register(SomeEvent.class, listener)</tt>.
 */
package li.cil.oc.api.event;

import li.cil.oc.api.API;