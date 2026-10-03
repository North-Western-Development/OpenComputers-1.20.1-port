package li.cil.oc.integration.jei;

import com.mojang.blaze3d.platform.InputConstants;
import li.cil.oc.Localization;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Manual;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * "Uses" page for every item that has a manual page, with a button opening the manual there. Client only.
 */
public final class ManualUsageHandler {
    private ManualUsageHandler() {
    }

    public static List<ManualUsageRecipe> getRecipes(IRecipeRegistration registration) {
        final List<ManualUsageRecipe> result = new ArrayList<>();
        for (ItemStack stack : registration.getIngredientManager().getAllItemStacks()) {
            final String path = Manual.pathFor(stack);
            if (path != null) result.add(new ManualUsageRecipe(stack, path));
        }
        return result;
    }

    public record ManualUsageRecipe(ItemStack stack, String path) {
    }

    public static final class ManualUsageRecipeCategory implements IRecipeCategory<ManualUsageRecipe> {
        public static final ManualUsageRecipeCategory INSTANCE = new ManualUsageRecipeCategory();

        public static final RecipeType<ManualUsageRecipe> TYPE = RecipeType.create(OpenComputers.ID, "manual", ManualUsageRecipe.class);

        public static final int recipeWidth = 160;
        public static final int recipeHeight = 125;

        private IDrawable icon;
        private Button button;

        private ManualUsageRecipeCategory() {
        }

        public void initialize(IGuiHelper guiHelper) {
            icon = guiHelper.drawableBuilder(new ResourceLocation(Settings.resourceDomain, "textures/items/manual.png"), 0, 0, 16, 16).setTextureSize(16, 16).build();
            button = Button.builder(Localization.localizeLater("nei.usage.oc.Manual"), b -> {
            }).bounds((recipeWidth - 100) / 2, 10, 100, 20).build();
        }

        @Override
        public RecipeType<ManualUsageRecipe> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.literal("OpenComputers Manual");
        }

        @Override
        public int getWidth() {
            return recipeWidth;
        }

        @Override
        public int getHeight() {
            return recipeHeight;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, ManualUsageRecipe recipe, IFocusGroup focuses) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(recipe.stack());
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, ManualUsageRecipe recipe, IFocusGroup focuses) {
            builder.addInputHandler(new IJeiInputHandler() {
                @Override
                public ScreenRectangle getArea() {
                    return new ScreenRectangle(0, 0, recipeWidth, recipeHeight);
                }

                @Override
                public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
                    final InputConstants.Key key = input.getKey();
                    if (key.getType() != InputConstants.Type.MOUSE) return false;
                    if (key.getValue() == GLFW.GLFW_MOUSE_BUTTON_LEFT || (button != null && button.isMouseOver(mouseX, mouseY))) {
                        if (!input.isSimulate()) {
                            final Minecraft minecraft = Minecraft.getInstance();
                            if (minecraft.player != null) {
                                minecraft.player.closeContainer();
                                Manual.openFor(minecraft.player);
                                Manual.navigate(recipe.path());
                            }
                        }
                        return true;
                    }
                    return false;
                }
            });
        }

        @Override
        public void draw(ManualUsageRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
            if (button != null) button.render(graphics, (int) mouseX, (int) mouseY, 0);
        }
    }
}
