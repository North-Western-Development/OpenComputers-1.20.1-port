-- Walkthrough: the robot drives a 3x3 square forever, changing its light colour at each corner.
local r = component.proxy(component.list("robot")())
local colors = {0xff3030, 0x30ff30, 0x3080ff, 0xffd030}
local i = 0
local function wait(t) local e = computer.uptime() + t repeat computer.pullSignal(e - computer.uptime()) until computer.uptime() >= e end
while true do
  for _ = 1, 3 do r.move(3) end
  r.turn(true)
  i = i + 1
  r.setLightColor(colors[i % 4 + 1])
  wait(0.3)
end
