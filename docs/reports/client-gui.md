# client gui + client top-level — wave 2 report

- Rule-2 singletons: client PacketHandler.INSTANCE, ComponentTracker.INSTANCE, Manual.INSTANCE. Manual statics: tabs, history (Deque<History>; peek()), makeRelative, History(path, offset), Tab, LanguageKey. LinkSegment → Manual.makeRelative(url, Manual.history.peek().path), Manual.INSTANCE.navigate(...).
- Static: Textures, KeyBindings, PacketSender, Sound (startLoop overloads), ColorHandler.
- Textures.GUI/Font/Icons/Model.X = full texture paths; Textures.Item/Block.X atlas sprite names; Textures.getSprite(loc); Textures.bind(loc) → RenderSystem.setShaderTexture.
- KeyBindings static fields; getBoundKey(km), isActiveAndMatches(km, Key).
- DisplayBuffer/InputBuffer stateful interfaces (InputBuffer.State); Window abstract class extends Screen; LockedHotbar.isSlotClickAllowed(Slot); WidgetContainer.widgets().
- gui.Screen(TextBuffer, boolean, Supplier<Boolean>, Supplier<Boolean>); gui.Drive(Inventory, Supplier<ItemStack>).
- Networking: client.PacketHandler.registerClientReceiver() → common.PacketHandler.registerClientReceiver(INSTANCE).
- Proxy: preInit sets api.API.manual; initClient: super.initClient(), Audio.init(), Sound.init(), S2C receiver, KeyMappingRegistry, ColorHandlerRegistry, ClientRenderers.register(); CLIENT_SETUP: CreativeTab.instance, GuiTypes.register() screens, cutout render types.

## Assumptions to verify
1. ClientRenderers.register() exists, safe during mod construction; registers HighlightRenderer, PetRenderer, MFUTargetRenderer, WirelessNetworkDebugRenderer, HologramRenderer, TextBufferRenderCache ticking.
2. NanomachinesHandler.Client, RackMountableRenderHandler, common.component.TextBuffer client handlers: owners register them (NOT client proxy).
3. BufferRenderer.drawBackground(PoseStack,int,int[,boolean]), drawText(PoseStack, TextBuffer), static margin/innerMargin; Document.render(GuiGraphics, Segment, x, y, maxW, maxH, yOffset, Font, mouseX, mouseY) → Optional<InteractiveSegment>; Document.parse(Iterable<String>), height(Segment,int,Font), lineHeight(Font); InteractiveSegment.tooltip() → Optional<String>, onMouseClick(int,int).
4. ContainerTypes.X are RegistrySupplier<MenuType<X>>. Robot container: info.screenBuffer (Optional), info.hasKeyboard, info.mainInvSize, generateSlotsFor(int), selectedSlot(), globalBuffer(). Drone: statusText(), selectedSlot(). Assembler: isAssembling(), assemblyProgress(), assemblyRemainingTime(). AssemblerTemplates.select(stack) → Optional<Template>; Template.validate(Container) → Triple<Boolean, Component, Component[]>.
5. Hologram fields volume,width,needsRendering,hasPower,scale,translation,rotation*, colors(); Robot info.components, info.lightColor, selectedSlot field, setAnimateSwing/setAnimateTurn/move; Rack.lastData, Raid.presence, Printer/Assembler.requiredEnergy, Relay.lastMessage, Screen.invertTouchMode, Charger.chargeSpeed/hasPower, Disassembler.isActive; Tablet.get(stack, player) → TabletWrapper (data.isRunning, isDirty); ControllerImpl.loadData/configuration.triggers/activeBehaviorsDirty/storedEnergy; PetRenderer.hidden/isInitialized; Loot.disksForClient/disksForCyclingClient; ClientGpuTextBufferHandler statics; common.component.TextBuffer.data, .proxy.setChanged(), markInitialized().

## TODO(port)
JEI integration; key conflict contexts (Shift/Ctrl also polled from GLFW); Manual uses setScreen; sound cleanup on CLIENT_PLAYER_QUIT only; registerModel dropped.
