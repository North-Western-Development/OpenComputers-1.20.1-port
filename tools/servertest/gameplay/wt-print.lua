-- Walkthrough: prints a small two-part "lamp" in the 3D printer next to the computer, twice.
local p = component.proxy(component.list("printer3d")())
p.reset()
p.setLabel("OC Lamp")
p.addShape(4, 0, 4, 12, 2, 12, "minecraft:block/dark_oak_planks")
p.addShape(7, 2, 7, 9, 10, 9, "minecraft:block/iron_block")
p.addShape(3, 10, 3, 13, 16, 13, "minecraft:block/glowstone")
pcall(p.setLightLevel, 15)
p.commit(2)
while true do computer.pullSignal() end
