package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.Localization;
import li.cil.oc.client.PacketSender;
import li.cil.oc.client.Textures;
import li.cil.oc.util.RenderState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Rack extends DynamicGuiContainer<li.cil.oc.common.container.Rack> {
    // (u, v, w, h)
    public static final int[] busMasterBlankUVs = {195, 14, 3, 5};
    public static final int[] busMasterPresentUVs = {194, 20, 5, 5};
    public static final int[] busSlaveBlankUVs = {195, 1, 3, 4};
    public static final int[] busSlavePresentUVs = {194, 6, 5, 4};

    public static final int[] connectorMasterUVs = {194, 26, 1, 3};
    public static final int[] connectorSlaveUVs = {194, 11, 1, 2};

    // (w, h)
    public static final int[] hoverMasterSize = {3, 3};
    public static final int[] hoverSlaveSize = {3, 2};

    public static final int[][] wireMasterUVs = {
            {186, 16, 6, 3},
            {186, 20, 6, 3},
            {186, 24, 6, 3},
            {186, 28, 6, 3},
            {186, 32, 6, 3}
    };
    public static final int[][] wireSlaveUVs = {
            {186, 1, 6, 2},
            {186, 4, 6, 2},
            {186, 7, 6, 2},
            {186, 10, 6, 2},
            {186, 13, 6, 2}
    };

    // (x, y)
    public static final int[][] busStart = {
            {45, 22},
            {56, 22},
            {67, 22},
            {78, 22},
            {89, 22}
    };

    public static final int busGap = 3;

    public static final int[][] connectorStart = {
            {37, 23},
            {37, 43},
            {37, 63},
            {37, 83}
    };

    public static final int connectorGap = 2;

    public static final int[] relayModeUVs = {195, 30, 4, 2};

    public static final int[][] wireRelay = {
            {50, 104},
            {61, 104},
            {72, 104},
            {83, 104}
    };

    public static final Direction[] busToSide = Arrays.stream(Direction.values()).filter(d -> d != Direction.SOUTH).toArray(Direction[]::new);
    public static final Map<Direction, Integer> sideToBus = new EnumMap<>(Direction.class);

    static {
        for (int i = 0; i < busToSide.length; i++) sideToBus.put(busToSide[i], i);
    }

    public ImageButton relayButton;

    // bus -> mountable -> connectable
    public final ImageButton[][][] wireButtons;

    public Rack(li.cil.oc.common.container.Rack state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
        imageHeight = 210;
        wireButtons = new ImageButton[inventoryContainer.otherInventory.getContainerSize()][4][5];
    }

    public String sideName(Direction side) {
        switch (side) {
            case UP:
                return Localization.Rack.Top();
            case DOWN:
                return Localization.Rack.Bottom();
            case EAST:
                return Localization.Rack.Left();
            case WEST:
                return Localization.Rack.Right();
            case NORTH:
                return Localization.Rack.Back();
            default:
                return Localization.Rack.None();
        }
    }

    protected void onRackButton(int mountable, int connectable, int bus) {
        final Optional<Direction> current = inventoryContainer.nodeMapping[mountable][connectable];
        if (current.isPresent() && current.get() == busToSide[bus]) {
            PacketSender.sendRackMountableMapping(inventoryContainer, mountable, connectable, Optional.empty());
        } else {
            PacketSender.sendRackMountableMapping(inventoryContainer, mountable, connectable, Optional.of(busToSide[bus]));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        for (int bus = 0; bus < 5; bus++) {
            for (int mountable = 0; mountable < inventoryContainer.otherInventory.getContainerSize(); mountable++) {
                final boolean[] presence = inventoryContainer.nodePresence[mountable];
                for (int connectable = 0; connectable < 4; connectable++) {
                    wireButtons[mountable][connectable][bus].visible = presence[connectable];
                }
            }
        }
        final String relayMessage = inventoryContainer.isRelayEnabled ? Localization.Rack.RelayEnabled() : Localization.Rack.RelayDisabled();
        relayButton.setMessage(Component.literal(relayMessage));
        super.render(graphics, mouseX, mouseY, dt);
    }

    @Override
    protected void init() {
        super.init();

        relayButton = new ImageButton(leftPos + 101, topPos + 96, 65, 18,
                b -> PacketSender.sendRackRelayState(inventoryContainer, !inventoryContainer.isRelayEnabled),
                Textures.GUI.ButtonRelay, Component.literal(Localization.Rack.RelayDisabled()),
                false, 0xE0E0E0, 0xA0A0A0, 0xFFFFA0, 18);
        addRenderableWidget(relayButton);

        final int mw = hoverMasterSize[0];
        final int mh = hoverMasterSize[1];
        final int sw = hoverSlaveSize[0];
        final int sh = hoverSlaveSize[1];
        final int mbh = busMasterBlankUVs[3];
        final int sbh = busSlaveBlankUVs[3];
        for (int bus = 0; bus < 5; bus++) {
            for (int mountable = 0; mountable < inventoryContainer.otherInventory.getContainerSize(); mountable++) {
                final int offset = mountable * (mbh + sbh * 3 + busGap);
                final int bx = busStart[bus][0];
                final int by = busStart[bus][1];
                final int fBus = bus;
                final int fMountable = mountable;

                {
                    final ImageButton button = new ImageButton(leftPos + bx, topPos + by + offset + 1, mw, mh,
                            b -> onRackButton(fMountable, 0, fBus));
                    addRenderableWidget(button);
                    wireButtons[mountable][0][bus] = button;
                }

                for (int connectable = 0; connectable < 3; connectable++) {
                    final int fConnectable = connectable;
                    final ImageButton button = new ImageButton(leftPos + bx, topPos + by + offset + 1 + mbh + sbh * connectable, sw, sh,
                            b -> onRackButton(fMountable, fConnectable + 1, fBus));
                    addRenderableWidget(button);
                    wireButtons[mountable][connectable + 1][bus] = button;
                }
            }
        }
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(graphics, mouseX, mouseY);
        RenderState.pushAttrib(); // Prevents NEI render glitch.

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderState.makeItBlend();

        if (inventoryContainer.isRelayEnabled) {
            final int left = relayModeUVs[0], top = relayModeUVs[1], w = relayModeUVs[2], h = relayModeUVs[3];
            for (int[] xy : wireRelay) {
                drawRect(graphics, xy[0], xy[1], w, h, left, top);
            }
        }

        final int mcx = connectorMasterUVs[0], mcy = connectorMasterUVs[1], mcw = connectorMasterUVs[2], mch = connectorMasterUVs[3];
        final int mbx = busMasterBlankUVs[0], mby = busMasterBlankUVs[1], mbw = busMasterBlankUVs[2], mbh = busMasterBlankUVs[3];
        final int mpx = busMasterPresentUVs[0], mpy = busMasterPresentUVs[1], mpw = busMasterPresentUVs[2], mph = busMasterPresentUVs[3];
        final int scx = connectorSlaveUVs[0], scy = connectorSlaveUVs[1], scw = connectorSlaveUVs[2], sch = connectorSlaveUVs[3];
        final int sbx = busSlaveBlankUVs[0], sby = busSlaveBlankUVs[1], sbw = busSlaveBlankUVs[2], sbh = busSlaveBlankUVs[3];
        final int spx = busSlavePresentUVs[0], spy = busSlavePresentUVs[1], spw = busSlavePresentUVs[2], sph = busSlavePresentUVs[3];
        for (int mountable = 0; mountable < inventoryContainer.otherInventory.getContainerSize(); mountable++) {
            final boolean[] presence = inventoryContainer.nodePresence[mountable];

            // Draw connectable indicators next to item slots.
            final int cx = connectorStart[mountable][0];
            final int cy = connectorStart[mountable][1];
            if (presence[0]) {
                drawRect(graphics, cx, cy, mcw, mch, mcx, mcy);
                final Optional<Direction> masterSide = inventoryContainer.nodeMapping[mountable][0];
                if (masterSide.isPresent()) {
                    final int bus = sideToBus.get(masterSide.get());
                    final int[] wire = wireMasterUVs[bus];
                    final int mwx = wire[0], mwy = wire[1], mww = wire[2], mwh = wire[3];
                    for (int i = 0; i <= bus; i++) {
                        final int xOffset = mcw + i * (mpw + mww);
                        drawRect(graphics, cx + xOffset, cy, mww, mwh, mwx, mwy);
                    }
                }
                for (int connectable = 1; connectable < 4; connectable++) {
                    final Optional<Direction> side = inventoryContainer.nodeMapping[mountable][connectable];
                    if (side.isPresent()) {
                        final int bus = sideToBus.get(side.get());
                        final int[] wire = wireSlaveUVs[bus];
                        final int swx = wire[0], swy = wire[1], sww = wire[2], swh = wire[3];
                        final int yOffset = (mch + connectorGap) + (sch + connectorGap) * (connectable - 1);
                        for (int i = 0; i <= bus; i++) {
                            final int xOffset = scw + i * (spw + sww);
                            drawRect(graphics, cx + xOffset, cy + yOffset, sww, swh, swx, swy);
                        }
                    }
                }
            }
            for (int connectable = 1; connectable < 4; connectable++) {
                if (presence[connectable]) {
                    final int yOffset = (mch + connectorGap) + (sch + connectorGap) * (connectable - 1);
                    drawRect(graphics, cx, cy + yOffset, scw, sch, scx, scy);
                }
            }

            // Draw connection points on buses.
            final int yOffset = mountable * (mbh + sbh * 3 + busGap);
            for (int bus = 0; bus < 5; bus++) {
                final int bx = busStart[bus][0];
                final int by = busStart[bus][1];
                if (presence[0]) {
                    drawRect(graphics, bx - 1, by + yOffset, mpw, mph, mpx, mpy);
                } else {
                    drawRect(graphics, bx, by + yOffset, mbw, mbh, mbx, mby);
                }
                for (int connectable = 0; connectable < 3; connectable++) {
                    if (presence[connectable + 1]) {
                        drawRect(graphics, bx - 1, by + yOffset + mph + sph * connectable, spw, sph, spx, spy);
                    } else {
                        drawRect(graphics, bx, by + yOffset + mbh + sbh * connectable, sbw, sbh, sbx, sby);
                    }
                }
            }
        }

        for (int bus = 0; bus < 5; bus++) {
            final int x = 122;
            final int y = 20 + bus * 11;

            graphics.drawString(font,
                    Localization.localizeImmediately(sideName(busToSide[bus])),
                    x, y, 0x404040, false);
        }

        if (mouseX >= leftPos + 122 && mouseY >= topPos + 20 && mouseX < leftPos + 158 && mouseY < topPos + 20 + 5 * 11) {
            final List<String> tooltip = new ArrayList<>(Localization.Rack.OrientationTooltip().lines().toList());
            copiedDrawHoveringText(graphics, tooltip, mouseX - leftPos, mouseY - topPos, font);
        }

        if (relayButton.isMouseOver(mouseX, mouseY)) {
            final List<String> tooltip = new ArrayList<>(Localization.Rack.RelayModeTooltip().lines().toList());
            copiedDrawHoveringText(graphics, tooltip, mouseX - leftPos, mouseY - topPos, font);
        }

        RenderState.popAttrib();
    }

    @Override
    protected void drawSecondaryBackgroundLayer(GuiGraphics graphics) {
        RenderSystem.setShaderColor(1, 1, 1, 1); // Required under Linux.
        graphics.blit(Textures.GUI.Rack, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    private void drawRect(GuiGraphics graphics, int x, int y, int w, int h, int u, int v) {
        final float u0 = u / 256f;
        final float v0 = v / 256f;
        final float u1 = u0 + w / 256f;
        final float v1 = v0 + h / 256f;
        GuiUtil.texturedQuad(graphics, Textures.GUI.Rack, x, y, x + w, y + h, windowZ(), u0, u1, v0, v1);
    }
}
