-- Characters outside the BMP: gpu.set/get/fill round trip and the unicode library.
local gpu = component.proxy(component.list("gpu")())
gpu.bind((component.list("screen")()))
local o = {}
local E, S = "\u{1F600}", "\u{1F40D}" -- grinning face (wide), snake
local s = "A" .. E .. "B"
gpu.set(1, 1, s)
local function G(x, y) return (gpu.get(x, y)) end
o[#o + 1] = "get=" .. G(1, 1) .. "|" .. G(2, 1) .. "|" .. G(3, 1) .. "|" .. G(4, 1)
o[#o + 1] = "rt=" .. tostring(G(2, 1) == E)
gpu.fill(1, 2, 3, 1, S)
o[#o + 1] = "fill=" .. G(1, 2) .. G(2, 2) .. G(3, 2) .. ":" .. tostring(G(3, 2) == S)
gpu.set(1, 3, E, true) -- vertical
o[#o + 1] = "vert=" .. tostring(G(1, 3) == E)
gpu.set(-1, 4, "xy" .. E .. "z") -- partly left of the screen
o[#o + 1] = "left=" .. G(1, 4) .. G(3, 4)
o[#o + 1] = "len=" .. unicode.len(s) .. " sub=" .. unicode.sub(s, 2, 2) .. "," .. unicode.sub(s, -2) .. "," .. unicode.sub(s, 0, 1)
o[#o + 1] = "rev=" .. unicode.reverse(s) .. " char=" .. tostring(unicode.char(0x1F600) == E)
o[#o + 1] = "wide=" .. tostring(unicode.isWide(E)) .. "," .. unicode.charWidth(E) .. " wlen=" .. unicode.wlen(s)
o[#o + 1] = "wtrunc=" .. unicode.wtrunc(s, 2) .. "," .. unicode.wtrunc(s, 4) .. " upper=" .. unicode.upper("a" .. E)
error("RESULT " .. table.concat(o, " "))
