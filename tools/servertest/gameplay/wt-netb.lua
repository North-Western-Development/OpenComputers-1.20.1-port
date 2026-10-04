-- Walkthrough: listens on port 42 and shows every received message.
local m = component.proxy(component.list("modem")())
local g = component.proxy(component.list("gpu")())
g.bind(component.list("screen")())
g.setResolution(30, 8)
g.fill(1, 1, 30, 8, " ")
g.set(2, 2, "Computer B (receiver)")
m.open(42)
while true do
  local ev, _, from, port, dist, msg = computer.pullSignal()
  if ev == "modem_message" then
    g.set(2, 4, "got: " .. tostring(msg) .. "    ")
    g.set(2, 5, "from " .. from:sub(1, 8) .. " port " .. port)
  end
end
