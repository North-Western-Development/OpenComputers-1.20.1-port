-- Nanomachines: talks to the nanomachines of the player standing next to this computer through
-- its wireless card (like the nanomachines program of OpenOS). The first run (empty EEPROM data)
-- enables inputs 1 and 2 (safe) and briefly input 3 (overload damage); later runs (after a
-- restart) report the state, which must match, then overload the player until they die.
local m = component.proxy(component.list("modem")())
local eeprom = component.proxy(component.list("eeprom")())
local port = 7
m.open(port)
local function cmd(...)
  m.broadcast(port, "nanomachines", ...)
  local deadline = computer.uptime() + 5
  while computer.uptime() < deadline do
    local sig = table.pack(computer.pullSignal(deadline - computer.uptime()))
    if sig[1] == "modem_message" and sig[6] == "nanomachines" then
      local t = {}
      for i = 7, sig.n do
        t[#t + 1] = type(sig[i]) == "number" and string.format("%.1f", sig[i]) or tostring(sig[i])
      end
      return table.concat(t, ",")
    end
  end
  return "timeout"
end
local o = {}
local function add(k, ...) o[#o + 1] = k .. "=" .. cmd(...) end
local first = eeprom.getData() == ""
add("port", "setResponsePort", port)
add("power", "getPowerState")
add("name", "getName")
add("inputs", "getTotalInputCount")
add("safe", "getSafeActiveInputs")
add("max", "getMaxActiveInputs")
if first then
  add("set1", "setInput", 1, true)
  add("set2", "setInput", 2, true)
end
add("in1", "getInput", 1)
add("in2", "getInput", 2)
add("in3", "getInput", 3)
add("effects", "getActiveEffects")
if first then
  add("health", "getHealth")
  add("set3", "setInput", 3, true)
  local t = computer.uptime() + 4
  while computer.uptime() < t do computer.pullSignal(t - computer.uptime()) end
  add("overloaded", "getHealth")
  add("unset3", "setInput", 3, false)
  eeprom.setData("configured")
else
  -- Overload until the player dies (death message of the overload damage type).
  add("set3", "setInput", 3, true)
  add("set4", "setInput", 4, true)
  local t = computer.uptime() + 15
  while computer.uptime() < t do computer.pullSignal(t - computer.uptime()) end
end
error("RESULT nano " .. (first and "first " or "again ") .. table.concat(o, " "))
