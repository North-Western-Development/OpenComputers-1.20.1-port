-- Assembler east of the computer, loaded with a tier 1 case, CPU, RAM, EEPROM and an inventory
-- upgrade: assemble a robot.
local a = component.proxy(component.list("assembler")())
local o = {}
local function add(k, f, ...) local r = table.pack(pcall(f, ...)) for i = 1, r.n do r[i] = tostring(r[i]) end o[#o + 1] = k .. "=" .. table.concat(r, ",", 2) end
add("status", a.status)
add("start", a.start)
add("busy", a.status)
local t = computer.uptime() + 90
while computer.uptime() < t and a.status() == "busy" do computer.pullSignal(0.5) end
add("done", a.status)
error("RESULT " .. table.concat(o, " "))
