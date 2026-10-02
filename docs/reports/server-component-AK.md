# server/component A–K + traits — wave 2 report

- Traits are interfaces. Abstract members: WorldAware.position(); InventoryAware.fakePlayer(), inventory(), selectedSlot()/setSelectedSlot(int); TankAware.tank(), selectedTank()/setSelectedTank(int); NetworkAware.node(); SideRestricted.checkSideForAction(Arguments,int); InventoryTransfer.onTransferContents() → Optional<String>; WakeMessageAware state via wakeMessage()/setWakeMessage(Optional<String>), wakeMessageFuzzy()/setWakeMessageFuzzy(boolean).
- fakePlayer clash fixed. stackInSlot → ItemStack; insertionSlots() → List<Integer>; blockContent(side) → Pair<Boolean,String>; closestEntity → Optional; TankAware.capacityOf/amountOf statics. Call ExtendedArguments.checkSideForAction fully qualified.
- server.component.Agent interface; Robot/Drone implement; Agent.super.onWorldInteraction; record ClickParams; CapturedDrops record (drops picked up from world).
- No ComponentPackage: `import static li.cil.oc.util.ResultWrapper.result;`.
- New hook common.platform.ComponentPlatform: fakePlayer(ServerLevel, GameProfile), mayInteract(Player, BlockPos, Direction), canTossItem(ItemEntity, Player).
- Components: public `node` field + covariant node(). DebugCard.AccessContext final class (player, nonce, static remove/loadData); DebugCard.checkAccess(Optional<AccessContext>) static throws Exception; HandleValue own file.
- Drive saves under world root.
- Fixes: detect/scanContentsAt liquid check; place() MISS=air; tank empty checks; GPU setDepth returns bits; DebugCard setBlocks arg 6; InternetCard TCPNotifier daemon.
- Blocker: Callbacks.java interface scanning (sent to machine agent).
