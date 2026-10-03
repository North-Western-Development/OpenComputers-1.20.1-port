-- Hologram projector (tier 1) on top of the computer.
local h = component.proxy(component.list("hologram")())
local o = {}
local function add(k, f, ...) local r = table.pack(pcall(f, ...)) for i = 1, r.n do r[i] = tostring(r[i]) end o[#o + 1] = k .. "=" .. table.concat(r, ",", 2) end
add("clear", h.clear)
add("set", h.set, 1, 1, 1, 1)
add("get", h.get, 1, 1, 1)
add("get0", h.get, 2, 1, 1)
add("fill", h.fill, 5, 5, 1, 10, 1)
add("get2", h.get, 5, 10, 5)
add("depth", h.maxDepth)
add("scale", h.setScale, 2)
add("getScale", h.getScale)
add("pal", h.setPaletteColor, 1, 0xff0000)
add("getPal", h.getPaletteColor, 1)
add("copy", h.copy, 1, 1, 16, 16, 1, 0)
error("RESULT " .. table.concat(o, " "))
