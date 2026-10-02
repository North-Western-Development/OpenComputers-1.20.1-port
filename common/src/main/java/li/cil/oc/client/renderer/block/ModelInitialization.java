package li.cil.oc.client.renderer.block;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.client.platform.RenderPlatform;
import li.cil.oc.common.item.CustomModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Replaces the models of blocks/items with code generated geometry and
 * provides stack dependent item models.
 * <p>
 * Model loading itself is loader specific, see {@link RenderPlatform#registerModelHooks()}.
 * The replacement logic lives here:
 * <ul>
 *     <li>cable, net splitter, print, robot: block states point to {@code block/air};
 *     all their model locations (including {@code #inventory}) are replaced.</li>
 *     <li>robot afterimage: replaced with an empty model.</li>
 *     <li>screens and rack: replaced at bake time, wrapping the JSON model.</li>
 *     <li>items implementing {@link CustomModel} or registered via
 *     {@link #registerItemModelOverride}: their {@code #inventory} model is wrapped
 *     so that the model is chosen per stack; {@link CustomModel#modelLocations()}
 *     are loaded as additional models.</li>
 * </ul>
 */
public final class ModelInitialization {
    private ModelInitialization() {
    }

    private static final Set<ResourceLocation> additionalModels = new LinkedHashSet<>();

    private static final List<ItemModelOverride> itemModelOverrides = new ArrayList<>();

    private record ItemModelOverride(Supplier<? extends ItemLike> item, Function<ItemStack, ResourceLocation> modelForStack) {
    }

    private static boolean initialized = false;

    /**
     * Installs the platform model hooks. Must be called during client
     * initialization, before resources are loaded the first time.
     */
    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        RenderPlatform.registerModelHooks();
    }

    // ----------------------------------------------------------------------- //

    /**
     * Requests an additional model to be loaded and baked, so that it can be
     * used in {@link #registerItemModelOverride}. Accepts plain model locations
     * ({@code opencomputers:item/floppy_black}) or {@code #inventory} model
     * resource locations (converted to their item model location).
     * Must be called before resources are (re)loaded to take effect.
     */
    public static synchronized void registerAdditionalModel(ResourceLocation location) {
        additionalModels.add(normalize(location));
    }

    /**
     * Registers a per-stack model selection for an item (replacement for the
     * Forge-only {@code CustomModel}). The returned locations should have been
     * registered via {@link #registerAdditionalModel}; if the function returns
     * null or the model is missing, the item's normal model is used.
     */
    public static synchronized void registerItemModelOverride(Supplier<? extends ItemLike> item, Function<ItemStack, ResourceLocation> modelForStack) {
        itemModelOverrides.add(new ItemModelOverride(item, modelForStack));
    }

    /**
     * Called by the platform hooks to determine which additional models to load.
     */
    public static synchronized Collection<ResourceLocation> additionalModels() {
        final Set<ResourceLocation> result = new LinkedHashSet<>(additionalModels);
        // Items with stack dependent models (floppies, tablets, terminals).
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof CustomModel custom && OpenComputers.ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                for (ResourceLocation location : custom.modelLocations()) {
                    result.add(normalize(location));
                }
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(result));
    }

    /**
     * Called by the platform hooks for every baked top level model. Returns the
     * model to use instead (or the passed one).
     */
    public static BakedModel modifyBakedModel(ResourceLocation id, @Nullable BakedModel model) {
        if (!(id instanceof ModelResourceLocation location) || !OpenComputers.ID.equals(location.getNamespace())) {
            return model;
        }
        final String name = location.getPath();
        final boolean isItem = "inventory".equals(location.getVariant());

        if (Constants.BlockName.Cable.equals(name)) return CableModel.INSTANCE;
        if (Constants.BlockName.NetSplitter.equals(name)) return NetSplitterModel.INSTANCE;
        if (Constants.BlockName.Print.equals(name)) return PrintModel.INSTANCE;
        if (Constants.BlockName.Robot.equals(name)) return RobotModel.INSTANCE;
        if (Constants.BlockName.RobotAfterimage.equals(name)) return NullModel.INSTANCE;
        if (Constants.BlockName.ScreenTier1.equals(name) ||
            Constants.BlockName.ScreenTier2.equals(name) ||
            Constants.BlockName.ScreenTier3.equals(name)) return ScreenModel.INSTANCE;
        if (Constants.BlockName.Rack.equals(name)) return new ServerRackModel(model);

        if (isItem && model != null) {
            final List<Function<ItemStack, ResourceLocation>> overrides = new ArrayList<>();
            final Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(location.getNamespace(), name));
            if (item instanceof CustomModel custom) {
                overrides.add(custom::getModelLocation);
            }
            synchronized (ModelInitialization.class) {
                for (ItemModelOverride entry : itemModelOverrides) {
                    final ResourceLocation itemId = itemId(entry.item());
                    if (itemId != null && itemId.getNamespace().equals(location.getNamespace()) && itemId.getPath().equals(name)) {
                        overrides.add(entry.modelForStack());
                    }
                }
            }
            if (!overrides.isEmpty()) {
                return new StackSelectingModel(model, overrides);
            }
        }

        return model;
    }

    // ----------------------------------------------------------------------- //

    @Nullable
    private static ResourceLocation itemId(Supplier<? extends ItemLike> supplier) {
        try {
            final ItemLike itemLike = supplier.get();
            if (itemLike == null) return null;
            final Item item = itemLike.asItem();
            return BuiltInRegistries.ITEM.getKey(item);
        } catch (Throwable t) {
            return null;
        }
    }

    private static ResourceLocation normalize(ResourceLocation location) {
        if (location instanceof ModelResourceLocation mrl && "inventory".equals(mrl.getVariant())) {
            return new ResourceLocation(mrl.getNamespace(), "item/" + mrl.getPath());
        }
        return location;
    }

    /**
     * Wraps an item model and picks the actual model per stack.
     */
    private static final class StackSelectingModel implements BakedModel {
        private final BakedModel original;
        private final ItemOverrides overrides;

        StackSelectingModel(BakedModel original, List<Function<ItemStack, ResourceLocation>> selectors) {
            this.original = original;
            this.overrides = new SmartBlockModelBase.StackOverrides(stack -> {
                for (Function<ItemStack, ResourceLocation> selector : selectors) {
                    final ResourceLocation location = selector.apply(stack);
                    if (location != null) {
                        final BakedModel selected = RenderPlatform.getModel(normalize(location));
                        if (selected != null && selected != Minecraft.getInstance().getModelManager().getMissingModel()) {
                            return selected.getOverrides().resolve(selected, stack, null, null, 0);
                        }
                    }
                }
                return original.getOverrides().resolve(original, stack, null, null, 0);
            }) {
                @Nullable
                @Override
                public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                    final BakedModel resolved = super.resolve(model, stack, level, entity, seed);
                    return resolved != null ? resolved : original;
                }
            };
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return original.getQuads(state, side, rand);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return original.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return original.isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return original.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer() {
            return original.isCustomRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return original.getParticleIcon();
        }

        @Override
        public ItemTransforms getTransforms() {
            return original.getTransforms();
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }
    }
}
