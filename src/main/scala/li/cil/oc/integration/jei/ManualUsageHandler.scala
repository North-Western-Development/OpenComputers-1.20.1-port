package li.cil.oc.integration.jei

import java.util
import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.Localization
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.{IFocusGroup, RecipeIngredientRole, RecipeType}
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.resources.ResourceLocation
import org.lwjgl.glfw.GLFW

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._

object ManualUsageHandler {

  def getRecipes(registration: IRecipeRegistration): util.List[ManualUsageRecipe] = registration.getIngredientManager.getAllIngredients(VanillaTypes.ITEM_STACK).collect {
    case stack: ItemStack => api.Manual.pathFor(stack) match {
      case s: String => Option(new ManualUsageRecipe(stack, s))
      case _ => None
    }
  }.flatten.toList

  class ManualUsageRecipe(val stack: ItemStack, val path: String)

  object ManualUsageRecipeCategory extends IRecipeCategory[ManualUsageRecipe] {
    val recipeWidth: Int = 160
    val recipeHeight: Int = 125
    private var background: IDrawable = _
    private var icon: IDrawable = _
    private val button = new Button((160 - 100) / 2, 10, 100, 20, Localization.localizeLater("nei.usage.oc.Manual"), new Button.OnPress {
      override def onPress(b: Button) = ()
    })

    private val recipeType: RecipeType[ManualUsageRecipe] =
      RecipeType.create(OpenComputers.ID, "OC_api", classOf[ManualUsageRecipe])

    def initialize(guiHelper: IGuiHelper) {
      background = guiHelper.createBlankDrawable(recipeWidth, recipeHeight)
      icon = guiHelper.drawableBuilder(ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain, "textures/items/manual.png"), 0, 0, 16, 16).setTextureSize(16, 16).build()
    }

    override def getBackground: IDrawable = background

    override def getIcon: IDrawable = icon

    override def setRecipe(recipeLayout: IRecipeLayoutBuilder, recipeWrapper: ManualUsageRecipe, ingredients: IFocusGroup) {
      recipeLayout.addSlot(RecipeIngredientRole.INPUT, 0, 0).addItemStack(recipeWrapper.stack)
    }

    override def draw(recipeWrapper: ManualUsageRecipe, recipeSlotsView: IRecipeSlotsView, stack: PoseStack, mouseX: Double, mouseY: Double) {
      button.render(stack, mouseX.toInt, mouseY.toInt, 0)
    }

    @Deprecated
    override def getTitle = Component.literal("OpenComputers Manual")

    override def getRecipeType: RecipeType[ManualUsageRecipe] = recipeType
  }

}
