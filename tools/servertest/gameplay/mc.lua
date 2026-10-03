-- Microcontroller with a redstone card: lights the lamps around it for 20s.
local rs = component.proxy(component.list("redstone")())
for s = 0, 5 do rs.setOutput(s, 15) end
local t = {}
for _, n in component.list() do t[#t + 1] = n end
table.sort(t)
local t0 = computer.uptime() + 20
while computer.uptime() < t0 do computer.pullSignal(1) end
error("RESULT mc comps=" .. table.concat(t, ","))
