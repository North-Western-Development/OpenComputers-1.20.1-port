-- Drone test: flies up 3 and east 2 blocks, reports offsets and its own view of the world.
local d = component.proxy(component.list("drone")())
local o = {}
local function add(k, ...) local t = table.pack(...) for i = 1, t.n do t[i] = tostring(t[i]) end o[#o + 1] = k .. "=" .. table.concat(t, ",") end
local function sleep(s) local t = computer.uptime() + s while computer.uptime() < t do computer.pullSignal(t - computer.uptime()) end end
local function settle() local t = computer.uptime() + 10 sleep(0.5) while d.getOffset() > 0.1 and computer.uptime() < t do sleep(0.2) end end
d.setStatusText("OC test")
add("status", d.getStatusText())
add("inv", d.inventorySize())
add("maxv", d.getMaxVelocity and d.getMaxVelocity())
add("det", d.detect(0))
d.move(0, 3, 0)
add("off0", math.floor(d.getOffset() * 10 + 0.5) / 10)
settle()
add("off1", math.floor(d.getOffset() * 10 + 0.5) / 10)
d.move(2, 0, 0)
settle()
add("off2", math.floor(d.getOffset() * 10 + 0.5) / 10)
add("det2", d.detect(0))
d.setLightColor(0xff0000)
add("light", d.getLightColor())
add("name", d.name())
sleep(10)
error("RESULT " .. table.concat(o, " "))
