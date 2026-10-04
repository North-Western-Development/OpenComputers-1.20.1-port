-- Walkthrough: draws a coloured height map into a tier 2 hologram projector and spins it.
local h = component.proxy(component.list("hologram")())
h.clear()
h.setPaletteColor(1, 0x2080ff)
h.setPaletteColor(2, 0x40ff60)
h.setPaletteColor(3, 0xffa020)
pcall(h.setScale, 1)
for x = 1, 48 do
  for z = 1, 48 do
    local dx, dz = (x - 24.5) / 24, (z - 24.5) / 24
    local y = math.floor(12 + 9 * math.sin(dx * 5) * math.cos(dz * 5))
    h.fill(x, z, 1, y, y > 16 and 3 or (y > 9 and 2 or 1))
  end
end
pcall(h.setRotationSpeed, 30, 0, 1, 0)
while true do computer.pullSignal() end
