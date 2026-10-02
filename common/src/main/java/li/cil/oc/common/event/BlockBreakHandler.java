package li.cil.oc.common.event;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import li.cil.oc.common.block.SimpleBlock;

/**
 * Replaces Forge's {@code IForgeBlock.removedByPlayer} for OC blocks: lets blocks veto being broken
 * by a player (creative cases/robots) and run logic before removal (robots saving their state).
 * See {@link SimpleBlock#removedByPlayer}.
 */
public final class BlockBreakHandler {
    private BlockBreakHandler() {
    }

    public static void register() {
        BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            if (state.getBlock() instanceof SimpleBlock block && !block.removedByPlayer(state, level, pos, player)) {
                return EventResult.interruptFalse();
            }
            return EventResult.pass();
        });
    }
}
