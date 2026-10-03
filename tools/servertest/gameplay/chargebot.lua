-- Robot next to a powered charger: its energy must go up.
local function sleep(s) local t = computer.uptime() + s while computer.uptime() < t do computer.pullSignal(t - computer.uptime()) end end
local e0 = computer.energy()
sleep(8)
local e1 = computer.energy()
error(string.format("RESULT e0=%.0f e1=%.0f max=%.0f charged=%s", e0, e1, computer.maxEnergy(), tostring(e1 > e0)))
