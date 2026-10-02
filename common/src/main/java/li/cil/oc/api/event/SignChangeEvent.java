package li.cil.oc.api.event;

import net.minecraft.world.level.block.entity.SignBlockEntity;

/**
 * A bit more specific sign change event that holds information about new text of the sign. Used in the sign upgrade.
 */
public abstract class SignChangeEvent extends OCEvent {
    public final SignBlockEntity sign;
    public final String[] lines;

    private SignChangeEvent(SignBlockEntity sign, String[] lines) {
        this.sign = sign;
        this.lines = lines;
    }

    public static class Pre extends SignChangeEvent {
        @Override
        public boolean isCancelable() {
            return true;
        }

        public Pre(SignBlockEntity sign, String[] lines) {
            super(sign, lines);
        }
    }

    public static class Post extends SignChangeEvent {
        public Post(SignBlockEntity sign, String[] lines) {
            super(sign, lines);
        }
    }
}
