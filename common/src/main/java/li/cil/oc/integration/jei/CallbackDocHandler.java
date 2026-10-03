package li.cil.oc.integration.jei;

import com.google.common.base.Strings;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.server.machine.Callbacks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * "Uses" pages listing the Lua callbacks (with their documentation) of every component item. Client only.
 */
public final class CallbackDocHandler {
    private CallbackDocHandler() {
    }

    private static final Pattern DocPattern = Pattern.compile("(?s)^function(\\(.*?\\).*?) -- (.*)$");

    private static final Pattern VexPattern = Pattern.compile("(?s)^function(\\(.*?\\).*?); (.*)$");

    private static final int LinesPerPage = 12;

    public static List<CallbackDocRecipe> getRecipes(IRecipeRegistration registration) {
        final List<CallbackDocRecipe> result = new ArrayList<>();
        for (ItemStack stack : registration.getIngredientManager().getAllItemStacks()) {
            final List<String> callbacks = new ArrayList<>();
            for (Class<?> env : Driver.environmentsFor(stack)) {
                callbacks.addAll(getCallbacks(env));
            }

            if (!callbacks.isEmpty()) {
                final List<String> pages = new ArrayList<>();
                String last = "";
                callbacks.sort(null);
                for (String doc : callbacks) {
                    if (lineCount(last) + 2 + lineCount(doc) > LinesPerPage) {
                        // We've potentially got some pretty long documentation here, split it up first.
                        addPages(pages, last);
                        last = doc;
                    } else if (!last.isEmpty()) {
                        last = last + "\n\n" + doc;
                    } else {
                        last = doc;
                    }
                }
                // The last page may be too long as well.
                addPages(pages, last);

                for (String page : pages) {
                    result.add(new CallbackDocRecipe(stack, page));
                }
            }
        }
        return result;
    }

    private static int lineCount(String s) {
        return (int) s.lines().count();
    }

    private static void addPages(List<String> pages, String text) {
        final List<String> lines = text.lines().toList();
        for (int i = 0; i < lines.size(); i += LinesPerPage) {
            pages.add(String.join("\n", lines.subList(i, Math.min(i + LinesPerPage, lines.size()))));
        }
    }

    private static List<String> getCallbacks(Class<?> env) {
        final List<String> result = new ArrayList<>();
        if (env == null) return result;
        for (Map.Entry<String, Callbacks.Callback> entry : Callbacks.fromClass(env).entrySet()) {
            final String name = entry.getKey();
            final String doc = entry.getValue().annotation.doc();
            if (Strings.isNullOrEmpty(doc)) {
                result.add(name);
            } else {
                final String signature;
                final String documentation;
                final Matcher doc1 = DocPattern.matcher(doc);
                final Matcher doc2 = VexPattern.matcher(doc);
                if (doc1.matches()) {
                    signature = name + doc1.group(1);
                    documentation = doc1.group(2);
                } else if (doc2.matches()) {
                    signature = name + doc2.group(1);
                    documentation = doc2.group(2);
                } else {
                    signature = name;
                    documentation = doc;
                }
                result.add(wrap(signature, 160).stream().map(l -> ChatFormatting.BLACK + l).collect(Collectors.joining("\n")) +
                        ChatFormatting.RESET + "\n" +
                        wrap(documentation, 152).stream().map(l -> "  " + l).collect(Collectors.joining("\n")));
            }
        }
        return result;
    }

    private static List<String> wrap(String line, int width) {
        final List<String> list = new ArrayList<>();
        Minecraft.getInstance().font.getSplitter().splitLines(line, width, Style.EMPTY, true,
                (style, start, end) -> list.add(line.substring(start, end)));
        return list;
    }

    public record CallbackDocRecipe(ItemStack stack, String page) {
    }

    public static final class CallbackDocRecipeCategory implements IRecipeCategory<CallbackDocRecipe> {
        public static final CallbackDocRecipeCategory INSTANCE = new CallbackDocRecipeCategory();

        public static final RecipeType<CallbackDocRecipe> TYPE = RecipeType.create(OpenComputers.ID, "part_api", CallbackDocRecipe.class);

        public static final int recipeWidth = 160;
        public static final int recipeHeight = 125;

        private IDrawable icon;

        private CallbackDocRecipeCategory() {
        }

        public void initialize(IGuiHelper guiHelper) {
            icon = new DrawableAnimatedIcon(new ResourceLocation(Settings.resourceDomain, "textures/items/tablet_on.png"), 0, 0, 16, 16, 16, 32,
                    guiHelper.createTickTimer(20, 1, true), 0, 16);
        }

        @Override
        public RecipeType<CallbackDocRecipe> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.literal("OpenComputers API");
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
        public void setRecipe(IRecipeLayoutBuilder builder, CallbackDocRecipe recipe, IFocusGroup focuses) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(recipe.stack());
        }

        @Override
        public void draw(CallbackDocRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
            final Font font = Minecraft.getInstance().font;
            final List<String> lines = recipe.page().lines().toList();
            for (int line = 0; line < lines.size(); line++) {
                graphics.drawString(font, lines.get(line), 4, 4 + line * (font.lineHeight + 1), 0x333333, false);
            }
        }
    }
}
