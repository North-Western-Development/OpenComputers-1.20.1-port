package li.cil.oc.common.init

import li.cil.oc.Constants
import li.cil.oc.CreativeTab
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.Tier
import li.cil.oc.common.block._
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.material.Material
import net.minecraft.world.item.{Item, Rarity}
import net.minecraft.world.level.block.Block
import net.minecraftforge.registries.RegisterEvent

object Blocks {
  // Helper available during the RegisterEvent for blocks.
  private var currentHelper: RegisterEvent.RegisterHelper[Block] = _

  /** Wraps Items.registerBlock and additionally registers the Block instance
    * with the Forge BLOCKS registry, binding its intrusive holder. */
  private def registerBlock(instance: Block, id: String, itemProps: Item.Properties): Block = {
    Items.registerBlock(instance, id, itemProps)
    if (currentHelper != null) {
      currentHelper.register(new ResourceLocation(OpenComputers.ID, id), instance)
    }
    instance
  }

  private def registerBlockOnly(instance: Block, id: String): Block = {
    Items.registerBlockOnly(instance, id)
    if (currentHelper != null) {
      currentHelper.register(new ResourceLocation(OpenComputers.ID, id), instance)
    }
    instance
  }

  def init(helper: RegisterEvent.RegisterHelper[Block]) {
    currentHelper = helper
    try {
      def defaultProps = Properties.of(Material.METAL).strength(2, 5)
      def defaultItemProps = new Item.Properties().tab(CreativeTab)
      registerBlock(new Adapter(defaultProps), Constants.BlockName.Adapter, defaultItemProps)
      registerBlock(new Assembler(defaultProps), Constants.BlockName.Assembler, defaultItemProps)
      registerBlock(new Cable(defaultProps), Constants.BlockName.Cable, defaultItemProps)
      registerBlock(new Capacitor(defaultProps), Constants.BlockName.Capacitor, defaultItemProps)
      registerBlock(new Case(defaultProps, Tier.One), Constants.BlockName.CaseTier1, defaultItemProps)
      registerBlock(new Case(defaultProps, Tier.Three), Constants.BlockName.CaseTier3, defaultItemProps.rarity(Rarity.RARE))
      registerBlock(new Case(defaultProps, Tier.Two), Constants.BlockName.CaseTier2, defaultItemProps.rarity(Rarity.UNCOMMON))
      registerBlock(new ChameliumBlock(Properties.of(Material.STONE).strength(2, 5)), Constants.BlockName.ChameliumBlock, defaultItemProps)
      registerBlock(new Charger(defaultProps), Constants.BlockName.Charger, defaultItemProps)
      registerBlock(new Disassembler(defaultProps), Constants.BlockName.Disassembler, defaultItemProps)
      registerBlock(new DiskDrive(defaultProps), Constants.BlockName.DiskDrive, defaultItemProps)
      registerBlock(new Geolyzer(defaultProps), Constants.BlockName.Geolyzer, defaultItemProps)
      registerBlock(new Hologram(defaultProps, Tier.One), Constants.BlockName.HologramTier1, defaultItemProps)
      registerBlock(new Hologram(defaultProps, Tier.Two), Constants.BlockName.HologramTier2, defaultItemProps.rarity(Rarity.UNCOMMON))
      registerBlock(new Keyboard(Properties.of(Material.STONE).strength(2, 5).noOcclusion), Constants.BlockName.Keyboard, defaultItemProps)
      registerBlock(new MotionSensor(defaultProps), Constants.BlockName.MotionSensor, defaultItemProps)
      registerBlock(new PowerConverter(defaultProps), Constants.BlockName.PowerConverter,
        new Item.Properties().tab(if (!Settings.get.ignorePower) CreativeTab else null))
      registerBlock(new PowerDistributor(defaultProps), Constants.BlockName.PowerDistributor, defaultItemProps)
      registerBlock(new Printer(defaultProps), Constants.BlockName.Printer, defaultItemProps)
      registerBlock(new Raid(defaultProps), Constants.BlockName.Raid, defaultItemProps)
      registerBlock(new Redstone(defaultProps), Constants.BlockName.Redstone, defaultItemProps)
      registerBlock(new Relay(defaultProps), Constants.BlockName.Relay, defaultItemProps)
      registerBlock(new Screen(defaultProps, Tier.One), Constants.BlockName.ScreenTier1, defaultItemProps)
      registerBlock(new Screen(defaultProps, Tier.Three), Constants.BlockName.ScreenTier3, defaultItemProps.rarity(Rarity.RARE))
      registerBlock(new Screen(defaultProps, Tier.Two), Constants.BlockName.ScreenTier2, defaultItemProps.rarity(Rarity.UNCOMMON))
      registerBlock(new Rack(defaultProps), Constants.BlockName.Rack, defaultItemProps)
      registerBlock(new Waypoint(defaultProps), Constants.BlockName.Waypoint, defaultItemProps)

      registerBlock(new Case(defaultProps, Tier.Four), Constants.BlockName.CaseCreative, defaultItemProps.rarity(Rarity.EPIC))
      registerBlock(new Microcontroller(defaultProps), Constants.BlockName.Microcontroller, new Item.Properties())
      registerBlock(new Print(Properties.of(Material.METAL).strength(1, 5).noOcclusion.dynamicShape), Constants.BlockName.Print, new Item.Properties())
      registerBlockOnly(new RobotAfterimage(Properties.of(Material.AIR).noCollission.instabreak.noOcclusion.dynamicShape), Constants.BlockName.RobotAfterimage)
      registerBlock(new RobotProxy(defaultProps.noOcclusion.dynamicShape), Constants.BlockName.Robot, new Item.Properties())

      // v1.5.10
      registerBlock(new FakeEndstone(Properties.of(Material.STONE).strength(3, 15)), Constants.BlockName.Endstone, defaultItemProps)

      // v1.5.14
      registerBlock(new NetSplitter(defaultProps), Constants.BlockName.NetSplitter, defaultItemProps)

      // v1.5.16
      registerBlock(new Transposer(defaultProps), Constants.BlockName.Transposer, defaultItemProps)

      // v1.7.2
      registerBlock(new CarpetedCapacitor(defaultProps), Constants.BlockName.CarpetedCapacitor, defaultItemProps)
    } finally {
      currentHelper = null
    }
  }
}
