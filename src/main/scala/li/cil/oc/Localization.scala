package li.cil.oc

import net.minecraft.locale.Language
import net.minecraft.network.chat.{ClickEvent, Component, HoverEvent, MutableComponent, TextComponent, TranslatableComponent}

import scala.util.matching.Regex

object Localization {
  private def resolveKey(key: String): Option[String] =
    if (canLocalize(Settings.namespace + key)) Option(Settings.namespace + key)
    else if (canLocalize(key)) Option(key)
    else Option.empty

  def canLocalize(key: String): Boolean = Language.getInstance().has(key)

  def localizeLater(key: String): MutableComponent = new TranslatableComponent(resolveKey(key).getOrElse(key))

  def localizeLater(key: String, values: AnyRef*): MutableComponent = new TranslatableComponent(resolveKey(key).getOrElse(key), values: _*)

  def localizeImmediately(key: String, values: AnyRef*): String = {
    resolveKey(key).map(k => String.format(Language.getInstance().getOrDefault(k), values: _*).linesIterator.map(_.trim).mkString("\n")).getOrElse(key)
  }

  def localizeImmediately(key: String): String = {
    resolveKey(key).map(k => Language.getInstance().getOrDefault(k).linesIterator.map(_.trim).mkString("\n")).getOrElse(key)
  }

  object Analyzer {
    def Address(value: String): MutableComponent = {
      val result = localizeLater("gui.Analyzer.Address", value)
      result.setStyle(result.getStyle
        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value))
        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, localizeLater("gui.Analyzer.CopyToClipboard"))))
    }

    def AddressCopied: MutableComponent = localizeLater("gui.Analyzer.AddressCopied")

    def ChargerSpeed(value: Double): MutableComponent = localizeLater("gui.Analyzer.ChargerSpeed", (value * 100).toInt + "%")

    def ComponentName(value: String): MutableComponent = localizeLater("gui.Analyzer.ComponentName", value)

    def Components(count: Int, maxCount: Int): MutableComponent = localizeLater("gui.Analyzer.Components", count + "/" + maxCount)

    def LastError(value: String): MutableComponent = localizeLater("gui.Analyzer.LastError", localizeLater(value))

    def RobotOwner(owner: String): MutableComponent = localizeLater("gui.Analyzer.RobotOwner", owner)

    def RobotName(name: String): MutableComponent = localizeLater("gui.Analyzer.RobotName", name)

    def RobotXp(experience: Double, level: Int): MutableComponent = localizeLater("gui.Analyzer.RobotXp", f"$experience%.2f", level.toString)

    def StoredEnergy(value: String): MutableComponent = localizeLater("gui.Analyzer.StoredEnergy", value)

    def TotalEnergy(value: String): MutableComponent = localizeLater("gui.Analyzer.TotalEnergy", value)

    def Users(list: Iterable[String]): MutableComponent = localizeLater("gui.Analyzer.Users", list.mkString(", "))

    def WirelessStrength(value: Double): MutableComponent = localizeLater("gui.Analyzer.WirelessStrength", value.toInt.toString)
  }

  object Assembler {
    def InsertTemplate: String = localizeImmediately("gui.Assembler.InsertCase")

    def CollectResult: String = localizeImmediately("gui.Assembler.Collect")

    def InsertCPU: MutableComponent = localizeLater("gui.Assembler.InsertCPU")

    def InsertRAM: MutableComponent = localizeLater("gui.Assembler.InsertRAM")

    def Complexity(complexity: Int, maxComplexity: Int): MutableComponent = {
      val message = localizeLater("gui.Assembler.Complexity", complexity.toString, maxComplexity.toString)
      if (complexity > maxComplexity) new TextComponent("§4").append(message)
      else message
    }

    def Run: String = localizeImmediately("gui.Assembler.Run")

    def Progress(progress: Double, timeRemaining: String): String = localizeImmediately("gui.Assembler.Progress", progress.toInt.toString, timeRemaining)

    def Warning(name: String): MutableComponent = new TextComponent("§7- ").append(localizeLater("gui.Assembler.Warning." + name))

    def Warnings: MutableComponent = localizeLater("gui.Assembler.Warnings")
  }

  object Chat {
    def WarningLuaFallback: MutableComponent = new TextComponent("§aOpenComputers§f: ").append(localizeLater("gui.Chat.WarningLuaFallback"))

    def WarningProjectRed: MutableComponent = new TextComponent("§aOpenComputers§f: ").append(localizeLater("gui.Chat.WarningProjectRed"))

    def WarningRecipes: MutableComponent = new TextComponent("§aOpenComputers§f: ").append(localizeLater("gui.Chat.WarningRecipes"))

    def WarningClassTransformer: MutableComponent = new TextComponent("§aOpenComputers§f: ").append(localizeLater("gui.Chat.WarningClassTransformer"))

    def WarningLink(url: String): MutableComponent = new TextComponent("§aOpenComputers§f: ").append(localizeLater("gui.Chat.WarningLink", url))

    def InfoNewVersion(version: String): MutableComponent = new TextComponent("§aOpenComputers§f: ").append(localizeLater("gui.Chat.NewVersion", version))

    def TextureName(name: String): MutableComponent = new TextComponent("§aOpenComputers§f: ").append(localizeLater("gui.Chat.TextureName", name))
  }

  object Computer {
    def TurnOff: String = localizeImmediately("gui.Robot.TurnOff")

    def TurnOn: String = localizeImmediately("gui.Robot.TurnOn")

    def Power: String = localizeImmediately("gui.Robot.Power")
  }

  object Drive {
    def Managed: String = localizeImmediately("gui.Drive.Managed")

    def Unmanaged: String = localizeImmediately("gui.Drive.Unmanaged")

    def Warning: String = localizeImmediately("gui.Drive.Warning")

    def ReadOnlyLock: String = localizeImmediately("gui.Drive.ReadOnlyLock")

    def LockWarning: String = localizeImmediately("gui.Drive.ReadOnlyLockWarning")
  }

  object Raid {
    def Warning: String = localizeImmediately("gui.Raid.Warning")
  }

  object Rack {
    def Top: String = localizeImmediately("gui.Rack.Top")

    def Bottom: String = localizeImmediately("gui.Rack.Bottom")

    def Left: String = localizeImmediately("gui.Rack.Left")

    def Right: String = localizeImmediately("gui.Rack.Right")

    def Back: String = localizeImmediately("gui.Rack.Back")

    def None: String = localizeImmediately("gui.Rack.None")

    def RelayEnabled: String = localizeImmediately("gui.Rack.Enabled")

    def RelayDisabled: String = localizeImmediately("gui.Rack.Disabled")

    def RelayModeTooltip: String = localizeImmediately("gui.Rack.RelayModeTooltip")
  }

  object Switch {
    def TransferRate: String = localizeImmediately("gui.Switch.TransferRate")

    def PacketsPerCycle: String = localizeImmediately("gui.Switch.PacketsPerCycle")

    def QueueSize: String = localizeImmediately("gui.Switch.QueueSize")
  }

  object Terminal {
    def InvalidKey: MutableComponent = localizeLater("gui.Terminal.InvalidKey")

    def OutOfRange: MutableComponent = localizeLater("gui.Terminal.OutOfRange")
  }

  object Tooltip {
    def DiskUsage(used: Long, capacity: Long): String = localizeImmediately("tooltip.diskusage", used.toString, capacity.toString)

    def DiskMode(isUnmanaged: Boolean): String = localizeImmediately(if (isUnmanaged) "tooltip.diskmodeunmanaged" else "tooltip.diskmodemanaged")

    def Materials: String = localizeImmediately("tooltip.materials")

    def DiskLock(lockInfo: String): String = if (lockInfo.isEmpty) "" else localizeImmediately("tooltip.disklocked", lockInfo)

    def Tier(tier: Int): String = localizeImmediately("tooltip.tier", tier.toString)

    def PrintBeaconBase: String = localizeImmediately("tooltip.print.BeaconBase")

    def PrintLightValue(level: Int): String = localizeImmediately("tooltip.print.LightValue", level.toString)

    def PrintRedstoneLevel(level: Int): String = localizeImmediately("tooltip.print.RedstoneLevel", level.toString)

    def MFULinked(isLinked: Boolean): String = localizeImmediately(if (isLinked) "tooltip.upgrademf.Linked" else "tooltip.upgrademf.Unlinked")

    def ExperienceLevel(level: Double): String = localizeImmediately("tooltip.robot_level", level.toString)
  }

}
