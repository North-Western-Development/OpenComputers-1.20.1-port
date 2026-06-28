package li.cil.oc.client.renderer.markdown.segment

import li.cil.oc.client.renderer.markdown.MarkupFormat
import net.minecraft.ChatFormatting

private[markdown] class ItalicSegment(parent: Segment, text: String) extends TextSegment(parent, text) {
  override protected def format = ChatFormatting.ITALIC.toString

  override def toString(format: MarkupFormat.Value): String = format match {
    case MarkupFormat.Markdown => s"*$text*"
    case MarkupFormat.IGWMod => s"[prefix{o}]$text [prefix{}]"
  }
}
