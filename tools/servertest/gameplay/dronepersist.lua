-- Drone persistence: hovers 2 blocks up and counts; the server is restarted meanwhile.
local d = component.proxy(component.list("drone")())
local tok = tostring(math.random(1, 1000000000))
d.move(0, 2, 0)
local n = 0
while n < 80 do
  n = n + 1
  d.setStatusText(tok .. ":" .. n)
  computer.pullSignal(0.5)
end
error("RESULT drone n=" .. n .. " status=" .. d.getStatusText() .. " tok=" .. tok .. " off=" .. string.format("%.1f", d.getOffset()))
