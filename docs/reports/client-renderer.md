# client renderer — wave 2 report

- Entry: client.renderer.ClientRenderers.register() (model hooks, world render callbacks, tick handlers, drone renderer, Drone.customRenderer, HoverBoots.armorModel); BERs + Fabric item/armor renderers on CLIENT_SETUP (registerBlockEntityRenderers(), registerItemRenderers()).
- Hook client.platform.RenderPlatform: registerModelHooks()/getModel(id) (Forge ModelEvent + ForgeSmartBakedModel w/ ModelData; Fabric ModelLoadingPlugin + FabricSmartBakedModel), registerLevelRenderer(cb), registerBlockHighlightRenderer(cb), registerItemRenderer/registerArmorModel (Fabric only).
- SmartBlockModelBase vanilla BakedModel: getBlockQuads(state, side, rand, @Nullable BlockEntity); StackOverrides. ModelInitialization.modifyBakedModel swaps cable/netsplitter/print/robot/robotafterimage/screen1-3/rack + CustomModel items.
- Text: RenderCache VertexBuffers; DynamicFontRenderer DynamicTexture glyph atlas; TextBufferRenderData dirty()/setDirty()/data()/viewport().
- RenderTypes built from shards; BLOCK_OVERLAY_COLOR; UPGRADE_* entitySolid; MFU_LINES lines shader.
- AW: ItemOverrides <init>()V.
- Depends on tiles: Screen.getRenderBoundingBox(), Hologram.getViewDistance()/getFadeStartDistanceSquared(), Robot.componentSlots()/containerSlots() → Iterable<Integer>.
- Pets drawn in world space (no RenderPlayerEvent on Fabric).
- Done: Forge hover boots copyPropertiesTo.
