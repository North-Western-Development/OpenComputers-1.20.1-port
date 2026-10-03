-- Chunkloader test: after 30 s of machine uptime the robot moves one block south (across a chunk
-- border when placed at z = 15 mod 16), then idles.
local r = component.proxy(component.list("robot")())
while computer.uptime() < 30 do computer.pullSignal(1) end
r.move(3)
while true do computer.pullSignal(1) end
