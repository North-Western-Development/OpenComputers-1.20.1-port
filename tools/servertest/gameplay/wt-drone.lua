-- Walkthrough: the drone flies a loop around its start position, changing light colour.
local d = component.proxy(component.list("drone")())
d.setStatusText("Hello!")
local function wait(t) local e = computer.uptime() + t repeat computer.pullSignal(e - computer.uptime()) until computer.uptime() >= e end
local path = {{0, 3, 0}, {3, 0, 0}, {0, 0, 3}, {-3, 0, 0}, {0, 0, -3}, {0, -3, 0}}
local colors = {0xff0000, 0x00ff00, 0x0080ff, 0xffff00, 0xff00ff, 0x00ffff}
while true do
  for i, p in ipairs(path) do
    d.setLightColor(colors[i])
    d.setStatusText(({"Up!", "East", "South", "West", "North", "Down"})[i])
    d.move(p[1], p[2], p[3])
    wait(2.5)
  end
end
