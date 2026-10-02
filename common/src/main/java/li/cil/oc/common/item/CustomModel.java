package li.cil.oc.common.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * Items whose model depends on the stack (floppy color, terminal / tablet state).
 * <p>
 * Port note: this no longer references client classes. Locations are plain model ids
 * in the form {@code opencomputers:item/<name>} (i.e. {@code models/item/<name>.json}),
 * which is what Forge's {@code ModelEvent.RegisterAdditional} / Fabric's
 * {@code ModelLoadingPlugin} take for extra models. The client is responsible for
 * registering {@link #modelLocations()} as additional models and for swapping in
 * {@link #getModelLocation(ItemStack)} (e.g. via a wrapping BakedModel / ItemOverrides).
 * The old {@code bakeModels(ModelBakeEvent)} hook is gone (the drone uses
 * {@code builtin/entity} + a BEWLR now).
 */
public interface CustomModel {
    ResourceLocation getModelLocation(ItemStack stack);

    /** Formerly {@code registerModelLocations()}: all models this item may use. */
    default List<ResourceLocation> modelLocations() {
        return Collections.emptyList();
    }
}
