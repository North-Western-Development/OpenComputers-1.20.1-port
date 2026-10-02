package li.cil.oc.client;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.OpenComputers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;


/**
 * Texture locations used by OpenComputers' client side code.
 * <p>
 * GUI, font, icon and model textures are plain textures: their locations are
 * the full resource paths ({@code opencomputers:textures/gui/x.png}), so the
 * texture manager loads them on first use (blit / RenderType / {@link #bind}).
 * <p>
 * Item and block textures live in the block atlas. Stitching is done through
 * {@code assets/minecraft/atlases/blocks.json} (directory sources for
 * {@code blocks/} and {@code items/}), so the locations here are sprite names
 * ({@code opencomputers:blocks/x}) to be looked up via {@link #getSprite}.
 */
public final class Textures {
    private Textures() {
    }

    private static ResourceLocation texture(String path) {
        return new ResourceLocation(OpenComputers.ID, "textures/" + path + ".png");
    }

    private static ResourceLocation sprite(String path) {
        return new ResourceLocation(OpenComputers.ID, path);
    }

    public static final class Font {
        private Font() {
        }

        public static final ResourceLocation Aliased = L("chars_aliased");
        public static final ResourceLocation AntiAliased = L("chars");

        private static ResourceLocation L(String name) {
            return texture("font/" + name);
        }
    }

    public static final class GUI {
        private GUI() {
        }

        public static final ResourceLocation Background = L("background");
        public static final ResourceLocation Bar = L("bar");
        public static final ResourceLocation Borders = L("borders");
        public static final ResourceLocation ButtonDriveMode = L("button_drive_mode");
        public static final ResourceLocation ButtonPower = L("button_power");
        public static final ResourceLocation ButtonRange = L("button_range");
        public static final ResourceLocation ButtonRun = L("button_run");
        public static final ResourceLocation ButtonScroll = L("button_scroll");
        public static final ResourceLocation ButtonSide = L("button_side");
        public static final ResourceLocation ButtonRelay = L("button_relay");
        public static final ResourceLocation Computer = L("computer");
        public static final ResourceLocation Database = L("database");
        public static final ResourceLocation Database1 = L("database1");
        public static final ResourceLocation Database2 = L("database2");
        public static final ResourceLocation Disassembler = L("disassembler");
        public static final ResourceLocation Drive = L("drive");
        public static final ResourceLocation Drone = L("drone");
        public static final ResourceLocation KeyboardMissing = L("keyboard_missing");
        public static final ResourceLocation Manual = L("manual");
        public static final ResourceLocation ManualHome = L("manual_home");
        public static final ResourceLocation ManualMissingItem = L("manual_missing_item");
        public static final ResourceLocation ManualTab = L("manual_tab");
        public static final ResourceLocation Nanomachines = L("nanomachines_power");
        public static final ResourceLocation NanomachinesBar = L("nanomachines_power_bar");
        public static final ResourceLocation Printer = L("printer");
        public static final ResourceLocation PrinterInk = L("printer_ink");
        public static final ResourceLocation PrinterMaterial = L("printer_material");
        public static final ResourceLocation PrinterProgress = L("printer_progress");
        public static final ResourceLocation Rack = L("rack");
        public static final ResourceLocation Raid = L("raid");
        public static final ResourceLocation Range = L("range");
        public static final ResourceLocation Robot = L("robot");
        public static final ResourceLocation RobotAssembler = L("robot_assembler");
        public static final ResourceLocation RobotNoScreen = L("robot_noscreen");
        public static final ResourceLocation RobotSelection = L("robot_selection");
        public static final ResourceLocation Server = L("server");
        public static final ResourceLocation Slot = L("slot");
        public static final ResourceLocation UpgradeTab = L("upgrade_tab");
        public static final ResourceLocation Waypoint = L("waypoint");

        private static ResourceLocation L(String name) {
            return texture("gui/" + name);
        }
    }

    /** Slot icons live in common code, see {@link li.cil.oc.common.container.SlotIcons}. */
    public static final class Icons {
        private Icons() {
        }

        public static ResourceLocation get(String slotType) {
            return li.cil.oc.common.container.SlotIcons.get(slotType);
        }

        public static ResourceLocation get(int tier) {
            return li.cil.oc.common.container.SlotIcons.get(tier);
        }
    }

    public static final class Model {
        private Model() {
        }

        public static final ResourceLocation UpgradeCrafting = L("crafting_upgrade");
        public static final ResourceLocation UpgradeGenerator = L("generator_upgrade");
        public static final ResourceLocation UpgradeInventory = L("inventory_upgrade");
        public static final ResourceLocation HologramEffect = L("hologram_effect");
        public static final ResourceLocation Drone = L("drone");
        public static final ResourceLocation Robot = L("robot");

        private static ResourceLocation L(String name) {
            return texture("model/" + name);
        }
    }

    /**
     * Sprites in the block atlas (see class doc).
     */
    public static final class Item {
        private Item() {
        }

        public static final ResourceLocation DroneItem = L("drone");
        public static final ResourceLocation Robot = L("robot");

        private static ResourceLocation L(String name) {
            return sprite("items/" + name);
        }
    }

    /**
     * These are kept in the block texture atlas to support animations (see class doc).
     */
    public static final class Block {
        private Block() {
        }

        public static final ResourceLocation AdapterOn = L("overlay/adapter_on");
        public static final ResourceLocation AssemblerSideAssembling = L("overlay/assembler_side_assembling");
        public static final ResourceLocation AssemblerSideOn = L("overlay/assembler_side_on");
        public static final ResourceLocation AssemblerTopOn = L("overlay/assembler_top_on");
        public static final ResourceLocation CaseFrontActivity = L("overlay/case_front_activity");
        public static final ResourceLocation CaseFrontError = L("overlay/case_front_error");
        public static final ResourceLocation CaseFrontOn = L("overlay/case_front_on");
        public static final ResourceLocation ChargerFrontOn = L("overlay/charger_front_on");
        public static final ResourceLocation ChargerSideOn = L("overlay/charger_side_on");
        public static final ResourceLocation DisassemblerSideOn = L("overlay/disassembler_side_on");
        public static final ResourceLocation DisassemblerTopOn = L("overlay/disassembler_top_on");
        public static final ResourceLocation DiskDriveFrontActivity = L("overlay/diskdrive_front_activity");
        public static final ResourceLocation GeolyzerTopOn = L("overlay/geolyzer_top_on");
        public static final ResourceLocation MicrocontrollerFrontLight = L("overlay/microcontroller_front_light");
        public static final ResourceLocation MicrocontrollerFrontOn = L("overlay/microcontroller_front_on");
        public static final ResourceLocation MicrocontrollerFrontError = L("overlay/microcontroller_front_error");
        public static final ResourceLocation NetSplitterOn = L("overlay/netsplitter_on");
        public static final ResourceLocation PowerDistributorSideOn = L("overlay/powerdistributor_side_on");
        public static final ResourceLocation PowerDistributorTopOn = L("overlay/powerdistributor_top_on");
        public static final ResourceLocation RackDiskDrive = L("rack_disk_drive");
        public static final ResourceLocation RackDiskDriveActivity = L("overlay/rack_disk_drive_activity");
        public static final ResourceLocation RackServer = L("rack_server");
        public static final ResourceLocation RackServerActivity = L("overlay/rack_server_activity");
        public static final ResourceLocation RackServerOn = L("overlay/rack_server_on");
        public static final ResourceLocation RackServerError = L("overlay/rack_server_error");
        public static final ResourceLocation RackServerNetworkActivity = L("overlay/rack_server_network_activity");
        public static final ResourceLocation RackTerminalServer = L("rack_terminal_server");
        public static final ResourceLocation RackTerminalServerOn = L("overlay/rack_terminal_server_on");
        public static final ResourceLocation RackTerminalServerPresence = L("overlay/rack_terminal_server_presence");
        public static final ResourceLocation RaidFrontActivity = L("overlay/raid_front_activity");
        public static final ResourceLocation RaidFrontError = L("overlay/raid_front_error");
        public static final ResourceLocation ScreenUpIndicator = L("overlay/screen_up_indicator");
        public static final ResourceLocation SwitchSideOn = L("overlay/switch_side_on");
        public static final ResourceLocation TransposerOn = L("overlay/transposer_on");

        public static final ResourceLocation Cable = L("cable");
        public static final ResourceLocation CableCap = L("cablecap");
        public static final ResourceLocation GenericTop = L("generic_top");
        public static final ResourceLocation NetSplitterSide = L("netsplitter_side");
        public static final ResourceLocation NetSplitterTop = L("netsplitter_top");
        public static final ResourceLocation RackFront = L("rack_front");
        public static final ResourceLocation RackSide = L("rack_side");

        private static ResourceLocation L(String name) {
            return sprite("blocks/" + name);
        }

        private static ResourceLocation B(String name) {
            return L(name);
        }

        // Kill me now.
        public static final class Screen {
            private Screen() {
            }

        public static final ResourceLocation[] Single = {
          B("screen/b"),
          B("screen/b"),
          B("screen/b2"),
          B("screen/b2"),
          B("screen/b2"),
          B("screen/b2")
        };

        public static final ResourceLocation[] SingleFront = {
          B("screen/f"),
          B("screen/f2")
        };

        public static final ResourceLocation[][][] Horizontal = {
          // Vertical.
          {
            {
              B("screen/bht"),
              B("screen/bhb"),
              B("screen/bht2"),
              B("screen/bht2"),
              B("screen/b2"),
              B("screen/b2")
            },
            {
              B("screen/bhm"),
              B("screen/bhm"),
              B("screen/bhm2"),
              B("screen/bhm2"),
              B("screen/b"), // Not rendered.
              B("screen/b") // Not rendered.
            },
            {
              B("screen/bhb"),
              B("screen/bht"),
              B("screen/bhb2"),
              B("screen/bhb2"),
              B("screen/b2"),
              B("screen/b2")
            }
          },
          // Horizontal.
          {
            {
              B("screen/bhb2"),
              B("screen/bht2"),
              B("screen/bht"),
              B("screen/bhb"),
              B("screen/b2"),
              B("screen/b2")
            },
            {
              B("screen/bhm2"),
              B("screen/bhm2"),
              B("screen/bhm"),
              B("screen/bhm"),
              B("screen/b"), // Not rendered.
              B("screen/b") // Not rendered.
            },
            {
              B("screen/bht2"),
              B("screen/bhb2"),
              B("screen/bhb"),
              B("screen/bht"),
              B("screen/b2"),
              B("screen/b2")
            }
          }
        };

        public static final ResourceLocation[][] HorizontalFront = {
          // Vertical.
          {
            B("screen/fhb2"),
            B("screen/fhm2"),
            B("screen/fht2")
          },
          // Horizontal.
          {
            B("screen/fhb"),
            B("screen/fhm"),
            B("screen/fht")
          }
        };

        public static final ResourceLocation[][][] Vertical = {
          // Vertical.
          {
            {
              B("screen/b"),
              B("screen/b"),
              B("screen/bvt"),
              B("screen/bvt"),
              B("screen/bvt"),
              B("screen/bvt")
            },
            {
              B("screen/b"), // Not rendered.
              B("screen/b"), // Not rendered.
              B("screen/bvm"),
              B("screen/bvm"),
              B("screen/bvm"),
              B("screen/bvm")
            },
            {
              B("screen/b"),
              B("screen/b"),
              B("screen/bvb2"),
              B("screen/bvb2"),
              B("screen/bvb2"),
              B("screen/bvb2")
            }
          },
          // Horizontal.
          {
            {
              B("screen/b2"),
              B("screen/b2"),
              B("screen/bvt"),
              B("screen/bvt"),
              B("screen/bht2"),
              B("screen/bhb2")
            },
            {
              B("screen/b"), // Not rendered.
              B("screen/b"), // Not rendered.
              B("screen/bvm"),
              B("screen/bvm"),
              B("screen/bhm2"),
              B("screen/bhm2")
            },
            {
              B("screen/b2"),
              B("screen/b2"),
              B("screen/bvb"),
              B("screen/bvb"),
              B("screen/bhb2"),
              B("screen/bht2")
            }
          }
        };

        public static final ResourceLocation[][] VerticalFront = {
          // Vertical.
          {
            B("screen/fvt"),
            B("screen/fvm"),
            B("screen/fvb2")
          },
          // Horizontal.
          {
            B("screen/fvt"),
            B("screen/fvm"),
            B("screen/fvb")
          }
        };

        public static final ResourceLocation[][][][] Multi = {
          // Vertical.
          {
            // Top.
            {
              {
                B("screen/bht"),
                B("screen/bhb"),
                B("screen/btl"),
                B("screen/btr"),
                B("screen/bvb"),
                B("screen/bvt")
              },
              {
                B("screen/bhm"),
                B("screen/bhm"),
                B("screen/btm"),
                B("screen/btm"),
                B("screen/b"), // Not rendered.
                B("screen/b") // Not rendered.
              },
              {
                B("screen/bhb"),
                B("screen/bht"),
                B("screen/btr"),
                B("screen/btl"),
                B("screen/bvt"),
                B("screen/bvb")
              }
            },
            // Middle.
            {
              {
                B("screen/b"), // Not rendered.
                B("screen/b"), // Not rendered.
                B("screen/bml"),
                B("screen/bmr"),
                B("screen/bvm"),
                B("screen/bvm")
              },
              {
                B("screen/b"), // Not rendered.
                B("screen/b"), // Not rendered.
                B("screen/bmm"),
                B("screen/bmm"),
                B("screen/b"), // Not rendered.
                B("screen/b") // Not rendered.
              },
              {
                B("screen/b"), // Not rendered.
                B("screen/b"), // Not rendered.
                B("screen/bmr"),
                B("screen/bml"),
                B("screen/bvm"),
                B("screen/bvt")
              }
            },
            // Bottom.
            {
              {
                B("screen/bht"),
                B("screen/bhb"),
                B("screen/bbl2"),
                B("screen/bbr2"),
                B("screen/bvt"),
                B("screen/bvb2")
              },
              {
                B("screen/bhm"),
                B("screen/bhm"),
                B("screen/bbm2"),
                B("screen/bbm2"),
                B("screen/b"), // Not rendered.
                B("screen/b") // Not rendered.
              },
              {
                B("screen/bhb"),
                B("screen/bht"),
                B("screen/bbr2"),
                B("screen/bbl2"),
                B("screen/bvb2"),
                B("screen/bvt")
              }
            }
          },
          // Horizontal.
          {
            // Top.
            {
              {
                B("screen/bhb2"),
                B("screen/bht2"),
                B("screen/btl"),
                B("screen/btr"),
                B("screen/bht2"),
                B("screen/bhb2")
              },
              {
                B("screen/bhm2"),
                B("screen/bhm2"),
                B("screen/btm"),
                B("screen/btm"),
                B("screen/b"), // Not rendered.
                B("screen/b") // Not rendered.
              },
              {
                B("screen/bht2"),
                B("screen/bhb2"),
                B("screen/btr"),
                B("screen/btl"),
                B("screen/bht2"),
                B("screen/bhb2")
              }
            },
            // Middle.
            {
              {
                B("screen/b"), // Not rendered.
                B("screen/b"), // Not rendered.
                B("screen/bml"),
                B("screen/bml"),
                B("screen/bhm2"),
                B("screen/bhm2")
              },
              {
                B("screen/b"), // Not rendered.
                B("screen/b"), // Not rendered.
                B("screen/bmm"),
                B("screen/bmm"),
                B("screen/b"), // Not rendered.
                B("screen/b") // Not rendered.
              },
              {
                B("screen/b"), // Not rendered.
                B("screen/b"), // Not rendered.
                B("screen/bmr"),
                B("screen/bmr"),
                B("screen/bhm2"),
                B("screen/bhm2")
              }
            },
            // Bottom.
            {
              {
                B("screen/bhb2"),
                B("screen/bht2"),
                B("screen/bbl"),
                B("screen/bbr"),
                B("screen/bhb2"),
                B("screen/bht2")
              },
              {
                B("screen/bhm2"),
                B("screen/bhm2"),
                B("screen/bbm"),
                B("screen/bbm"),
                B("screen/b"), // Not rendered.
                B("screen/b") // Not rendered.
              },
              {
                B("screen/bht2"),
                B("screen/bhb2"),
                B("screen/bbr"),
                B("screen/bbl"),
                B("screen/bhb2"),
                B("screen/bht2")
              }
            }
          }
        };

        public static final ResourceLocation[][][] MultiFront = {
          // Vertical.
          {
            {
              B("screen/ftr"),
              B("screen/ftm"),
              B("screen/ftl")
            },
            {
              B("screen/fmr"),
              B("screen/fmm"),
              B("screen/fml")
            },
            {
              B("screen/fbr2"),
              B("screen/fbm2"),
              B("screen/fbl2")
            }
          },
          // Horizontal.
          {
            {
              B("screen/ftr"),
              B("screen/ftm"),
              B("screen/ftl")
            },
            {
              B("screen/fmr"),
              B("screen/fmm"),
              B("screen/fml")
            },
            {
              B("screen/fbr"),
              B("screen/fbm"),
              B("screen/fbl")
            }
          }
        };

        }

        public static void bind() {
            Textures.bind(InventoryMenu.BLOCK_ATLAS);
        }
    }

    /**
     * Sets the given texture as shader texture 0 (1.20.1 replacement for binding
     * a texture). Passing null unbinds.
     */
    public static void bind(ResourceLocation location) {
        if (location != null) RenderSystem.setShaderTexture(0, location);
        else RenderSystem.setShaderTexture(0, 0);
    }

    public static TextureAtlasSprite getSprite(ResourceLocation location) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(location);
    }
}
