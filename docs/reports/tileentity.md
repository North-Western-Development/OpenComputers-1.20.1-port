# common tileentity — wave 2 report

- traits.TileEntity abstract class extends BlockEntity; other traits are interfaces extending traits.TileEntityTrait (getLevel, getBlockPos, isServer, x(), position(), updateEntity, initialize, dispose...).
- State: nested `State` class per stateful trait, created by base class, accessors redstoneAwareState(), hubState(), computerState(), ... (null if not implemented). RobotProxy shares its robot's state.
- Lifecycle: base class dispatches static hooks per trait (Environment.onUpdateEntity(this) etc.) in Scala linearization order.
- Renames: hasErrored()/setHasErrored, openSides()/setOpenSides, relayDelay()/setRelayDelay (+relayAmount, maxQueueSize, relayCooldown), globalBuffer()/setGlobalBuffer, setPitch/setYaw; queue(), plugs(), packetsPerCycleAvg(), buffer(), machine(); redstoneAwareState().isOutputEnabled; Hub.Plug static nested (hub field); RedstoneChangedEventArgs own class.
- Ctors (type, pos, state[, tier]); Robot(pos, state); RobotProxy(type, pos, state[, Robot]); Robot.setLevelAndPosition.
- dispose() from setRemoved() (vanilla calls it on chunk unload on both loaders); initialize() from clearRemoved().
- Sync: getUpdateTag + ClientboundBlockEntityDataPacket.create; load() routes via oc:isServerData flag.
- PowerAcceptor implements EnergyHandlerProvider via PowerAcceptorEnergyHandler.
- Capabilities.getNetworkNode(be, side), getSidedEnvironment, getEnvironment, getColored helpers.

## Open
- Robot/RobotProxy implement common.transfer.FluidHandler but aren't exposed to other mods (need a provider like energy).
- Component capability forwarding lost; RelayCCAdapter dropped.
