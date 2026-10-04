-- Walkthrough: sends a numbered message over its network card every second and shows it.
local m = component.proxy(component.list("modem")())
local g = component.proxy(component.list("gpu")())
g.bind(component.list("screen")())
g.setResolution(30, 8)
g.fill(1, 1, 30, 8, " ")
g.set(2, 2, "Computer A (sender)")
local n = 0
while true do
  n = n + 1
  m.broadcast(42, "ping #" .. n)
  g.set(2, 4, "sent: ping #" .. n .. "   ")
  computer.pullSignal(1)
end
