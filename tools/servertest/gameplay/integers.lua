-- Lua 5.3 integers from Java: math.type of callback results and signal arguments, no "n"
-- field in returned lists. Needs a redstone card; a redstone block appears east of the case.
local o = {}
local function T(v) return tostring(math.type(v)) end
local tmp = component.proxy(computer.tmpAddress())
local eeprom = component.proxy(component.list("eeprom")())
o[#o + 1] = "size=" .. T(eeprom.getSize()) .. ":" .. tostring(eeprom.getSize())
o[#o + 1] = "total=" .. T(tmp.spaceTotal())
local h = tmp.open("/x", "w") tmp.write(h, "hello") tmp.close(h)
o[#o + 1] = "fsize=" .. T(tmp.size("/x")) .. ":" .. tostring(tmp.size("/x"))
local list = tmp.list("/")
o[#o + 1] = "list=" .. tostring(list[1]) .. ",n=" .. tostring(list.n) .. ",#=" .. #list
o[#o + 1] = "uptime=" .. T(computer.uptime())
local rs = component.proxy(component.list("redstone")())
o[#o + 1] = "input=" .. T(rs.getInput(5))
computer.pushSignal("lua", 7, 2.5, 1 << 40)
local _, i, f, big = computer.pullSignal(1)
o[#o + 1] = "pushed=" .. T(i) .. "," .. T(f) .. "," .. T(big) .. ":" .. tostring(big)
local deadline = computer.uptime() + 20
while computer.uptime() < deadline do
  local sig = table.pack(computer.pullSignal(deadline - computer.uptime()))
  if sig[1] == "redstone_changed" then
    o[#o + 1] = "rs=" .. T(sig[3]) .. "," .. T(sig[4]) .. "," .. T(sig[5]) .. ":" .. tostring(sig[5])
    break
  end
end
error("RESULT " .. table.concat(o, " "))
