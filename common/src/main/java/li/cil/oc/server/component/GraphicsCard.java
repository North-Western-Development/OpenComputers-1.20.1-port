package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.component.GpuTextBuffer;
import li.cil.oc.common.component.traits.VideoRamDevice;
import li.cil.oc.common.component.traits.VideoRamRasterizer;
import li.cil.oc.server.machine.Machine;
import li.cil.oc.util.PackedColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static li.cil.oc.util.ResultWrapper.result;

// IMPORTANT: usually methods with side effects should *not* be direct
// callbacks to avoid the massive headache synchronizing them ensues, in
// particular when it comes to world saving. I'm making an exception for
// screens, though since they'd be painfully sluggish otherwise. This also
// means we have to use a somewhat nasty trick in common.component.Buffer's
// save function: we wait for all computers in the same network to finish
// their current execution and then pause them, to ensure the state of the
// buffer is "clean", meaning the computer has the correct state when it is
// saved in turn. If we didn't, a computer might change a screen after it was
// saved, but before the computer was saved, leading to mismatching states in
// the save file - a Bad Thing (TM).

public class GraphicsCard extends AbstractManagedEnvironment implements DeviceInfo, VideoRamDevice {
    private static final int RESERVED_SCREEN_INDEX = 0;

    public final int tier;

    public final ComponentConnector node;

    // State of the VideoRamDevice trait.
    private final Map<Integer, GpuTextBuffer> internalBuffers = new HashMap<>();

    private final Pair<Integer, Integer> maxResolution;

    private final TextBuffer.ColorDepth maxDepth;

    private Optional<String> screenAddress = Optional.empty();

    private Optional<TextBuffer> screenInstance = Optional.empty();

    private int bufferIndex = RESERVED_SCREEN_INDEX; // screen is index zero

    public final double[] setBackgroundCosts = {1.0 / 32, 1.0 / 64, 1.0 / 128};
    public final double[] setForegroundCosts = {1.0 / 32, 1.0 / 64, 1.0 / 128};
    public final double[] setPaletteColorCosts = {1.0 / 2, 1.0 / 8, 1.0 / 16};
    public final double[] setCosts = {1.0 / 64, 1.0 / 128, 1.0 / 256};
    public final double[] copyCosts = {1.0 / 16, 1.0 / 32, 1.0 / 64};
    public final double[] fillCosts = {1.0 / 32, 1.0 / 64, 1.0 / 128};
    // These are dirty page bitblt budget costs
    // a single bitblt can send a screen of data, which is n*set calls where set is writing an entire line
    // So for each tier, we multiple the set cost with the number of lines the screen may have
    public final double bitbltCost;
    public final double totalVRAM;

    public boolean budgetExhausted = false; // for especially expensive calls, bitblt

    public GraphicsCard(int tier) {
        this.tier = tier;
        this.node = (ComponentConnector) Network.newNode(this, Visibility.Neighbors).
                withComponent("gpu").
                withConnector().
                create();
        setNode(node);
        this.maxResolution = Settings.screenResolutionsByTier[tier];
        this.maxDepth = Settings.screenDepthsByTier[tier];
        this.bitbltCost = Settings.get().bitbltCost * Math.pow(2, tier);
        this.totalVRAM = (maxResolution.getLeft() * maxResolution.getRight()) * Settings.get().vramSizes[Math.min(Math.max(0, tier), 2)];
    }

    @Override
    public ComponentConnector node() {
        return node;
    }

    @Override
    public Map<Integer, GpuTextBuffer> internalBuffers() {
        return internalBuffers;
    }

    private Object[] screen(int index, Function<TextBuffer, Object[]> f) {
        if (index == RESERVED_SCREEN_INDEX) {
            if (screenInstance.isPresent()) {
                final TextBuffer screen = screenInstance.get();
                synchronized (screen) {
                    return f.apply(screen);
                }
            } else return new Object[]{null, "no screen"};
        } else {
            final Optional<GpuTextBuffer> buffer = getBuffer(index);
            if (buffer.isPresent()) return f.apply(buffer.get());
            else return new Object[]{null, "invalid buffer index"};
        }
    }

    private Object[] screen(Function<TextBuffer, Object[]> f) {
        return screen(bufferIndex, f);
    }

    // ----------------------------------------------------------------------- //

    private Map<String, String> deviceInfo;

    public String capacityInfo() {
        return Integer.toString(maxResolution.getLeft() * maxResolution.getRight());
    }

    public String widthInfo() {
        return new String[]{"1", "4", "8"}[maxDepth.ordinal()];
    }

    public String clockInfo() {
        return ((int) (2000 / setBackgroundCosts[tier]) / 100) + "/" + ((int) (2000 / setForegroundCosts[tier]) / 100) + "/" + ((int) (2000 / setPaletteColorCosts[tier]) / 100) + "/" + ((int) (2000 / setCosts[tier]) / 100) + "/" + ((int) (2000 / copyCosts[tier]) / 100) + "/" + ((int) (2000 / fillCosts[tier]) / 100);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            final Map<String, String> info = new HashMap<>();
            info.put(DeviceAttribute.Class, DeviceClass.Display);
            info.put(DeviceAttribute.Description, "Graphics controller");
            info.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            info.put(DeviceAttribute.Product, "MPG" + ((tier + 1) * 1000) + " GTZ");
            info.put(DeviceAttribute.Capacity, capacityInfo());
            info.put(DeviceAttribute.Width, widthInfo());
            info.put(DeviceAttribute.Clock, clockInfo());
            deviceInfo = info;
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    private boolean resolveInvokeCosts(int idx, Context context, double budgetCost, int units, double factor) {
        if (idx == RESERVED_SCREEN_INDEX) {
            context.consumeCallBudget(budgetCost);
            return consumePower(units, factor);
        }
        return true;
    }

    @Callback(direct = true, doc = "function(): number -- returns the index of the currently selected buffer. 0 is reserved for the screen. Can return 0 even when there is no screen")
    public Object[] getActiveBuffer(Context context, Arguments args) {
        return result(bufferIndex);
    }

    @Callback(direct = true, doc = "function(index: number): number -- Sets the active buffer to `index`. 1 is the first vram buffer and 0 is reserved for the screen. returns nil for invalid index (0 is always valid)")
    public Object[] setActiveBuffer(Context context, Arguments args) {
        final int previousIndex = bufferIndex;
        final int newIndex = args.checkInteger(0);
        if (newIndex != RESERVED_SCREEN_INDEX && getBuffer(newIndex).isEmpty()) {
            return result(null, "invalid buffer index");
        } else {
            bufferIndex = newIndex;
            if (bufferIndex == RESERVED_SCREEN_INDEX) {
                screen(s -> result(true));
            }
            return result(previousIndex);
        }
    }

    @Callback(direct = true, doc = "function(): number -- Returns an array of indexes of the allocated buffers")
    public Object[] buffers(Context context, Arguments args) {
        return result((Object) bufferIndexes());
    }

    @Callback(direct = true, doc = "function([width: number, height: number]): number -- allocates a new buffer with dimensions width*height (defaults to max resolution) and appends it to the buffer list. Returns the index of the new buffer and returns nil with an error message on failure. A buffer can be allocated even when there is no screen bound to this gpu. Index 0 is always reserved for the screen and thus the lowest index of an allocated buffer is always 1.")
    public Object[] allocateBuffer(Context context, Arguments args) {
        final int width = args.optInteger(0, maxResolution.getLeft());
        final int height = args.optInteger(1, maxResolution.getRight());
        final int size = width * height;
        if (width <= 0 || height <= 0) {
            return result(null, "invalid page dimensions: must be greater than zero");
        } else if (size > (totalVRAM - calculateUsedMemory())) {
            return result(null, "not enough video memory");
        } else if (node == null) {
            return result(null, "graphics card appears disconnected");
        } else {
            final PackedColor.ColorFormat format = PackedColor.Depth.format(Settings.screenDepthsByTier[tier]);
            final li.cil.oc.util.TextBuffer buffer = new li.cil.oc.util.TextBuffer(width, height, format);
            final GpuTextBuffer page = GpuTextBuffer.wrap(node.address(), nextAvailableBufferIndex(), buffer);
            addBuffer(page);
            return result(page.id);
        }
    }

    // this event occurs when the gpu is told a page was removed - we need to notify the screen of this
    // we do this because the VideoRamDevice trait only notifies itself, it doesn't assume there is a screen
    @Override
    public void onBufferRamDestroy(int id) {
        // first protect our buffer index - it needs to fall back to the screen if its buffer was removed
        if (id != RESERVED_SCREEN_INDEX) {
            screen(RESERVED_SCREEN_INDEX, s -> {
                if (s instanceof VideoRamRasterizer oc) return result(oc.removeBuffer(node.address(), id));
                else return result(true); // addon mod screen type that is not video ram aware
            });
        }
        if (id == bufferIndex) {
            bufferIndex = RESERVED_SCREEN_INDEX;
        }
    }

    @Callback(direct = true, doc = "function(index: number): boolean -- Closes buffer at `index`. Returns true if a buffer closed. If the current buffer is closed, index moves to 0")
    public Object[] freeBuffer(Context context, Arguments args) {
        final int index = args.optInteger(0, bufferIndex);
        if (removeBuffers(new int[]{index}) == 1) return result(true);
        else return result(null, "no buffer at index");
    }

    @Callback(direct = true, doc = "function(): number -- Closes all buffers and returns the count. If the active buffer is closed, index moves to 0")
    public Object[] freeAllBuffers(Context context, Arguments args) {
        return result(removeAllBuffers());
    }

    @Callback(direct = true, doc = "function(): number -- returns the total memory size of the gpu vram. This does not include the screen.")
    public Object[] totalMemory(Context context, Arguments args) {
        return result(totalVRAM);
    }

    @Callback(direct = true, doc = "function(): number -- returns the total free memory not allocated to buffers. This does not include the screen.")
    public Object[] freeMemory(Context context, Arguments args) {
        return result(totalVRAM - calculateUsedMemory());
    }

    @Callback(direct = true, doc = "function(index: number): number, number -- returns the buffer size at index. Returns the screen resolution for index 0. returns nil for invalid indexes")
    public Object[] getBufferSize(Context context, Arguments args) {
        final int idx = args.optInteger(0, bufferIndex);
        return screen(idx, s -> result(s.getWidth(), s.getHeight()));
    }

    private double determineBitbltBudgetCost(TextBuffer dst, TextBuffer src) {
        // large dirty buffers need throttling so their budget cost is more
        // clean buffers have no budget cost.
        if (src instanceof GpuTextBuffer page) {
            if (dst instanceof GpuTextBuffer) return 0.0; // no cost to write to ram
            else if (page.dirty) { // screen target will need the new buffer
                // small buffers are cheap, so increase with size of buffer source
                return bitbltCost * (src.getWidth() * src.getHeight()) / (maxResolution.getLeft() * maxResolution.getRight());
            } else return .001; // bitblt a clean page to screen has a minimal cost
        }
        return 0.0; // from screen is free
    }

    private double determineBitbltEnergyCost(TextBuffer dst) {
        // memory to memory copies are extremely energy efficient
        // rasterizing to the screen has the same cost as copy (in fact, screen-to-screen blt _is_ a copy
        if (dst instanceof GpuTextBuffer) return 0;
        else return Settings.get().gpuCopyCost / 15;
    }

    @Callback(direct = true, doc = "function([dst: number, col: number, row: number, width: number, height: number, src: number, fromCol: number, fromRow: number]):boolean -- bitblt from buffer to screen. All parameters are optional. Writes to `dst` page in rectangle `x, y, width, height`, defaults to the bound screen and its viewport. Reads data from `src` page at `fx, fy`, default is the active page from position 1, 1")
    public Object[] bitblt(Context context, Arguments args) {
        final int dstIdx = args.optInteger(0, RESERVED_SCREEN_INDEX);
        return screen(dstIdx, dst -> {
            final int col = args.optInteger(1, 1);
            final int row = args.optInteger(2, 1);
            final int w = args.optInteger(3, dst.getWidth());
            final int h = args.optInteger(4, dst.getHeight());
            final int srcIdx = args.optInteger(5, bufferIndex);
            return screen(srcIdx, src -> {
                final int fromCol = args.optInteger(6, 1);
                final int fromRow = args.optInteger(7, 1);

                double budgetCost = determineBitbltBudgetCost(dst, src);
                final double energyCost = determineBitbltEnergyCost(dst);
                final double tierCredit = ((tier + 1) * .5);
                final double overBudget = budgetCost - tierCredit;

                if (overBudget > 0) {
                    if (budgetExhausted) { // we've thrown once before
                        if (overBudget > tierCredit) { // we need even more pause than just a single tierCredit
                            final double pauseNeeded = overBudget - tierCredit;
                            final double seconds = (pauseNeeded / tierCredit) / 20;
                            context.pause(seconds);
                        }
                        budgetCost = 0; // remove the rest of the budget cost at this point
                    } else {
                        budgetExhausted = true;
                        throw sneakyThrow(new LimitReachedException());
                    }
                }
                budgetExhausted = false;

                if (resolveInvokeCosts(dstIdx, context, budgetCost, w * h, energyCost)) {
                    if (dstIdx == srcIdx) {
                        final int tx = col - fromCol;
                        final int ty = row - fromRow;
                        dst.copy(fromCol - 1, fromRow - 1, w, h, tx, ty);
                        return result(true);
                    } else {
                        // at least one of the two buffers is a gpu buffer
                        GpuTextBuffer.bitblt(dst, col, row, w, h, src, fromRow, fromCol);
                        return result(true);
                    }
                } else return result(null, "not enough energy");
            });
        });
    }

    @Callback(doc = "function(address:string[, reset:boolean=true]):boolean -- Binds the GPU to the screen with the specified address and resets screen settings if `reset` is true.")
    public Object[] bind(Context context, Arguments args) {
        final String address = args.checkString(0);
        final boolean reset = args.optBoolean(1, true);
        final Node other = node.network().node(address);
        if (other == null) {
            return result(null, "invalid address");
        } else if (other.host() instanceof TextBuffer buffer) {
            screenAddress = Optional.of(address);
            screenInstance = Optional.of(buffer);
            return screen(s -> {
                if (reset) {
                    resetScreen(s);
                    s.setBackgroundColor(0x000000);
                    if (s instanceof VideoRamRasterizer oc) oc.removeAllBuffers();
                } else context.pause(0); // To discourage outputting "in realtime" to multiple screens using one GPU.
                return result(true);
            });
        } else return result(null, "not a screen");
    }

    private void resetScreen(TextBuffer s) {
        final int smw = s.getMaximumWidth();
        final int smh = s.getMaximumHeight();
        s.setResolution(Math.min(maxResolution.getLeft(), smw), Math.min(maxResolution.getRight(), smh));
        s.setColorDepth(TextBuffer.ColorDepth.values()[Math.min(maxDepth.ordinal(), s.getMaximumColorDepth().ordinal())]);
        s.setForegroundColor(0xFFFFFF);
    }

    @Callback(direct = true, doc = "function():string -- Get the address of the screen the GPU is currently bound to.")
    public Object[] getScreen(Context context, Arguments args) {
        return screen(RESERVED_SCREEN_INDEX, s -> result(s.node().address()));
    }

    @Callback(direct = true, doc = "function():number, boolean -- Get the current background color and whether it's from the palette or not.")
    public Object[] getBackground(Context context, Arguments args) {
        return screen(s -> result(s.getBackgroundColor(), s.isBackgroundFromPalette()));
    }

    @Callback(direct = true, doc = "function(value:number[, palette:boolean]):number, number or nil -- Sets the background color to the specified value. Optionally takes an explicit palette index. Returns the old value and if it was from the palette its palette index.")
    public Object[] setBackground(Context context, Arguments args) {
        final int color = args.checkInteger(0);
        if (bufferIndex == RESERVED_SCREEN_INDEX) {
            context.consumeCallBudget(setBackgroundCosts[tier]);
        }
        return screen(s -> {
            final int oldValue = s.getBackgroundColor();
            final Object oldColor;
            final Object oldIndex;
            if (s.isBackgroundFromPalette()) {
                oldColor = s.getPaletteColor(oldValue);
                oldIndex = oldValue;
            } else {
                oldColor = oldValue;
                oldIndex = null;
            }
            s.setBackgroundColor(color, args.optBoolean(1, false));
            return result(oldColor, oldIndex);
        });
    }

    @Callback(direct = true, doc = "function():number, boolean -- Get the current foreground color and whether it's from the palette or not.")
    public Object[] getForeground(Context context, Arguments args) {
        return screen(s -> result(s.getForegroundColor(), s.isForegroundFromPalette()));
    }

    @Callback(direct = true, doc = "function(value:number[, palette:boolean]):number, number or nil -- Sets the foreground color to the specified value. Optionally takes an explicit palette index. Returns the old value and if it was from the palette its palette index.")
    public Object[] setForeground(Context context, Arguments args) {
        final int color = args.checkInteger(0);
        if (bufferIndex == RESERVED_SCREEN_INDEX) {
            context.consumeCallBudget(setForegroundCosts[tier]);
        }
        return screen(s -> {
            final int oldValue = s.getForegroundColor();
            final Object oldColor;
            final Object oldIndex;
            if (s.isForegroundFromPalette()) {
                oldColor = s.getPaletteColor(oldValue);
                oldIndex = oldValue;
            } else {
                oldColor = oldValue;
                oldIndex = null;
            }
            s.setForegroundColor(color, args.optBoolean(1, false));
            return result(oldColor, oldIndex);
        });
    }

    @Callback(direct = true, doc = "function(index:number):number -- Get the palette color at the specified palette index.")
    public Object[] getPaletteColor(Context context, Arguments args) {
        final int index = args.checkInteger(0);
        return screen(s -> {
            try {
                return result(s.getPaletteColor(index));
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("invalid palette index");
            }
        });
    }

    @Callback(direct = true, doc = "function(index:number, color:number):number -- Set the palette color at the specified palette index. Returns the previous value.")
    public Object[] setPaletteColor(Context context, Arguments args) {
        final int index = args.checkInteger(0);
        final int color = args.checkInteger(1);
        if (bufferIndex == RESERVED_SCREEN_INDEX) {
            context.consumeCallBudget(setPaletteColorCosts[tier]);
            context.pause(0.1);
        }
        return screen(s -> {
            try {
                final int oldColor = s.getPaletteColor(index);
                s.setPaletteColor(index, color);
                return result(oldColor);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("invalid palette index");
            }
        });
    }

    @Callback(direct = true, doc = "function():number -- Returns the currently set color depth.")
    public Object[] getDepth(Context context, Arguments args) {
        return screen(s -> result(PackedColor.Depth.bits(s.getColorDepth())));
    }

    @Callback(doc = "function(depth:number):number -- Set the color depth. Returns the previous value.")
    public Object[] setDepth(Context context, Arguments args) {
        final int depth = args.checkInteger(0);
        return screen(s -> {
            final TextBuffer.ColorDepth oldDepth = s.getColorDepth();
            if (depth == 1) s.setColorDepth(TextBuffer.ColorDepth.OneBit);
            else if (depth == 4 && maxDepth.ordinal() >= TextBuffer.ColorDepth.FourBit.ordinal())
                s.setColorDepth(TextBuffer.ColorDepth.FourBit);
            else if (depth == 8 && maxDepth.ordinal() >= TextBuffer.ColorDepth.EightBit.ordinal())
                s.setColorDepth(TextBuffer.ColorDepth.EightBit);
            else throw new IllegalArgumentException("unsupported depth");
            // Note: the 1.16 code returned the enum value (not convertible to Lua); return the bit depth.
            return result(PackedColor.Depth.bits(oldDepth));
        });
    }

    @Callback(direct = true, doc = "function():number -- Get the maximum supported color depth.")
    public Object[] maxDepth(Context context, Arguments args) {
        return screen(s -> result(PackedColor.Depth.bits(TextBuffer.ColorDepth.values()[Math.min(maxDepth.ordinal(), s.getMaximumColorDepth().ordinal())])));
    }

    @Callback(direct = true, doc = "function():number, number -- Get the current screen resolution.")
    public Object[] getResolution(Context context, Arguments args) {
        return screen(s -> result(s.getWidth(), s.getHeight()));
    }

    @Callback(doc = "function(width:number, height:number):boolean -- Set the screen resolution. Returns true if the resolution changed.")
    public Object[] setResolution(Context context, Arguments args) {
        final int w = args.checkInteger(0);
        final int h = args.checkInteger(1);
        final int mw = maxResolution.getLeft();
        final int mh = maxResolution.getRight();
        // Even though the buffer itself checks this again, we need this here for
        // the minimum of screen and GPU resolution.
        if (w < 1 || h < 1 || w > mw || h > mw || h * w > mw * mh)
            throw new IllegalArgumentException("unsupported resolution");
        return screen(s -> result(s.setResolution(w, h)));
    }

    @Callback(direct = true, doc = "function():number, number -- Get the maximum screen resolution.")
    public Object[] maxResolution(Context context, Arguments args) {
        return screen(s -> {
            final int smw = s.getMaximumWidth();
            final int smh = s.getMaximumHeight();
            return result(Math.min(maxResolution.getLeft(), smw), Math.min(maxResolution.getRight(), smh));
        });
    }

    @Callback(direct = true, doc = "function():number, number -- Get the current viewport resolution.")
    public Object[] getViewport(Context context, Arguments args) {
        return screen(s -> result(s.getViewportWidth(), s.getViewportHeight()));
    }

    @Callback(doc = "function(width:number, height:number):boolean -- Set the viewport resolution. Cannot exceed the screen resolution. Returns true if the resolution changed.")
    public Object[] setViewport(Context context, Arguments args) {
        final int w = args.checkInteger(0);
        final int h = args.checkInteger(1);
        final int mw = maxResolution.getLeft();
        final int mh = maxResolution.getRight();
        // Even though the buffer itself checks this again, we need this here for
        // the minimum of screen and GPU resolution.
        if (w < 1 || h < 1 || w > mw || h > mw || h * w > mw * mh)
            throw new IllegalArgumentException("unsupported viewport size");
        return screen(s -> {
            if (w > s.getWidth() || h > s.getHeight())
                throw new IllegalArgumentException("unsupported viewport size");
            return result(s.setViewport(w, h));
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number):string, number, number, number or nil, number or nil -- Get the value displayed on the screen at the specified index, as well as the foreground and background color. If the foreground or background is from the palette, returns the palette indices as fourth and fifth results, else nil, respectively.")
    public Object[] get(Context context, Arguments args) {
        final int x = args.checkInteger(0) - 1;
        final int y = args.checkInteger(1) - 1;
        return screen(s -> {
            final int fgValue = s.getForegroundColor(x, y);
            final Object fgColor;
            final Object fgIndex;
            if (s.isForegroundFromPalette(x, y)) {
                fgColor = s.getPaletteColor(fgValue);
                fgIndex = fgValue;
            } else {
                fgColor = fgValue;
                fgIndex = null;
            }

            final int bgValue = s.getBackgroundColor(x, y);
            final Object bgColor;
            final Object bgIndex;
            if (s.isBackgroundFromPalette(x, y)) {
                bgColor = s.getPaletteColor(bgValue);
                bgIndex = bgValue;
            } else {
                bgColor = bgValue;
                bgIndex = null;
            }

            return result(s.get(x, y), fgColor, bgColor, fgIndex, bgIndex);
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, value:string[, vertical:boolean]):boolean -- Plots a string value to the screen at the specified position. Optionally writes the string vertically.")
    public Object[] set(Context context, Arguments args) {
        final int x = args.checkInteger(0) - 1;
        final int y = args.checkInteger(1) - 1;
        final String value = args.checkString(2);
        final boolean vertical = args.optBoolean(3, false);

        return screen(s -> {
            if (resolveInvokeCosts(bufferIndex, context, setCosts[tier], value.length(), Settings.get().gpuSetCost)) {
                s.set(x, y, value, vertical);
                return result(true);
            } else return result(null, "not enough energy");
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, width:number, height:number, tx:number, ty:number):boolean -- Copies a portion of the screen from the specified location with the specified size by the specified translation.")
    public Object[] copy(Context context, Arguments args) {
        final int x = args.checkInteger(0) - 1;
        final int y = args.checkInteger(1) - 1;
        final int w = Math.max(0, args.checkInteger(2));
        final int h = Math.max(0, args.checkInteger(3));
        final int tx = args.checkInteger(4);
        final int ty = args.checkInteger(5);
        return screen(s -> {
            if (resolveInvokeCosts(bufferIndex, context, copyCosts[tier], w * h, Settings.get().gpuCopyCost)) {
                s.copy(x, y, w, h, tx, ty);
                return result(true);
            } else return result(null, "not enough energy");
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, width:number, height:number, char:string):boolean -- Fills a portion of the screen at the specified position with the specified size with the specified character.")
    public Object[] fill(Context context, Arguments args) throws Exception {
        final int x = args.checkInteger(0) - 1;
        final int y = args.checkInteger(1) - 1;
        final int w = Math.max(0, args.checkInteger(2));
        final int h = Math.max(0, args.checkInteger(3));
        final String value = args.checkString(4);
        if (value.length() == 1) return screen(s -> {
            final char c = value.charAt(0);
            final double cost = c == ' ' ? Settings.get().gpuClearCost : Settings.get().gpuFillCost;
            if (resolveInvokeCosts(bufferIndex, context, fillCosts[tier], w * h, cost)) {
                s.fill(x, y, w, h, value.charAt(0));
                return result(true);
            } else {
                return result(null, "not enough energy");
            }
        });
        else throw new Exception("invalid fill value");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    private boolean consumePower(double n, double cost) {
        return node.tryChangeBuffer(-n * cost);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if (node.isNeighborOf(message.source())) {
            if ("computer.stopped".equals(message.name()) || "computer.started".equals(message.name())) {
                bufferIndex = RESERVED_SCREEN_INDEX;
                removeAllBuffers();
            }
        }

        if ("computer.stopped".equals(message.name()) && node.isNeighborOf(message.source())) {
            screen(s -> {
                resetScreen(s);
                final int w = s.getWidth();
                final int h = s.getHeight();
                if (message.source().host() instanceof Machine machine && machine.lastError() != null) {
                    if (s.getColorDepth().ordinal() > TextBuffer.ColorDepth.OneBit.ordinal()) s.setBackgroundColor(0x0000FF);
                    else s.setBackgroundColor(0x000000);
                    s.fill(0, 0, w, h, ' ');
                    try {
                        final Pattern wrapRegEx = Pattern.compile("(.{1," + Math.max(1, w - 2) + "})\\s");
                        final String text = Localization.localizeImmediately(machine.lastError()).replace("\t", "  ") + "\n";
                        final String[] lines = wrapRegEx.matcher(text).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + "\n")).lines().toArray(String[]::new);
                        final int firstRow = Math.max((h - lines.length) / 2, 2);

                        final String title = "Unrecoverable Error";
                        s.set((w - title.length()) / 2, firstRow - 2, title, false);

                        int maxLineLength = 0;
                        for (String line : lines) maxLineLength = Math.max(maxLineLength, line.length());
                        final int col = Math.max((w - maxLineLength) / 2, 0);
                        for (int idx = 0; idx < lines.length; idx++) {
                            final int row = firstRow + idx;
                            s.set(col, row, lines[idx], false);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                } else {
                    s.setBackgroundColor(0x000000);
                    s.fill(0, 0, w, h, ' ');
                }
                return null; // For screen()
            });
        }
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (screenInstance.isEmpty() && screenAddress.map(a -> a.equals(node.address())).orElse(false)) {
            if (node.host() instanceof TextBuffer buffer) {
                screenInstance = Optional.of(buffer);
            } // else: Not the screen node we're looking for.
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node || screenAddress.map(a -> a.equals(node.address())).orElse(false)) {
            screenAddress = Optional.empty();
            screenInstance = Optional.empty();
        }
    }

    // ----------------------------------------------------------------------- //

    private static final String SCREEN_KEY = "screen";
    private static final String BUFFER_INDEX_KEY = "bufferIndex";
    private static final String VIDEO_RAM_KEY = "videoRam";
    private static final String NBT_PAGES = "pages";
    private static final String NBT_PAGE_IDX = "page_idx";
    private static final String NBT_PAGE_DATA = "page_data";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);

        if (nbt.contains(SCREEN_KEY)) {
            final String screen = nbt.getString(SCREEN_KEY);
            screenAddress = screen.isEmpty() ? Optional.empty() : Optional.of(screen);
            screenInstance = Optional.empty();
        }

        if (nbt.contains(BUFFER_INDEX_KEY)) {
            bufferIndex = nbt.getInt(BUFFER_INDEX_KEY);
        }

        removeAllBuffers(); // JUST in case
        if (nbt.contains(VIDEO_RAM_KEY)) {
            final CompoundTag videoRamNbt = nbt.getCompound(VIDEO_RAM_KEY);
            final ListTag nbtPages = videoRamNbt.getList(NBT_PAGES, Tag.TAG_COMPOUND);
            for (int i = 0; i < nbtPages.size(); i++) {
                final CompoundTag nbtPage = nbtPages.getCompound(i);
                final int idx = nbtPage.getInt(NBT_PAGE_IDX);
                final CompoundTag data = nbtPage.getCompound(NBT_PAGE_DATA);
                loadBuffer(node.address(), idx, data);
            }
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);

        screenAddress.ifPresent(address -> nbt.putString(SCREEN_KEY, address));

        nbt.putInt(BUFFER_INDEX_KEY, bufferIndex);

        final CompoundTag videoRamNbt = new CompoundTag();
        final ListTag nbtPages = new ListTag();

        for (int idx : bufferIndexes()) {
            final Optional<GpuTextBuffer> page = getBuffer(idx);
            if (page.isPresent()) {
                final CompoundTag nbtPage = new CompoundTag();
                nbtPage.putInt(NBT_PAGE_IDX, idx);
                final CompoundTag data = new CompoundTag();
                page.get().data.saveData(data);
                nbtPage.put(NBT_PAGE_DATA, data);
                nbtPages.add(nbtPage);
            }
        }
        videoRamNbt.put(NBT_PAGES, nbtPages);
        nbt.put(VIDEO_RAM_KEY, videoRamNbt);
    }
}
