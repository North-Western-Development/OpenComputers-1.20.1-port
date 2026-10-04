-- Walkthrough: microcontroller with a redstone card blinking the lamps around it.
local rs = component.proxy(component.list("redstone")())
local on = false
while true do
  on = not on
  for s = 0, 5 do rs.setOutput(s, on and 15 or 0) end
  computer.pullSignal(0.5)
end
