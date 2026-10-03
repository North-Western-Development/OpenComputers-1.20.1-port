-- 3D printer east of the computer (chamelium + ink inside): print a half slab.
local p = component.proxy(component.list("printer3d")())
local o = {}
local function add(k, f, ...) local r = table.pack(pcall(f, ...)) for i = 1, r.n do r[i] = tostring(r[i]) end o[#o + 1] = k .. "=" .. table.concat(r, ",", 2) end
add("reset", p.reset)
add("label", p.setLabel, "OC Test Print")
add("shape", p.addShape, 0, 0, 0, 16, 8, 16, "minecraft:block/stone")
add("shape2", p.addShape, 4, 8, 4, 12, 16, 12, "minecraft:block/oak_planks", true)
add("count", p.getShapeCount)
add("commit", p.commit, 2)
local t = computer.uptime() + 20
local st
repeat computer.pullSignal(0.5) st = p.status() until st == "idle" or computer.uptime() > t
add("status", p.status)
error("RESULT " .. table.concat(o, " "))
