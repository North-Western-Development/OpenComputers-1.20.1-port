package li.cil.oc.integration.jei

import com.google.common.base.Strings
import com.google.common.collect.Lists
import li.cil.oc.server.machine.Callbacks
import li.cil.oc.{OpenComputers, Settings, api}
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.recipe.{IFocusGroup, RecipeIngredientRole}
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.{Component, Style}
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

import java.util
import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable

object CallbackDocHandler {

  private val DocPattern = """(?s)^function(\(.*?\).*?) -- (.*)$""".r

  private val VexPattern = """(?s)^function(\(.*?\).*?); (.*)$""".r

  def getRecipes(registration: IRecipeRegistration): util.List[CallbackDocRecipe] = registration.getIngredientManager.getAllIngredients(VanillaTypes.ITEM_STACK).collect {
    case stack: ItemStack =>
      val callbacks = api.Driver.environmentsFor(stack).flatMap(getCallbacks).toBuffer

      if (callbacks.nonEmpty) {
        val pages = mutable.Buffer.empty[String]
        val lastPage = callbacks.toArray.sorted.foldLeft("") {
          (last, doc) =>
            if (last.linesIterator.length + 2 + doc.linesIterator.length > 12) {
              // We've potentially got some pretty long documentation here, split it up first
              last.linesIterator.grouped(12).map(_.mkString("\n")).foreach(pages += _)
              doc
            }
            else if (last.nonEmpty) last + "\n\n" + doc
            else doc
        }
        // The last page may be too long as well.
        lastPage.linesIterator.grouped(12).map(_.mkString("\n")).foreach(pages += _)

        Option(pages.map(page => new CallbackDocRecipe(stack, page)))
      }
      else None
  }.flatten.flatten.toList

  private def getCallbacks(env: Class[_]) = if (env != null) {

    Callbacks.fromClass(env).map {
      case (name, callback) =>
        val doc = callback.annotation.doc
        if (Strings.isNullOrEmpty(doc)) name
        else {
          val (signature, documentation) = doc match {
            case DocPattern(head, tail) => (name + head, tail)
            case VexPattern(head, tail) => (name + head, tail)
            case _ => (name, doc)
          }
          wrap(signature, 160).map(ChatFormatting.BLACK.toString + _).mkString("\n") +
            ChatFormatting.RESET + "\n" +
            wrap(documentation, 152).map("  " + _).mkString("\n")
        }
    }
  }
  else Seq.empty

  protected def wrap(line: String, width: Int): util.List[String] = {
    val list = new util.ArrayList[String]
    Minecraft.getInstance.font.getSplitter.splitLines(line, width, Style.EMPTY, true,
      (style: Style, start: Int, end: Int) => list.add(line.substring(start, end)))
    list
  }

  class CallbackDocRecipe(val stack: ItemStack, val page: String)

  object CallbackDocRecipeCategory extends IRecipeCategory[CallbackDocRecipe] {
    val recipeWidth: Int = 160
    val recipeHeight: Int = 125
    private var background: IDrawable = _
    private var icon: IDrawable = _

    def initialize(guiHelper: IGuiHelper) {
      background = guiHelper.createBlankDrawable(recipeWidth, recipeHeight)
      icon = new DrawableAnimatedIcon(new ResourceLocation(Settings.resourceDomain, "textures/items/tablet_on.png"), 0, 0, 16, 16, 16, 32,
        guiHelper.createTickTimer(20, 1, true), 0, 16)
    }

    override def getRecipeClass = classOf[CallbackDocRecipe]

    override def getIcon: IDrawable = icon

    override def getBackground: IDrawable = background

    override def setRecipe(recipeLayout: IRecipeLayoutBuilder, recipeWrapper: CallbackDocRecipe, ingredients: IFocusGroup): Unit = {
      recipeLayout.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(Lists.newArrayList(recipeWrapper.stack))
    }

    override def draw(recipeWrapper: CallbackDocRecipe, recipeSlotsView: IRecipeSlotsView
    , guiGraphics: GuiGraphics, mouseX: Double, mouseY: Double): Unit = {
      val minecraft = Minecraft.getInstance
      for ((text, line) <- recipeWrapper.page.linesIterator.zipWithIndex) {
        guiGraphics.drawString(minecraft.font, text, 4, 4 + line * (minecraft.font.lineHeight + 1), 0x333333)
      }
    }

    @Deprecated
    override def getTitle = Component.literal("OpenComputers API")

    override def getRegistryName(recipe: CallbackDocRecipe) = new ResourceLocation(OpenComputers.ID, "part_api")
  }

}
