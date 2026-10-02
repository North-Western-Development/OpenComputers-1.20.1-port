package li.cil.oc.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import li.cil.oc.Localization;
import li.cil.oc.client.KeyBindings;
import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.traits.Window;
import li.cil.oc.client.renderer.markdown.Document;
import li.cil.oc.client.renderer.markdown.segment.InteractiveSegment;
import li.cil.oc.client.renderer.markdown.segment.Segment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Manual extends Window {
    public static final int documentMaxWidth = 230;
    public static final int documentMaxHeight = 176;
    public static final int scrollPosX = 244;
    public static final int scrollPosY = 6;
    public static final int scrollWidth = 6;
    public static final int scrollHeight = 180;
    public static final int tabPosX = -23;
    public static final int tabPosY = 7;
    public static final int tabWidth = 23;
    public static final int tabHeight = 26;
    public static final int maxTabsPerSide = 7;

    public boolean isScrolling = false;
    public Segment document = null;
    public int documentHeight = 0;
    public Optional<InteractiveSegment> currentSegment = Optional.empty();
    protected ImageButton scrollButton;

    private final List<ImageButton> tabButtons = new ArrayList<>();

    public Manual() {
        super(Component.empty());
    }

    @Override
    public int windowWidth() {
        return 256;
    }

    @Override
    public int windowHeight() {
        return 192;
    }

    @Override
    public ResourceLocation backgroundImage() {
        return Textures.GUI.Manual;
    }

    private boolean canScroll() {
        return maxOffset() > 0;
    }

    public int offset() {
        return li.cil.oc.client.Manual.history.peek().offset;
    }

    public int maxOffset() {
        return documentHeight - documentMaxHeight;
    }

    public String resolveLink(String path, String current) {
        if (path.startsWith("/")) return path;
        final int splitAt = current.lastIndexOf('/');
        if (splitAt >= 0) return current.substring(0, splitAt) + "/" + path;
        return path;
    }

    public void refreshPage() {
        Iterable<String> content = li.cil.oc.api.Manual.contentFor(li.cil.oc.client.Manual.history.peek().path);
        if (content == null) {
            content = Collections.singletonList("Document not found: " + li.cil.oc.client.Manual.history.peek().path);
        }
        document = Document.parse(content);
        documentHeight = Document.height(document, documentMaxWidth, font);
        scrollTo(offset());
    }

    public void pushPage(String path) {
        if (!path.equals(li.cil.oc.client.Manual.history.peek().path)) {
            li.cil.oc.client.Manual.history.push(new li.cil.oc.client.Manual.History(path));
            refreshPage();
        }
    }

    public void popPage() {
        if (li.cil.oc.client.Manual.history.size() > 1) {
            li.cil.oc.client.Manual.history.pop();
            refreshPage();
        } else {
            onClose();
        }
    }

    @Override
    protected void init() {
        super.init();
        minecraft.mouseHandler.releaseMouse();
        KeyMapping.releaseAll();

        tabButtons.clear();
        final List<li.cil.oc.client.Manual.Tab> tabs = li.cil.oc.client.Manual.tabs;
        for (int i = 0; i < tabs.size() && i < maxTabsPerSide; i++) {
            final li.cil.oc.client.Manual.Tab tab = tabs.get(i);
            final int x = leftPos + tabPosX;
            final int y = topPos + tabPosY + i * (tabHeight - 1);
            final ImageButton button = new ImageButton(x, y, tabWidth, tabHeight,
                    b -> li.cil.oc.api.Manual.navigate(tab.path), Textures.GUI.ManualTab);
            tabButtons.add(button);
            addRenderableWidget(button);
        }

        scrollButton = new ImageButton(leftPos + scrollPosX, topPos + scrollPosY, 6, 13, b -> {
        }, Textures.GUI.ButtonScroll);
        addRenderableWidget(scrollButton);

        refreshPage();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        super.render(graphics, mouseX, mouseY, dt);

        scrollButton.active = canScroll();
        scrollButton.hoverOverride = isScrolling;

        final List<li.cil.oc.client.Manual.Tab> tabs = li.cil.oc.client.Manual.tabs;
        for (int i = 0; i < tabs.size() && i < maxTabsPerSide && i < tabButtons.size(); i++) {
            final ImageButton button = tabButtons.get(i);
            graphics.pose().pushPose();
            graphics.pose().translate(button.getX() + 5, button.getY() + 5, 0);
            tabs.get(i).renderer.render(graphics);
            graphics.pose().popPose();
        }

        currentSegment = Document.render(graphics, document, leftPos + 8, topPos + 8, documentMaxWidth, documentMaxHeight, offset(), font, mouseX, mouseY);

        if (!isScrolling && currentSegment.isPresent()) {
            final Optional<String> tooltip = currentSegment.get().tooltip();
            if (tooltip.isPresent() && !tooltip.get().isEmpty()) {
                graphics.renderComponentTooltip(font, localizeAndWrap(tooltip.get()), mouseX, mouseY);
            }
        }

        if (!isScrolling) {
            for (int i = 0; i < tabs.size() && i < maxTabsPerSide && i < tabButtons.size(); i++) {
                final ImageButton button = tabButtons.get(i);
                if (mouseX > button.getX() && mouseX < button.getX() + tabWidth && mouseY > button.getY() && mouseY < button.getY() + tabHeight) {
                    tabs.get(i).tooltip.ifPresent(text -> graphics.renderComponentTooltip(font, localizeAndWrap(text), mouseX, mouseY));
                }
            }
        }

        if (canScroll() && (isCoordinateOverScrollBar(mouseX - leftPos, mouseY - topPos) || isScrolling)) {
            final List<Component> lines = Collections.singletonList(Component.literal((100 * offset() / maxOffset()) + "%"));
            graphics.renderComponentTooltip(font, lines, leftPos + scrollPosX + scrollWidth, scrollButton.getY() + scrollButton.getHeight() + 1);
        }
    }

    private static List<Component> localizeAndWrap(String text) {
        final List<Component> lines = new ArrayList<>();
        Localization.localizeImmediately(text).lines().forEach(line -> lines.add(Component.literal(line)));
        return lines;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int mods) {
        final InputConstants.Key input = InputConstants.getKey(keyCode, scanCode);
        if (KeyBindings.isActiveAndMatches(minecraft.options.keyJump, input)) {
            popPage();
            return true;
        }
        if (KeyBindings.isActiveAndMatches(minecraft.options.keyInventory, input)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, mods);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        if (scroll < 0) scrollDown();
        else scrollUp();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        final int mcx = (int) mouseX - leftPos;
        final int mcy = (int) mouseY - topPos;
        if (canScroll() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isCoordinateOverScrollBar(mcx, mcy)) {
            isScrolling = true;
            scrollMouse(mouseY);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isCoordinateOverContent(mcx, mcy)) {
            if (currentSegment.isPresent() && currentSegment.get().onMouseClick((int) mouseX, (int) mouseY)) {
                return true;
            }
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            popPage();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (isScrolling) scrollMouse(mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isScrolling) {
            scrollMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isScrolling) {
            isScrolling = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void scrollMouse(double mouseY) {
        scrollTo((int) Math.round((mouseY - topPos - scrollPosY - 6.5) * maxOffset() / (scrollHeight - 13.0)));
    }

    private void scrollUp() {
        scrollTo(offset() - Document.lineHeight(font) * 3);
    }

    private void scrollDown() {
        scrollTo(offset() + Document.lineHeight(font) * 3);
    }

    private void scrollTo(int row) {
        li.cil.oc.client.Manual.history.peek().offset = Math.max(0, Math.min(maxOffset(), row));
        final int yMin = topPos + scrollPosY;
        if (maxOffset() > 0) {
            scrollButton.setY(yMin + (scrollHeight - 13) * offset() / maxOffset());
        } else {
            scrollButton.setY(yMin);
        }
    }

    private boolean isCoordinateOverContent(int x, int y) {
        return x >= 8 && x < 8 + documentMaxWidth &&
                y >= 8 && y < 8 + documentMaxHeight;
    }

    private boolean isCoordinateOverScrollBar(int x, int y) {
        return x >= scrollPosX && x < scrollPosX + scrollWidth &&
                y >= scrollPosY && y < scrollPosY + scrollHeight;
    }
}
