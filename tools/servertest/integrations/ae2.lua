-- AE2 integration smoke test (see ae2.py / ae2.txt). Reports through error("RESULT ...").
local c = component
local out, fails = {}, 0
local function add(s) out[#out + 1] = tostring(s) end
local function step(name, f)
  local ok, err = pcall(f)
  if not ok then fails = fails + 1; add(name .. ":FAIL(" .. tostring(err) .. ")") end
end
local function first(t) return c.list(t)() end
local me, db, iface, bus
step("comps", function()
  local names = {}
  for _, n in c.list() do if n:sub(1, 3) == "me_" or n == "database" then names[#names + 1] = n end end
  table.sort(names)
  add("comps=" .. table.concat(names, ","))
  me, db = c.proxy(first("me_controller")), first("database")
  iface, bus = c.proxy(first("me_interface")), c.proxy(first("me_exportbus"))
end)
step("power", function()
  add("E=" .. me.getStoredPower() .. "/" .. me.getMaxStoredPower())
  add("powered=" .. tostring(me.isNetworkPowered()) .. " idle=" .. me.getIdlePowerUsage() .. " avg=" .. me.getAvgPowerUsage())
end)
step("items", function()
  local items = me.getItemsInNetwork()
  table.sort(items, function(a, b) return a.name < b.name end)
  for _, it in ipairs(items) do
    add(it.name .. "=" .. it.size .. (it.totalBytes and ("[cell " .. it.usedBytes .. "/" .. it.totalBytes .. "B]") or ""))
  end
  add("diamonds=" .. #me.getItemsInNetwork({name = "minecraft:diamond"}))
end)
step("fluids", function()
  for _, f in ipairs(me.getFluidsInNetwork()) do add(f.name .. "=" .. f.amount .. "mB") end
end)
step("cpus", function()
  local cpus = me.getCpus()
  add("cpus=" .. #cpus .. (cpus[1] and (":" .. cpus[1].storage .. "B") or ""))
  add("craftables=" .. #me.getCraftables())
end)
-- store() into the adapter's database, then use it to configure the interface and export bus.
step("store", function()
  add("store=" .. tostring(me.store({name = "minecraft:diamond"}, db)))
  local e = c.invoke(db, "get", 1)
  add("db1=" .. tostring(e and e.name))
end)
step("iface", function()
  add("iE=" .. iface.getStoredPower())
  add("iset=" .. tostring(iface.setInterfaceConfiguration(1, db, 1, 5)))
  local cfg = iface.getInterfaceConfiguration(1)
  add("icfg=" .. tostring(cfg and (cfg.name .. "x" .. cfg.size)))
end)
step("bus", function()
  add("pE=" .. bus.getStoredPower())
  add("eset=" .. tostring(bus.setExportConfiguration(2, 1, db, 1)))
  local ecfg = bus.getExportConfiguration(2, 1)
  add("ecfg=" .. tostring(ecfg and ecfg.name))
  add("export=" .. tostring(bus.exportIntoSlot(2)))
  add("diamondsLeft=" .. me.getItemsInNetwork({name = "minecraft:diamond"})[1].size)
end)
error("RESULT " .. (fails == 0 and "OK " or ("FAILS=" .. fails .. " ")) .. table.concat(out, " "))
