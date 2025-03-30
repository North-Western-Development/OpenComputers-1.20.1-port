package li.cil.oc.integration.util

import net.minecraft.client.resources.I18n
import net.minecraft.world.entity.LivingEntity
import net.minecraft.util.DamageSource
import net.minecraft.network.chat.Component
import net.minecraft.util.text.TranslationTextComponent

class DamageSourceWithRandomCause(name: String, numCauses: Int) extends DamageSource(name) {
  override def getLocalizedDeathMessage(damagee: LivingEntity): Component = {
    val damager = damagee.getKillCredit
    val format = "death.attack." + msgId + "." + (damagee.level.random.nextInt(numCauses) + 1)
    val withCauseFormat = format + ".player"
    if (damager != null && I18n.exists(withCauseFormat))
      new TranslationTextComponent(withCauseFormat, damagee.getDisplayName, damager.getDisplayName)
    else
      new TranslationTextComponent(format, damagee.getDisplayName)
  }
}
