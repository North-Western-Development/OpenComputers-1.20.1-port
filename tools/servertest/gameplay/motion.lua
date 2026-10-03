-- Motion sensor east of the computer; a pig gets teleported around in front of it.
local m = component.proxy(component.list("motion_sensor")())
local o = {}
local function add(k, f, ...) local r = table.pack(pcall(f, ...)) for i = 1, r.n do r[i] = tostring(r[i]) end o[#o + 1] = k .. "=" .. table.concat(r, ",", 2) end
add("sens", m.setSensitivity, 0.2)
add("getSens", m.getSensitivity)
local got = "none"
local t = computer.uptime() + 25
while computer.uptime() < t do
  local s = table.pack(computer.pullSignal(t - computer.uptime()))
  if s[1] == "motion" then got = string.format("%.1f,%.1f,%.1f,%s", s[3], s[4], s[5], tostring(s[6])) break end
end
o[#o + 1] = "motion=" .. got
error("RESULT " .. table.concat(o, " "))
