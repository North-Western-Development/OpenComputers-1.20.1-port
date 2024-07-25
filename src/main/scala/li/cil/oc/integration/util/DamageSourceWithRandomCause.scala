package li.cil.oc.integration.util


import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.{Component, TranslatableComponent}
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity

class DamageSourceWithRandomCause(name: String, numCauses: Int) extends DamageSource(name) {
  override def getLocalizedDeathMessage(damagee: LivingEntity): Component = {
    val damager = damagee.getKillCredit
    val format = "death.attack." + msgId + "." + (damagee.level.random.nextInt(numCauses) + 1)
    val withCauseFormat = format + ".player"
    if (damager != null && I18n.exists(withCauseFormat))
      new TranslatableComponent(withCauseFormat, damagee.getDisplayName, damager.getDisplayName)
    else
      new TranslatableComponent(format, damagee.getDisplayName)
  }
}
